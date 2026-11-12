package com.example.liferpg.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Wraps a MapView that sits inside a scrolling page: while a finger is on the map the parent scroll view
 * must not steal the gesture, otherwise the map can never be panned vertically.
 */
public class NestedMapContainer extends FrameLayout {

    public NestedMapContainer(Context c) {
        super(c);
    }

    public NestedMapContainer(Context c, AttributeSet a) {
        super(c, a);
    }

    public NestedMapContainer(Context c, AttributeSet a, int s) {
        super(c, a, s);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(
                    action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(ev);
    }
}
