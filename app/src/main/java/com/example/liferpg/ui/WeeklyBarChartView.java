package com.example.liferpg.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.liferpg.R;

/**
 * Minimal bar chart for the last 7 days of XP, drawn with Canvas (no chart library needed).
 * Past days use a mint-to-teal gradient; the last bar (today) is highlighted in gold.
 */
public class WeeklyBarChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String[] labels = new String[0];
    private int[] values = new int[0];

    public WeeklyBarChartView(Context context) {
        this(context, null);
    }

    public WeeklyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float sp = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 13,
                getResources().getDisplayMetrics());
        Typeface medium = ResourcesCompat.getFont(context, R.font.poppins_medium);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.lr_surface_alt));
        labelPaint.setColor(ContextCompat.getColor(context, R.color.lr_text_tertiary));
        labelPaint.setTextSize(sp);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        todayLabelPaint.setColor(ContextCompat.getColor(context, R.color.lr_gold_text));
        todayLabelPaint.setTextSize(sp);
        todayLabelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.lr_text));
        valuePaint.setTextSize(sp);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        if (medium != null) {
            labelPaint.setTypeface(medium);
            todayLabelPaint.setTypeface(medium);
            valuePaint.setTypeface(medium);
        }
    }

    public void setData(String[] labels, int[] values) {
        this.labels = labels;
        this.values = values;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float d = getResources().getDisplayMetrics().density;
        float labelH = labelPaint.getTextSize() + 12 * d;
        float topPad = valuePaint.getTextSize() + 12 * d;
        float bottom = h - labelH;
        barPaint.setShader(new LinearGradient(0, topPad, 0, bottom,
                ContextCompat.getColor(getContext(), R.color.lr_accent),
                ContextCompat.getColor(getContext(), R.color.lr_primary), Shader.TileMode.CLAMP));
        todayPaint.setShader(new LinearGradient(0, topPad, 0, bottom,
                ContextCompat.getColor(getContext(), R.color.lr_gold_light),
                ContextCompat.getColor(getContext(), R.color.lr_gold_dark), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = Math.min(labels.length, values.length);
        if (n == 0) return;
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth();
        float labelH = labelPaint.getTextSize() + 12 * d;
        float topPad = valuePaint.getTextSize() + 12 * d;
        float chartH = getHeight() - labelH - topPad;
        int max = 1;
        for (int i = 0; i < n; i++) max = Math.max(max, values[i]);
        float slot = w / n;
        float barW = slot * 0.46f;
        float radius = 8 * d;
        for (int i = 0; i < n; i++) {
            boolean today = i == n - 1;
            float cx = slot * i + slot / 2f;
            rect.set(cx - barW / 2f, topPad, cx + barW / 2f, topPad + chartH);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);
            if (values[i] > 0) {
                float h = Math.max(radius * 2, chartH * values[i] / max);
                rect.set(cx - barW / 2f, topPad + chartH - h, cx + barW / 2f, topPad + chartH);
                canvas.drawRoundRect(rect, radius, radius, today ? todayPaint : barPaint);
                canvas.drawText(String.valueOf(values[i]), cx, topPad + chartH - h - 5 * d, valuePaint);
            }
            canvas.drawText(labels[i], cx, getHeight() - 5 * d, today ? todayLabelPaint : labelPaint);
        }
    }
}
