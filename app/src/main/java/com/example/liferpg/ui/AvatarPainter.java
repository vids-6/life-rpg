package com.example.liferpg.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Draws the layered vector hero avatar on a 100 x 100 design grid. No bitmaps, no network:
 * every skin tone, hairstyle, outfit, accessory, backdrop and frame is a handful of paths, so the
 * avatar stays crisp at any size and costs almost nothing to render.
 *
 * Layer order (back to front): backdrop, back hair, body/outfit, neck, ears, head, face details,
 * eyes, mouth, front hair, accessory, frame.
 */
public final class AvatarPainter {
    static final int[] SKIN = {0xFFFFE3D0, 0xFFF7CBA8, 0xFFE0A878, 0xFFC58A5E, 0xFF94613F, 0xFF5F3A27};
    static final int[] HAIR = {0xFF1E1A28, 0xFF5B3A26, 0xFF9C4A2B, 0xFFE4BD62, 0xFFCBD0DC, 0xFF8B5CF6, 0xFF20A89E};
    static final int[][] BG = {
            {0xFF4C2FB8, 0xFF1F3C94}, {0xFF1E88C7, 0xFF123B7A}, {0xFFFF8A5B, 0xFF9B2F6B},
            {0xFF2E9E6B, 0xFF14442F}, {0xFF1A1740, 0xFF0A0919}, {0xFFE0568F, 0xFF5B2C8F}};
    /** main colour, trim colour */
    static final int[][] OUTFIT = {
            {0xFF7C4DFF, 0xFFF5C451}, {0xFF8E98AD, 0xFFF5C451}, {0xFF2F5BD6, 0xFFF5C451},
            {0xFF3A3552, 0xFFB7B0D6}, {0xFF3F8F52, 0xFF8A5A2B}, {0xFFB3263F, 0xFFF5C451}};

    private static final int INK = 0xFF1B1530;
    private static final int LIP = 0xFF5B2233;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Shader[] bgShaders = new Shader[BG.length];
    private Shader glowShader;
    private Shader arcaneShader;

