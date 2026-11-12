package com.example.liferpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.liferpg.logic.FocusTimerLogic;
import com.example.liferpg.logic.FocusTimerLogic.State;

public class FocusTimerLogicTest {
    private static final long T0 = 1_000_000L;

    @Test
    public void idleShowsFullDuration() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.selectDuration(45);
        assertEquals(State.IDLE, t.getState());
        assertEquals("45:00", FocusTimerLogic.format(t.remainingMs(T0)));
        assertEquals(0f, t.progress(T0), 0f);
    }

    @Test
    public void runningCountsDownAndFinishes() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(25, T0, "s1");
        assertEquals(State.RUNNING, t.getState());
        assertEquals("25:00", FocusTimerLogic.format(t.remainingMs(T0)));
        assertEquals("25:00", FocusTimerLogic.format(t.remainingMs(T0 + 1)));   // rounds up: first full second still reads 25:00
        assertEquals("24:59", FocusTimerLogic.format(t.remainingMs(T0 + 1000)));
        assertEquals("12:30", FocusTimerLogic.format(t.remainingMs(T0 + 12 * 60_000L + 30_000L)));
        assertEquals(0.5f, t.progress(T0 + 12 * 60_000L + 30_000L), 0.001f);
        assertFalse(t.isFinished(T0 + 25 * 60_000L - 1));
        assertTrue(t.isFinished(T0 + 25 * 60_000L));
        assertEquals("00:00", FocusTimerLogic.format(t.remainingMs(T0 + 26 * 60_000L)));
        assertEquals(1f, t.progress(T0 + 26 * 60_000L), 0f);
    }

    @Test
    public void pauseFreezesAndResumeContinues() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(15, T0, "s1");
        long pausedAt = T0 + 5 * 60_000L;
        t.pause(pausedAt);
        assertEquals(State.PAUSED, t.getState());
        assertEquals("10:00", FocusTimerLogic.format(t.remainingMs(pausedAt)));
        assertEquals("10:00", FocusTimerLogic.format(t.remainingMs(pausedAt + 3_600_000L))); // time passing while paused
        assertFalse(t.isFinished(pausedAt + 3_600_000L));
        t.resume(pausedAt + 3_600_000L);
        assertEquals(State.RUNNING, t.getState());
        assertEquals("09:30", FocusTimerLogic.format(t.remainingMs(pausedAt + 3_600_000L + 30_000L)));
    }

    @Test
    public void resetReturnsToIdleAndDropsSession() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(60, T0, "s1");
        t.reset();
        assertEquals(State.IDLE, t.getState());
        assertNull(t.getSessionId());
        assertEquals("60:00", FocusTimerLogic.format(t.remainingMs(T0 + 1000)));
        assertFalse(t.isFinished(T0 + 99 * 60_000L));
    }

    @Test
    public void durationCannotChangeWhileActive() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.start(25, T0, "s1");
        t.selectDuration(60);
        assertEquals(25, t.getDurationMin());
        t.start(60, T0 + 10, "s2");         // second start is ignored
        assertEquals("s1", t.getSessionId());
    }

    @Test
    public void formatCoversSingleAndDoubleDigitValues() {
        assertEquals("00:00", FocusTimerLogic.format(0));
        assertEquals("00:01", FocusTimerLogic.format(1));
        assertEquals("00:09", FocusTimerLogic.format(9_000));
        assertEquals("00:10", FocusTimerLogic.format(10_000));
        assertEquals("01:05", FocusTimerLogic.format(65_000));
        assertEquals("09:59", FocusTimerLogic.format(599_000));
        assertEquals("10:00", FocusTimerLogic.format(600_000));
        assertEquals("60:00", FocusTimerLogic.format(3_600_000));
        assertEquals("05:00", FocusTimerLogic.format(299_001)); // rounds up, matches what the user is counting
    }

    @Test
    public void restoreSurvivesProcessDeathAndRejectsBrokenState() {
        FocusTimerLogic t = new FocusTimerLogic();
        t.restore(State.RUNNING, 25, T0 + 60_000L, 0, "s1");
        assertEquals("01:00", FocusTimerLogic.format(t.remainingMs(T0)));
        assertTrue(t.isFinished(T0 + 60_000L));      // finished while the app was dead
        t.restore(State.PAUSED, 0, 0, 90_000L, "s2");
        assertEquals(25, t.getDurationMin());
        assertEquals("01:30", FocusTimerLogic.format(t.remainingMs(T0)));
        t.restore(State.RUNNING, 25, T0, 0, null);   // running without a session id is discarded
        assertEquals(State.IDLE, t.getState());
    }
}
