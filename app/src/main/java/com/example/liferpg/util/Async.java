package com.example.liferpg.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny helper: run blocking work on a background thread, deliver the result on the main thread. */
public final class Async {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Work<T> {
        T run() throws Exception;
    }

    public interface Done<T> {
        void onResult(T result, Exception error);
    }

    private Async() { }

    public static <T> void run(final Work<T> work, final Done<T> done) {
        IO.execute(new Runnable() {
            @Override
            public void run() {
                T result = null;
                Exception error = null;
                try {
                    result = work.run();
                } catch (Exception e) {
                    error = e;
                }
                final T r = result;
                final Exception err = error;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        done.onResult(r, err);
                    }
                });
            }
        });
    }

    /** Separate pool for slow network calls so database work is never queued behind them. */
    private static final ExecutorService NET = Executors.newFixedThreadPool(2);

    public static <T> void network(final Work<T> work, final Done<T> done) {
        NET.execute(new Runnable() {
            @Override
            public void run() {
                T result = null;
                Exception error = null;
                try {
                    result = work.run();
                } catch (Exception e) {
                    error = e;
                }
                final T r = result;
                final Exception err = error;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        done.onResult(r, err);
                    }
                });
            }
        });
    }
}
