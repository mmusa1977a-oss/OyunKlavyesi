package com.araziarabasi.yildizavi;

import android.graphics.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuneLevel {
    public final int levelNo;
    public final float length;
    public final int timeSeconds;
    public final List<BonusItem> bonuses = new ArrayList<>();
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<Checkpoint> checkpoints = new ArrayList<>();

    private final Random random = new Random();

    public DuneLevel(int levelNo) {
        this.levelNo = Math.max(1, levelNo);
        this.length = 7600 + this.levelNo * 1450;
        this.timeSeconds = Math.max(120, 245 - this.levelNo * 8);
        generate();
    }

    private void generate() {
        random.setSeed(1515L + levelNo * 91L);

        for (int i = 0; i < 45 + levelNo * 4; i++) {
            float x = 520 + i * 220 + random.nextInt(110);
            bonuses.add(new BonusItem(x, getGroundY(x) - 135 - random.nextInt(115), BonusItem.STAR));
        }

        for (int i = 0; i < 4 + levelNo; i++) {
            float x = 1200 + i * 1100 + random.nextInt(260);
            bonuses.add(new BonusItem(x, getGroundY(x) - 110, BonusItem.REPAIR));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 1900 + i * 1650 + random.nextInt(420);
            bonuses.add(new BonusItem(x, getGroundY(x) - 115, BonusItem.BIG));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 2500 + i * 1550 + random.nextInt(420);
            bonuses.add(new BonusItem(x, getGroundY(x) - 115, BonusItem.SMALL));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 3150 + i * 1700 + random.nextInt(420);
            bonuses.add(new BonusItem(x, getGroundY(x) - 115, BonusItem.SLOW));
        }

        for (int i = 0; i < 4 + levelNo; i++) {
            float x = 1550 + i * 1050 + random.nextInt(300);
            bonuses.add(new BonusItem(x, getGroundY(x) - 105, BonusItem.MINUS));
        }

        for (int i = 0; i < 8 + levelNo * 2; i++) {
            float x = 1000 + i * 760 + random.nextInt(300);
            obstacles.add(new Obstacle(x, getGroundY(x) - 22, 28 + random.nextInt(20)));
        }

        checkpoints.add(new Checkpoint(length / 3f, false));
        checkpoints.add(new Checkpoint(length * 2f / 3f, false));
        checkpoints.add(new Checkpoint(length, true));
    }

    public float getGroundY(float x) {
        float y = 525
                + (float)Math.sin(x * (0.0065f + levelNo * 0.00035f)) * 82
                + (float)Math.sin(x * 0.0155f) * 48
                + (float)Math.sin(x * 0.038f) * 18;

        y += ramp(x, 900, 350, -90);
        y += ramp(x, 1780, 440, 95);
        y += ramp(x, 3060, 500, -115);
        y += ramp(x, 4620, 540, 120);
        y += ramp(x, 6250, 520, -105);
        return y;
    }

    private float ramp(float x, float start, float width, float height) {
        float t = (x - start) / width;
        if (t < 0 || t > 1) return 0;
        return (float)Math.sin(t * Math.PI) * height;
    }

    public float getGroundAngle(float x) {
        return (float)Math.atan2(getGroundY(x + 24) - getGroundY(x - 24), 48);
    }

    public void drawBackground(Canvas canvas, Paint paint, float camX) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        int sky;
        if (levelNo % 4 == 1) sky = Color.rgb(225, 150, 55);
        else if (levelNo % 4 == 2) sky = Color.rgb(95, 150, 205);
        else if (levelNo % 4 == 3) sky = Color.rgb(190, 110, 70);
        else sky = Color.rgb(70, 70, 90);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sky);
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(245, 185, 70));
        canvas.drawCircle(w - 230, 175, 92, paint);

        paint.setColor(Color.argb(160, 115, 78, 40));
        Path far = new Path();
        far.moveTo(0, 395);
        for (int sx = 0; sx <= w; sx += 40) {
            far.lineTo(sx, 395 + (float)Math.sin((sx + camX * 0.15f) * 0.007f) * 55);
        }
        far.lineTo(w, h);
        far.lineTo(0, h);
        far.close();
        canvas.drawPath(far, paint);
    }

    public void drawGround(Canvas canvas, Paint paint, float camX) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(68, 43, 20));
        Path ground = new Path();
        ground.moveTo(0, h);
        for (int sx = 0; sx <= w + 30; sx += 16) {
            ground.lineTo(sx, getGroundY(camX + sx));
        }
        ground.lineTo(w, h);
        ground.close();
        canvas.drawPath(ground, paint);

        paint.setColor(Color.rgb(35, 25, 14));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        Path top = new Path();
        top.moveTo(0, getGroundY(camX));
        for (int sx = 0; sx <= w + 30; sx += 16) {
            top.lineTo(sx, getGroundY(camX + sx));
        }
        canvas.drawPath(top, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public void drawObjects(Canvas canvas, Paint paint, float camX) {
        int w = canvas.getWidth();

        for (Checkpoint c : checkpoints) {
            float sx = c.x - camX;
            if (sx < -120 || sx > w + 120) continue;

            float gy = getGroundY(c.x);
            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            canvas.drawLine(sx, gy - 150, sx, gy, paint);
            paint.setColor(c.finish ? Color.RED : Color.CYAN);
            canvas.drawRect(sx, gy - 150, sx + 46, gy - 116, paint);
        }

        for (BonusItem b : bonuses) b.draw(canvas, paint, camX);

        for (Obstacle o : obstacles) {
            float sx = o.x - camX;
            if (sx < -100 || sx > w + 100) continue;

            paint.setColor(Color.rgb(35, 35, 35));
            Path p = new Path();
            p.moveTo(sx - o.size, o.y + o.size * 0.7f);
            p.lineTo(sx, o.y - o.size);
            p.lineTo(sx + o.size, o.y + o.size * 0.7f);
            p.close();
            canvas.drawPath(p, paint);
        }
    }

    public static class Obstacle {
        public float x, y, size;

        public Obstacle(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    public static class Checkpoint {
        public float x;
        public boolean finish;
        public boolean passed;

        public Checkpoint(float x, boolean finish) {
            this.x = x;
            this.finish = finish;
        }
    }
}
