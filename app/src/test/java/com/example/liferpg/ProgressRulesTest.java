package com.example.liferpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import com.example.liferpg.logic.AchievementEngine;
import com.example.liferpg.logic.FocusTimerLogic;
import com.example.liferpg.logic.StreakCalculator;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ProgressSnapshot;

/** Streaks, achievements and the focus timer state machine. */
public class ProgressRulesTest {

    @Test
    public void streakStartsAtOne() {
        assertEquals(1, StreakCalculator.nextStreak(null, "2026-10-16", 0));
    }

    @Test
    public void sameDayDoesNotIncreaseStreak() {
        assertEquals(3, StreakCalculator.nextStreak("2026-10-16", "2026-10-16", 3));
    }

    @Test
    public void consecutiveDayIncreasesStreak() {
        assertEquals(4, StreakCalculator.nextStreak("2026-10-15", "2026-10-16", 3));
        assertEquals(2, StreakCalculator.nextStreak("2026-02-28", "2026-03-01", 1)); // month boundary
    }

    @Test
    public void missedDayResetsStreak() {
        assertEquals(1, StreakCalculator.nextStreak("2026-10-13", "2026-10-16", 7));
    }

    @Test
    public void displayedStreakLapsesAfterAMissedDay() {
        assertEquals(4, StreakCalculator.effectiveStreak("2026-10-16", "2026-10-16", 4));
        assertEquals(4, StreakCalculator.effectiveStreak("2026-10-15", "2026-10-16", 4));
        assertEquals(0, StreakCalculator.effectiveStreak("2026-10-14", "2026-10-16", 4));
        assertEquals(0, StreakCalculator.effectiveStreak(null, "2026-10-16", 0));
    }

    @Test
    public void noProgressUnlocksNothing() {
        assertTrue(AchievementEngine.evaluate(new ProgressSnapshot(0, 0, 0, 0, 1)).isEmpty());
    }

    @Test
    public void firstQuestUnlocksOnlyFirstQuest() {
        List<Achievement> list = AchievementEngine.evaluate(new ProgressSnapshot(1, 0, 0, 1, 1));
        assertEquals(1, list.size());
        assertEquals(Achievement.FIRST_QUEST, list.get(0));
    }

    @Test
    public void milestoneAchievements() {
        ProgressSnapshot s = new ProgressSnapshot(10, 1, 1, 5, 5);
        List<Achievement> list = AchievementEngine.evaluate(s);
        assertEquals(Achievement.values().length, list.size());
        assertTrue(AchievementEngine.isEarned(Achievement.TEN_QUESTS, s));
        assertFalse(AchievementEngine.isEarned(Achievement.TEN_QUESTS, new ProgressSnapshot(9, 0, 0, 0, 1)));
        assertFalse(AchievementEngine.isEarned(Achievement.STREAK_5, new ProgressSnapshot(0, 0, 0, 4, 1)));
        assertFalse(AchievementEngine.isEarned(Achievement.LEVEL_5, new ProgressSnapshot(0, 0, 0, 0, 4)));
    }

    @Test
    public void achievementProgressValues() {
        ProgressSnapshot s = new ProgressSnapshot(7, 0, 2, 3, 2);
        assertEquals(7, AchievementEngine.current(Achievement.TEN_QUESTS, s));
        assertEquals(3, AchievementEngine.current(Achievement.STREAK_5, s));
        assertEquals(2, AchievementEngine.current(Achievement.FIRST_WORKOUT, s));
    }

    @Test
    public void timerRunsPausesAndResumes() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(25, 1_000_000L, "s1");
        assertEquals(FocusTimerLogic.State.RUNNING, t.getState());
        assertEquals(25 * 60_000L, t.remainingMs(1_000_000L));
        assertFalse(t.isFinished(1_000_000L + 60_000L));

        t.pause(1_000_000L + 10 * 60_000L);
        assertEquals(FocusTimerLogic.State.PAUSED, t.getState());
        assertEquals(15 * 60_000L, t.remainingMs(9_999_999_999L)); // frozen while paused
        assertFalse(t.isFinished(9_999_999_999L));

        long resumeAt = 5_000_000L;
        t.resume(resumeAt);
        assertEquals(15 * 60_000L, t.remainingMs(resumeAt));
        assertTrue(t.isFinished(resumeAt + 15 * 60_000L));
    }

    @Test
    public void timerSurvivesBackgroundingBecauseItUsesWallClock() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(15, 0L, "s2");
        // App was in the background for 20 minutes
        assertTrue(t.isFinished(20 * 60_000L));
        assertEquals(0L, t.remainingMs(20 * 60_000L));
        assertEquals(1f, t.progress(20 * 60_000L), 0.0001f);
    }

    @Test
    public void timerResetReturnsToIdle() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(45, 0L, "s3");
        t.reset();
        assertEquals(FocusTimerLogic.State.IDLE, t.getState());
        assertEquals(null, t.getSessionId());
        assertFalse(t.isFinished(Long.MAX_VALUE / 2));
    }

    @Test
    public void timerRestoreRoundTrip() {
        FocusTimerLogic a = new FocusTimerLogic();
        a.start(60, 1000L, "abc");
        FocusTimerLogic b = new FocusTimerLogic();
        b.restore(a.getState(), a.getDurationMin(), a.getEndAtMs(), a.getStoredRemainingMs(), a.getSessionId());
        assertEquals(a.remainingMs(5000L), b.remainingMs(5000L));
        assertEquals("abc", b.getSessionId());
    }

    @Test
    public void durationCanOnlyBeChangedWhileIdle() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.selectDuration(45);
        assertEquals(45, t.getDurationMin());
        t.start(45, 0L, "x");
        t.selectDuration(15);
        assertEquals(45, t.getDurationMin());
    }

    @Test
    public void formatShowsMinutesAndSeconds() {
        assertEquals("25:00", FocusTimerLogic.format(25 * 60_000L));
        assertEquals("00:01", FocusTimerLogic.format(1L));
        assertEquals("00:00", FocusTimerLogic.format(0L));
    }
}
