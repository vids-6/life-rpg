package com.example.liferpg.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quests")
public class Quest {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String title = "";
    @NonNull
    public String description = "";
    /** Category enum name. */
    @NonNull
    public String category = "PERSONAL";
    /** Difficulty enum name. */
    @NonNull
    public String difficulty = "MEDIUM";
    /** Base XP (before any late penalty). Calculated from difficulty x duration for quests with a duration. */
    public int xpReward = 40;
    /** Estimated duration in minutes. 0 = quest created before durations existed (keeps its stored XP). */
    @ColumnInfo(defaultValue = "0")
    public int durationMin;
    /** Start-of-day epoch millis, 0 = no due date. */
    public long dueDate;
    public boolean recurringDaily;
    /** One-off quests only. */
    public boolean completed;
    public long completedAt;
    /** yyyy-MM-dd of the last completion (daily quests use it to reset each day). */
    public String lastCompletedDate;
    public int timesCompleted;
    public long createdAt;

    /** True when this quest cannot be completed again right now. */
    public boolean isDoneToday(String todayKey) {
        return recurringDaily ? todayKey.equals(lastCompletedDate) : completed;
    }

    /** A finished one-off quest: its reward-relevant fields can no longer be edited. */
    public boolean isRewardLocked() {
        return !recurringDaily && completed;
    }

    /** Once a quest has paid out, switching between one-off and daily could pay it out twice. */
    public boolean isRepeatLocked() {
        return timesCompleted > 0;
    }

    public Quest copy() {
        Quest q = new Quest();
        q.id = id;
        q.title = title;
        q.description = description;
        q.category = category;
        q.difficulty = difficulty;
        q.xpReward = xpReward;
        q.durationMin = durationMin;
        q.dueDate = dueDate;
        q.recurringDaily = recurringDaily;
        q.completed = completed;
        q.completedAt = completedAt;
        q.lastCompletedDate = lastCompletedDate;
        q.timesCompleted = timesCompleted;
        q.createdAt = createdAt;
        return q;
    }
}
