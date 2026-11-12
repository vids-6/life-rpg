package com.example.liferpg.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.Quest;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.QuestXp;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.Difficulty;
import com.example.liferpg.model.RewardResult;

public class QuestRepository {
    public static final int MAX_TITLE_LENGTH = 60;

    private final AppDatabase db;
    private final RewardEngine engine;

    public QuestRepository(Context context) {
        this.db = AppDatabase.get(context.getApplicationContext());
        this.engine = new RewardEngine(db);
    }

    /** Active quests first (earliest due date first), finished quests last. */
    public List<Quest> getAll() {
        final String today = DateUtils.todayKey();
        List<Quest> list = db.questDao().all();
        Collections.sort(list, new Comparator<Quest>() {
            @Override
            public int compare(Quest a, Quest b) {
                boolean da = a.isDoneToday(today);
                boolean dbb = b.isDoneToday(today);
                if (da != dbb) return da ? 1 : -1;
                long dueA = a.dueDate == 0 ? Long.MAX_VALUE : a.dueDate;
                long dueB = b.dueDate == 0 ? Long.MAX_VALUE : b.dueDate;
                if (dueA != dueB) return dueA < dueB ? -1 : 1;
                return Long.compare(b.createdAt, a.createdAt);
            }
        });
        return list;
    }

    public List<ActivityLog> history(long questId) {
        return db.activityDao().questHistory(questId, 10);
    }

    /** Validates and inserts/updates. Throws IllegalArgumentException with a user-friendly message. */
    public void save(Quest q) {
        String title = q.title == null ? "" : q.title.trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Quest title is required.");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title can be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        q.title = title;
        q.description = q.description == null ? "" : q.description.trim();
        if (q.id == 0) {
            applyRewardRules(q);
            q.completed = false;
            q.completedAt = 0;
            q.lastCompletedDate = null;
            q.timesCompleted = 0;
            q.createdAt = System.currentTimeMillis();
            db.questDao().insert(q);
            return;
        }
        // Edit: the stored row is the source of truth for completion state and for anything that is locked.
        db.runInTransaction(() -> {
            Quest stored = db.questDao().byId(q.id);
            if (stored == null) throw new IllegalArgumentException("This quest no longer exists.");
            stored.title = q.title;
            stored.description = q.description;
            if (!stored.isRewardLocked()) {
                stored.category = q.category;
                stored.difficulty = q.difficulty;
                stored.durationMin = q.durationMin;
                stored.xpReward = q.xpReward;
                stored.dueDate = q.dueDate;
                if (!stored.isRepeatLocked()) stored.recurringDaily = q.recurringDaily;
                applyRewardRules(stored);
            }
            db.questDao().update(stored);
        });
    }

    /** Duration-based quests always get their XP from the rules, never from whatever the UI sent. */
    private static void applyRewardRules(Quest q) {
        if (q.durationMin > 0) {
            if (q.durationMin > 24 * 60) throw new IllegalArgumentException("Duration is too long.");
            q.xpReward = QuestXp.baseXp(Difficulty.fromName(q.difficulty), q.durationMin);
        }
        if (!XpCalculator.isValidQuestXp(q.xpReward)) {
            throw new IllegalArgumentException("XP must be between " + XpCalculator.MIN_QUEST_XP
                    + " and " + XpCalculator.MAX_QUEST_XP + ".");
        }
        if (q.recurringDaily) q.dueDate = 0;
    }

    public void delete(long id) {
        db.questDao().delete(id);
    }

    /**
     * Completes a quest and grants XP exactly once: the quest state change and the reward happen
     * in one transaction, and the activity log's unique key blocks any second claim.
     * One-off quests: once. Daily quests: once per calendar day.
     */
    public RewardResult complete(final long questId) {
        return db.runInTransaction(() -> {
            Quest q = db.questDao().byId(questId);
            if (q == null) return RewardResult.rejected("Quest no longer exists.");
            String today = DateUtils.todayKey();
            if (q.isDoneToday(today)) {
                return RewardResult.rejected(q.recurringDaily
                        ? "Already completed today. It resets tomorrow."
                        : "Quest already completed.");
            }
            long now = System.currentTimeMillis();
            Category cat = Category.fromName(q.category);

            ActivityLog log = new ActivityLog();
            log.type = ActivityType.QUEST;
            log.refKey = q.recurringDaily ? "Q:" + q.id + ":" + today : "Q:" + q.id;
            log.refId = q.id;
            log.title = q.title;
            log.category = cat.name();
            // Overdue quests stay completable; they just pay a smaller share of their base XP.
            int base = XpCalculator.sanitizeQuestXp(q.xpReward);
            int percent = q.recurringDaily ? 100 : QuestXp.latePercent(q.dueDate, now);
            log.xp = QuestXp.earnedXp(base, percent);
            log.durationMin = q.durationMin;
            log.detail = percent < 100 ? "late:" + percent + "% of " + base : null;
            log.timestamp = now;
            log.dateKey = today;

            RewardResult result = engine.award(log, XpCalculator.questGain(cat, log.xp))
                    .withLate(base, percent);
            if (result.awarded) {
                q.completed = !q.recurringDaily;
                q.completedAt = now;
                q.lastCompletedDate = today;
                q.timesCompleted++;
                db.questDao().update(q);
            }
            return result;
        });
    }

    /**
     * "Today's quests": daily quests + quests due today + one-off quests completed today.
     * Unfinished ones come first.
     */
    public List<Quest> todayQuests() {
        final String today = DateUtils.todayKey();
        List<Quest> result = new ArrayList<>();
        for (Quest q : db.questDao().all()) {
            boolean dueToday = q.dueDate > 0 && DateUtils.keyForMillis(q.dueDate).equals(today);
            boolean completedToday = !q.recurringDaily && q.completed && q.completedAt > 0
                    && DateUtils.keyForMillis(q.completedAt).equals(today);
            if (q.recurringDaily || dueToday || completedToday) result.add(q);
        }
        Collections.sort(result, new Comparator<Quest>() {
            @Override
            public int compare(Quest a, Quest b) {
                boolean da = a.isDoneToday(today);
                boolean dbb = b.isDoneToday(today);
                if (da != dbb) return da ? 1 : -1;
                return Long.compare(b.createdAt, a.createdAt);
            }
        });
        return result;
    }

    /** {done, total} for "today's quests". */
    public int[] todayProgress() {
        String today = DateUtils.todayKey();
        int done = 0;
        List<Quest> list = todayQuests();
        for (Quest q : list) if (q.isDoneToday(today)) done++;
        return new int[]{done, list.size()};
    }
}
