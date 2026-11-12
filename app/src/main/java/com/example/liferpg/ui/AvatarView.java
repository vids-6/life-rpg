package com.example.liferpg.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Shows the customisable hero avatar. All drawing lives in {@link AvatarPainter}; this view only
 * measures itself (always square) and forwards the current {@link AvatarConfig}.
 */
public class AvatarView extends View {
    private final AvatarPainter painter = new AvatarPainter();
    private AvatarConfig config = new AvatarConfig();
    private boolean circle = true;
    private float zoom = 1f;
    private float focusX = 50f;
    private float focusY = 50f;

    public AvatarView(Context c) {
        super(c);
    }

    public AvatarView(Context c, AttributeSet a) {
        super(c, a);
    }

    public AvatarView(Context c, AttributeSet a, int s) {
        super(c, a, s);
    }

    public void setConfig(AvatarConfig cfg) {
        config = cfg == null ? new AvatarConfig() : cfg;
        invalidate();
    }

    public AvatarConfig getConfig() {
        return config;
    }

    public void setCircle(boolean circle) {
        this.circle = circle;
        invalidate();
    }

    /** Crop in on a point of the 100 x 100 design grid (used for face-part thumbnails). */
    public void setZoom(float zoom, float focusX, float focusY) {
        this.zoom = zoom;
        this.focusX = focusX;
        this.focusY = focusY;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int ws = MeasureSpec.getSize(w);
        int hs = MeasureSpec.getSize(h);
        int wm = MeasureSpec.getMode(w);
        int hm = MeasureSpec.getMode(h);
        int size;
        if (wm == MeasureSpec.UNSPECIFIED && hm == MeasureSpec.UNSPECIFIED) {
            size = (int) (96 * getResources().getDisplayMetrics().density);
        } else if (wm == MeasureSpec.UNSPECIFIED) {
            size = hs;
        } else if (hm == MeasureSpec.UNSPECIFIED) {
            size = ws;
        } else {
            size = Math.min(ws, hs);
        }
        setMeasuredDimension(resolveSize(size, w), resolveSize(size, h));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        painter.draw(canvas, config, getWidth(), getHeight(), circle, zoom, focusX, focusY);
    }
}