    private void fill(int color) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(color);
    }

    private void stroke(int color, float w) {
        p.setStyle(Paint.Style.STROKE);
        p.setShader(null);
        p.setColor(color);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    static int darker(int c, float f) {
        return 0xFF000000 | ((int) (((c >> 16) & 0xFF) * f) << 16)
                | ((int) (((c >> 8) & 0xFF) * f) << 8) | (int) ((c & 0xFF) * f);
    }

    static int lighter(int c, float f) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r += (int) ((255 - r) * f);
        g += (int) ((255 - g) * f);
        b += (int) ((255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * @param canvas  target
     * @param w,h     view size in pixels (the avatar is drawn in the largest centred square)
     * @param circle  clip to a circle (true) or a rounded square (false)
     * @param zoom    1 = whole bust; higher values crop in on (fx, fy) in 100-grid units (used for face thumbnails)
     */
    public void draw(Canvas canvas, AvatarConfig c, int w, int h, boolean circle, float zoom, float fx, float fy) {
        float size = Math.min(w, h);
        if (size <= 0) return;
        canvas.save();
        canvas.translate((w - size) / 2f, (h - size) / 2f);
        float s = size / 100f;
        canvas.scale(s, s);

        // Clip + backdrop (never zoomed)
        canvas.save();
        path.reset();
        if (circle) {
            path.addCircle(50, 50, 50, Path.Direction.CW);
        } else {
            rect.set(0, 0, 100, 100);
            path.addRoundRect(rect, 18, 18, Path.Direction.CW);
        }
        canvas.clipPath(path);
        drawBackdrop(canvas, c.bg);

        canvas.save();
        if (zoom != 1f) {
            canvas.translate(50, 50);
            canvas.scale(zoom, zoom);
            canvas.translate(-fx, -fy);
        }
        drawCharacter(canvas, c);
        canvas.restore();
        canvas.restore();

        drawFrame(canvas, c.frame, circle);
        canvas.restore();
    }

    private void drawBackdrop(Canvas cv, int bg) {
        if (bgShaders[bg] == null) {
            bgShaders[bg] = new LinearGradient(0, 0, 100, 100, BG[bg][0], BG[bg][1], Shader.TileMode.CLAMP);
        }
        if (glowShader == null) {
            glowShader = new RadialGradient(50, 46, 48, 0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(bgShaders[bg]);
        cv.drawRect(0, 0, 100, 100, p);
        p.setShader(glowShader);
        cv.drawRect(0, 0, 100, 100, p);
        p.setShader(null);
        fill(0x66FFFFFF);
        cv.drawCircle(14, 20, 1.1f, p);
        cv.drawCircle(84, 15, 1.5f, p);
        cv.drawCircle(90, 42, 0.9f, p);
        cv.drawCircle(9, 50, 0.9f, p);
        cv.drawCircle(25, 9, 0.8f, p);
        cv.drawCircle(76, 28, 0.7f, p);
    }

    private void drawCharacter(Canvas cv, AvatarConfig c) {
        int skin = SKIN[c.skin];
        int skinDark = darker(skin, 0.9f);
        int hair = HAIR[c.hairColor];

        // Back hair
        drawHairBack(cv, c.hairStyle, hair);

        // Body + neck
        drawOutfit(cv, c.outfit, skinDark);
        fill(skinDark);
        rect.set(43, 58, 57, 80);
        cv.drawRoundRect(rect, 4, 4, p);
        fill(0x26000000); // chin shadow on the neck
        rect.set(39, 63, 61, 74);
        cv.drawOval(rect, p);

        // Ears + head
        fill(skin);
        cv.drawCircle(29.6f, 47, 4.4f, p);
        cv.drawCircle(70.4f, 47, 4.4f, p);
        fill(skinDark);
        cv.drawCircle(29.6f, 47.2f, 2f, p);
        cv.drawCircle(70.4f, 47.2f, 2f, p);
        headPath();
        fill(skin);
        cv.drawPath(path, p);

        // Face
        if (c.extras == 2) drawBeard(cv, hair, skin);
        drawBrowsAndEyes(cv, c.eyes, hair);
        drawNose(cv, skinDark);
        drawMouth(cv, c.mouth, c.extras == 2);
        fill(0x30FF6B8A);
        cv.drawCircle(38, 55.5f, 3.6f, p);
        cv.drawCircle(62, 55.5f, 3.6f, p);
        if (c.extras == 1) drawFreckles(cv, skin);
        if (c.extras == 3) drawMustache(cv, hair);
        if (c.extras == 4) drawScar(cv);

        // Front hair + accessory
        drawHairFront(cv, c.hairStyle, hair);
        drawAccessory(cv, c.accessory, hair);
    }

    private void headPath() {
        path.reset();
        path.moveTo(50, 23.5f);
        path.cubicTo(63.5f, 23.5f, 71, 33, 71, 46);
        path.cubicTo(71, 58.5f, 63.5f, 68.5f, 50, 68.5f);
        path.cubicTo(36.5f, 68.5f, 29, 58.5f, 29, 46);
        path.cubicTo(29, 33, 36.5f, 23.5f, 50, 23.5f);
        path.close();
    }

    // ---------------------------------------------------------------- outfit

    private void drawOutfit(Canvas cv, int o, int skinDark) {
        int main = OUTFIT[o][0], trim = OUTFIT[o][1];
        path.reset();
        path.moveTo(10, 100);
        path.lineTo(13, 88);
        path.quadTo(16, 75, 34, 73);
        path.lineTo(66, 73);
        path.quadTo(84, 75, 87, 88);
        path.lineTo(90, 100);
        path.close();
        fill(main);
        cv.drawPath(path, p);
        // soft highlight on the shoulders
        fill(0x18FFFFFF);
        path.reset();
        path.moveTo(16, 84);
        path.quadTo(20, 76, 34, 74.5f);
        path.lineTo(34, 100);
        path.lineTo(12, 100);
        path.close();
        cv.drawPath(path, p);

        // neckline
        path.reset();
        path.moveTo(41, 73);
        path.lineTo(50, 87);
        path.lineTo(59, 73);
        path.close();
        fill(skinDark);
        cv.drawPath(path, p);
        stroke(trim, 2.2f);
        path.reset();
        path.moveTo(40, 73);
        path.lineTo(50, 88);
        path.lineTo(60, 73);
        cv.drawPath(path, p);

        switch (o) {
            case 1: // armour plates
                fill(darker(main, 0.8f));
                rect.set(14, 79, 33, 90);
                cv.drawRoundRect(rect, 5, 5, p);
                rect.set(67, 79, 86, 90);
                cv.drawRoundRect(rect, 5, 5, p);
                stroke(trim, 1.6f);
                cv.drawLine(14.5f, 92, 33, 92, p);
                cv.drawLine(67, 92, 85.5f, 92, p);
                fill(trim);
                cv.drawCircle(50, 95, 2f, p);
                break;
            case 2: // robe stars
                fill(trim);
                cv.drawCircle(26, 90, 1.8f, p);
                cv.drawCircle(74, 92, 1.8f, p);
                cv.drawCircle(63, 97, 1.4f, p);
                cv.drawCircle(36, 97, 1.2f, p);
                break;
            case 3: // hood + drawstrings
                stroke(darker(main, 0.8f), 5f);
                cv.drawArc(30, 66, 70, 90, 200, 140, false, p);
                stroke(0xFFE8E4FF, 1.5f);
                cv.drawLine(46, 82, 45, 93, p);
                cv.drawLine(54, 82, 55, 93, p);
                break;
            case 4: // ranger strap + clasp
                stroke(trim, 4f);
                cv.drawLine(22, 76, 72, 100, p);
                fill(0xFFF5C451);
                cv.drawCircle(48, 88, 2.2f, p);
                break;
            case 5: // royal buttons + epaulettes
                fill(trim);
                cv.drawCircle(50, 93, 1.8f, p);
                cv.drawCircle(50, 98, 1.8f, p);
                rect.set(14, 76, 30, 80);
                cv.drawRoundRect(rect, 2, 2, p);
                rect.set(70, 76, 86, 80);
                cv.drawRoundRect(rect, 2, 2, p);
                break;
            default: // tunic belt
                stroke(trim, 3f);
                cv.drawLine(13, 94, 87, 94, p);
        }
    }

    // ---------------------------------------------------------------- face

    private void drawBrowsAndEyes(Canvas cv, int style, int hair) {
        float[] xs = {42, 58};
        int brow = darker(hair, 0.85f);
        for (int i = 0; i < 2; i++) {
            float x = xs[i];
            boolean left = i == 0;
            switch (style) {
                case 1: // happy: closed smiling eyes
                    stroke(INK, 2.2f);
                    cv.drawArc(x - 3.6f, 44f, x + 3.6f, 51f, 200, 140, false, p);
                    break;
                case 2: // bright: big eyes with blue iris
                    fill(0xFFFFFFFF);
                    rect.set(x - 4, 43, x + 4, 52);
                    cv.drawOval(rect, p);
                    fill(0xFF4F8CFF);
                    cv.drawCircle(x, 48, 3.1f, p);
                    fill(INK);
                    cv.drawCircle(x, 48, 1.6f, p);
                    fill(0xFFFFFFFF);
                    cv.drawCircle(x + 1.1f, 46.8f, 1f, p);
                    break;
                case 3: // focused: narrow eyes, angled brows
                    fill(INK);
                    rect.set(x - 3, 46, x + 3, 50.4f);
                    cv.drawOval(rect, p);
                    break;
                default: // classic
                    fill(INK);
                    cv.drawCircle(x, 48, 2.5f, p);
                    fill(0xFFFFFFFF);
                    cv.drawCircle(x + 0.8f, 47.2f, 0.8f, p);
            }
            stroke(brow, 1.8f);
            if (style == 3) {
                cv.drawLine(x - 4.5f, left ? 41 : 43, x + 4.5f, left ? 43 : 41, p);
            } else {
                path.reset();
                path.moveTo(x - 4.2f, 41.4f);
                path.quadTo(x, 39.2f, x + 4.2f, 41.4f);
                cv.drawPath(path, p);
            }
        }
    }

    private void drawNose(Canvas cv, int skinDark) {
        stroke(darker(skinDark, 0.88f), 1.3f);
        path.reset();
        path.moveTo(49.2f, 50.5f);
        path.quadTo(47.8f, 54.2f, 50.4f, 54.4f);
        cv.drawPath(path, p);
    }

    private void drawMouth(Canvas cv, int style, boolean onBeard) {
        if (onBeard) { // light patch so the mouth stays readable on top of a beard
            fill(lighter(LIP, 0.55f));
            rect.set(43, 59, 57, 64.5f);
            cv.drawOval(rect, p);
        }
        switch (style) {
            case 1: // grin
                path.reset();
                path.moveTo(43, 59);
                path.quadTo(50, 59, 57, 59);
                path.quadTo(50, 70, 43, 59);
                path.close();
                fill(LIP);
                cv.drawPath(path, p);
                fill(0xFFFFFFFF);
                rect.set(45, 59, 55, 61.4f);
                cv.drawRect(rect, p);
                break;
            case 2: // smirk
                stroke(LIP, 2f);
                path.reset();
                path.moveTo(43, 62);
                path.quadTo(50, 64, 57, 59.4f);
                cv.drawPath(path, p);
                break;
            case 3: // calm
                stroke(LIP, 2f);
                cv.drawLine(44, 61.5f, 56, 61.5f, p);
                break;
            default: // smile
                stroke(LIP, 2f);
                path.reset();
                path.moveTo(43, 59.5f);
                path.quadTo(50, 66.5f, 57, 59.5f);
                cv.drawPath(path, p);
        }
    }

    private void drawFreckles(Canvas cv, int skin) {
        fill(darker(skin, 0.68f) & 0x00FFFFFF | 0x99000000);
        float[][] dots = {{36, 53}, {39, 55.5f}, {41.5f, 52.5f}, {64, 53}, {61, 55.5f}, {58.5f, 52.5f},
                {47, 52.5f}, {53, 52.5f}};
        for (float[] d : dots) cv.drawCircle(d[0], d[1], 0.75f, p);
    }

    private void drawMustache(Canvas cv, int hair) {
        fill(hair);
        path.reset();
        path.moveTo(50, 56.8f);
        path.cubicTo(46, 54.2f, 40.5f, 55.6f, 40, 58.6f);
        path.cubicTo(44, 57.6f, 47, 58.4f, 50, 58.2f);
        path.cubicTo(53, 58.4f, 56, 57.6f, 60, 58.6f);
        path.cubicTo(59.5f, 55.6f, 54, 54.2f, 50, 56.8f);
        path.close();
        cv.drawPath(path, p);
    }

    private void drawBeard(Canvas cv, int hair, int skin) {
        fill(hair);
        path.reset();
        path.moveTo(29.5f, 47);
        path.cubicTo(29.5f, 62, 38, 73.5f, 50, 73.8f);
        path.cubicTo(62, 73.5f, 70.5f, 62, 70.5f, 47);
        path.cubicTo(67f, 55, 63, 56.6f, 58, 56.6f);
        path.lineTo(42, 56.6f);
        path.cubicTo(37, 56.6f, 33f, 55, 29.5f, 47);
        path.close();
        cv.drawPath(path, p);
        fill(0x1FFFFFFF);
        rect.set(36, 66, 64, 71);
        cv.drawOval(rect, p);
    }

    private void drawScar(Canvas cv) {
        stroke(0xCCE59A9A, 1.5f);
        cv.drawLine(35.5f, 40.5f, 40.5f, 53, p);
        stroke(0xCCE59A9A, 1f);
        cv.drawLine(34.5f, 45, 38.5f, 43.5f, p);
        cv.drawLine(36.5f, 49.5f, 40.5f, 48, p);
    }

    // ---------------------------------------------------------------- hair

    private void drawHairBack(Canvas cv, int style, int hair) {
        fill(hair);
        if (style == 2) { // long
            rect.set(26, 24, 74, 90);
            cv.drawRoundRect(rect, 22, 22, p);
        } else if (style == 4) { // bun
            cv.drawCircle(50, 14.5f, 9.5f, p);
            fill(darker(hair, 0.82f));
            cv.drawCircle(50, 14.5f, 4.2f, p);
        }
    }

    private void drawHairFront(Canvas cv, int style, int hair) {
        if (style == 0) return;
        fill(hair);
        if (style == 5) { // curly: a cloud of curls
            float[][] cs = {{31, 36, 7.5f}, {39, 27.5f, 8f}, {50, 24.5f, 8.5f}, {61, 27.5f, 8f},
                    {69, 36, 7.5f}, {29.5f, 46, 5.2f}, {70.5f, 46, 5.2f}, {43, 30, 7f}, {57, 30, 7f}};
            for (float[] a : cs) cv.drawCircle(a[0], a[1], a[2], p);
        } else {
            // base cap with a side-swept fringe
            path.reset();
            path.moveTo(28.5f, 47);
            path.cubicTo(25.5f, 29, 35, 19.5f, 50, 19.5f);
            path.cubicTo(65, 19.5f, 74.5f, 29, 71.5f, 47);
            path.cubicTo(70.5f, 41, 69, 36.5f, 66.5f, 33.5f);
            path.cubicTo(60, 38, 46, 36.5f, 37, 30.5f);
            path.cubicTo(34, 35, 30.5f, 41, 28.5f, 47);
            path.close();
            cv.drawPath(path, p);
            if (style == 3) { // spiky
                float[][] t = {{30, 33, 33, 12, 43, 27}, {39, 26, 50, 5, 61, 26}, {57, 27, 67, 12, 70, 33}};
                for (float[] a : t) {
                    path.reset();
                    path.moveTo(a[0], a[1]);
                    path.lineTo(a[2], a[3]);
                    path.lineTo(a[4], a[5]);
                    path.close();
                    cv.drawPath(path, p);
                }
            } else if (style == 2) { // long side locks
                rect.set(27, 38, 34, 76);
                cv.drawRoundRect(rect, 3.5f, 3.5f, p);
                rect.set(66, 38, 73, 76);
                cv.drawRoundRect(rect, 3.5f, 3.5f, p);
            }
        }
        // highlight sweep
        stroke(lighter(hair, 0.35f) & 0x00FFFFFF | 0x88000000, 1.6f);
        path.reset();
        path.moveTo(38, 27);
        path.quadTo(46, 22.5f, 56, 23.5f);
        cv.drawPath(path, p);
    }

    // ---------------------------------------------------------------- accessories

    private void drawAccessory(Canvas cv, int a, int hair) {
        switch (a) {
            case 1: // glasses
                stroke(0xFF2A2347, 1.8f);
                cv.drawCircle(42, 48, 6.3f, p);
                cv.drawCircle(58, 48, 6.3f, p);
                cv.drawLine(48.3f, 47.5f, 51.7f, 47.5f, p);
                cv.drawLine(35.7f, 47.5f, 31.5f, 46.5f, p);
                cv.drawLine(64.3f, 47.5f, 68.5f, 46.5f, p);
                fill(0x2266CCFF);
                cv.drawCircle(42, 48, 6, p);
                cv.drawCircle(58, 48, 6, p);
                break;
            case 2: // crown
                path.reset();
                path.moveTo(33.5f, 30);
                path.lineTo(32.5f, 15);
                path.lineTo(42, 22.5f);
                path.lineTo(50, 10);
                path.lineTo(58, 22.5f);
                path.lineTo(67.5f, 15);
                path.lineTo(66.5f, 30);
                path.close();
                fill(0xFFF5C451);
                cv.drawPath(path, p);
                fill(0xFFD89B1E);
                rect.set(33.5f, 26.5f, 66.5f, 30.5f);
                cv.drawRect(rect, p);
                fill(0xFFB3263F);
                cv.drawCircle(50, 24f, 2.2f, p);
                fill(0xFF4F8CFF);
                cv.drawCircle(40.5f, 26, 1.4f, p);
                cv.drawCircle(59.5f, 26, 1.4f, p);
                break;
            case 3: // headband
                fill(0xFFE0568F);
                rect.set(29.5f, 32.5f, 70.5f, 38f);
                cv.drawRoundRect(rect, 2.7f, 2.7f, p);
                fill(0xFFF5C451);
                cv.drawCircle(50, 35.2f, 2f, p);
                break;
            case 4: // wizard hat
                fill(0xFF4C2FB8);
                rect.set(22, 27.5f, 78, 35);
                cv.drawRoundRect(rect, 3.8f, 3.8f, p);
                path.reset();
                path.moveTo(32, 29.5f);
                path.quadTo(47, 20, 53, 3);
                path.quadTo(60, 14, 68, 29.5f);
                path.close();
                cv.drawPath(path, p);
                fill(0xFFF5C451);
                rect.set(33, 25.5f, 67, 30);
                cv.drawRect(rect, p);
                fill(0xFFFFE08A);
                cv.drawCircle(51, 15, 1.5f, p);
                break;
            default:
        }
    }

    // ---------------------------------------------------------------- frame

    private void drawFrame(Canvas cv, int f, boolean circle) {
        if (f <= 0) return;
        float inset = 2f;
        switch (f) {
            case 1:
                stroke(0xFFF5C451, 4f);
                break;
            case 2:
                if (arcaneShader == null) {
                    arcaneShader = new LinearGradient(0, 0, 100, 100, 0xFFA78BFA, 0xFF4F8CFF, Shader.TileMode.CLAMP);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setShader(arcaneShader);
                break;
            case 3:
                stroke(0xFFD5DAE8, 4f);
                break;
            default:
                stroke(0xFF39D98A, 4f);
        }
        drawOutline(cv, inset, circle);
        p.setShader(null);
        if (f == 1 || f == 3) {
            stroke(f == 1 ? 0x66FFFFFF : 0x558A94AD, 1f);
            drawOutline(cv, inset + 3f, circle);
        }
    }

    private void drawOutline(Canvas cv, float inset, boolean circle) {
        if (circle) {
            cv.drawCircle(50, 50, 50 - inset, p);
        } else {
            rect.set(inset, inset, 100 - inset, 100 - inset);
            cv.drawRoundRect(rect, 18 - inset, 18 - inset, p);
        }
    }
}
