package com.araziarabasi.yildizavi;

import android.graphics.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Level {
    public final int levelNo;
    public final float length;
    public final int timeSeconds;

    public final List<Bonus> bonuses = new ArrayList<>();
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<Checkpoint> checkpoints = new ArrayList<>();

    private final Random random = new Random();

    public Level(int levelNo) {
        this.levelNo = Math.max(1, levelNo);
        this.length = 7800 + this.levelNo * 1550;
        this.timeSeconds = Math.max(120, 245 - this.levelNo * 8);
        generate();
    }

    private void generate() {
        random.setSeed(9000L + levelNo * 131L);

        for (int i = 0; i < 42 + levelNo * 5; i++) {
            float x = 520 + i * 215 + random.nextInt(130);
            bonuses.add(new Bonus(x, getGroundY(x) - 130 - random.nextInt(120), Bonus.STAR));
        }

        for (int i = 0; i < 4 + levelNo; i++) {
            float x = 1150 + i * 1150 + random.nextInt(260);
            bonuses.add(new Bonus(x, getGroundY(x) - 105, Bonus.REPAIR));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 1750 + i * 1650 + random.nextInt(420);
            bonuses.add(new Bonus(x, getGroundY(x) - 115, Bonus.BIG));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 2350 + i * 1550 + random.nextInt(460);
            bonuses.add(new Bonus(x, getGroundY(x) - 115, Bonus.SMALL));
        }

        for (int i = 0; i < 2 + levelNo / 2; i++) {
            float x = 3050 + i * 1700 + random.nextInt(500);
            bonuses.add(new Bonus(x, getGroundY(x) - 115, Bonus.SLOW));
        }

        for (int i = 0; i < 2 + levelNo / 3; i++) {
            float x = 3900 + i * 1850 + random.nextInt(500);
            bonuses.add(new Bonus(x, getGroundY(x) - 110, Bonus.WHEEL));
        }

        for (int i = 0; i < 5 + levelNo; i++) {
            float x = 1500 + i * 980 + random.nextInt(300);
            bonuses.add(new Bonus(x, getGroundY(x) - 105, Bonus.MINUS));
        }

        for (int i = 0; i < 8 + levelNo * 2; i++) {
            float x = 950 + i * 760 + random.nextInt(320);
            obstacles.add(new Obstacle(x, getGroundY(x) - 20, 30 + random.nextInt(16)));
        }

        checkpoints.add(new Checkpoint(length / 3f, false));
        checkpoints.add(new Checkpoint(length * 2f / 3f, false));
        checkpoints.add(new Checkpoint(length, true));
    }

    public float getGroundY(float x) {
        float base = 525
                + (float)Math.sin(x * (0.0068f + levelNo * 0.00035f)) * 80
                + (float)Math.sin(x * 0.016f) * 46
                + (float)Math.sin(x * 0.039f) * 17;

        float ramp = 0;
        ramp += rampWave(x, 950, 360, -85);
        ramp += rampWave(x, 1850, 430, 95);
        ramp += rampWave(x, 3050, 480, -110);
        ramp += rampWave(x, 4550, 520, 115);
        ramp += rampWave(x, 6100, 500, -95);
        return base + ramp;
    }

    private float rampWave(float x, float start, float width, float height) {
        float t = (x - start) / width;
        if (t < 0 || t > 1) return 0;
        return (float)Math.sin(t * Math.PI) * height;
    }

    public float getGroundAngle(float x) {
        float y1 = getGroundY(x - 28);
        float y2 = getGroundY(x + 28);
        return (float)Math.toDegrees(Math.atan2(y2 - y1, 56));
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
        for (int sx = 0; sx <= w + 30; sx += 18) {
            float wx = camX + sx;
            ground.lineTo(sx, getGroundY(wx));
        }
        ground.lineTo(w, h);
        ground.close();
        canvas.drawPath(ground, paint);

        paint.setColor(Color.rgb(101, 67, 32));
        for (int sx = 0; sx <= w; sx += 70) {
            float wx = camX + sx;
            float gy = getGroundY(wx);
            canvas.drawCircle(sx, gy + 35, 9, paint);
        }

        paint.setColor(Color.rgb(35, 25, 14));
        paint.setStrokeWidth(5);
        Path top = new Path();
        top.moveTo(0, getGroundY(camX));
        for (int sx = 0; sx <= w + 30; sx += 18) {
            float wx = camX + sx;
            top.lineTo(sx, getGroundY(wx));
        }
        canvas.drawPath(top, paint);
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

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(c.finish ? Color.RED : Color.CYAN);
            canvas.drawRect(sx, gy - 150, sx + 46, gy - 116, paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(17);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(c.finish ? "FINISH" : "CHECK", sx - 10, gy - 158, paint);
        }

        for (Bonus b : bonuses) b.draw(canvas, paint, camX);

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

            paint.setColor(Color.rgb(85, 85, 85));
            canvas.drawCircle(sx - 5, o.y, o.size * 0.18f, paint);
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
            this.passed = false;
        }
    }
}
