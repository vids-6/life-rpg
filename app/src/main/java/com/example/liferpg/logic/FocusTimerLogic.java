package com.example.liferpg.logic;

/**
 * Pure timer state machine. Uses wall-clock timestamps instead of ticking counters, so the
 * remaining time is always correct after the app was backgrounded or the activity recreated.
 */
public class FocusTimerLogic {
    public enum State { IDLE, RUNNING, PAUSED }

    private State state = State.IDLE;
    private int durationMin = 25;
    private long endAtMs;
    private long remainingMs;
    private String sessionId;

    public State getState() { return state; }

    public int getDurationMin() { return durationMin; }

    public String getSessionId() { return sessionId; }

    public long getEndAtMs() { return endAtMs; }

    public long getStoredRemainingMs() { return remainingMs; }

    /** Choose a duration while idle. Ignored while a session is active. */
    public void selectDuration(int minutes) {
        if (state == State.IDLE && minutes > 0) durationMin = minutes;
    }

    public void start(int minutes, long nowMs, String newSessionId) {
        if (state != State.IDLE || minutes <= 0) return;
        durationMin = minutes;
        sessionId = newSessionId;
        endAtMs = nowMs + minutes * 60_000L;
        remainingMs = 0;
        state = State.RUNNING;
    }

    public void pause(long nowMs) {
        if (state != State.RUNNING) return;
        remainingMs = Math.max(0L, endAtMs - nowMs);
        state = State.PAUSED;
    }

    public void resume(long nowMs) {
        if (state != State.PAUSED) return;
        endAtMs = nowMs + remainingMs;
        state = State.RUNNING;
    }

    public void reset() {
        state = State.IDLE;
        sessionId = null;
        endAtMs = 0;
        remainingMs = 0;
    }

    public long remainingMs(long nowMs) {
        switch (state) {
            case RUNNING: return Math.max(0L, endAtMs - nowMs);
            case PAUSED: return remainingMs;
            default: return durationMin * 60_000L;
        }
    }

    public boolean isFinished(long nowMs) {
        return state == State.RUNNING && nowMs >= endAtMs;
    }

    /** 0..1 fraction elapsed. */
    public float progress(long nowMs) {
        if (state == State.IDLE) return 0f;
        long total = durationMin * 60_000L;
        long elapsed = total - remainingMs(nowMs);
        return Math.max(0f, Math.min(1f, elapsed / (float) total));
    }

    /** Restore a persisted state. */
    public void restore(State state, int durationMin, long endAtMs, long remainingMs, String sessionId) {
        this.state = state;
        this.durationMin = durationMin > 0 ? durationMin : 25;
        this.endAtMs = endAtMs;
        this.remainingMs = remainingMs;
        this.sessionId = sessionId;
        if (this.state != State.IDLE && sessionId == null) reset();
    }

    public static String format(long ms) {
        long totalSec = (ms + 999) / 1000;
        return String.format(java.util.Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60);
    }
}
