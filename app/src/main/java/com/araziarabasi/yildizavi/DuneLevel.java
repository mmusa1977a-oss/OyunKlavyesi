package com.araziarabasi.yildizavi;

import android.graphics.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuneLevel {
    public final int levelNo;
    public final float startX;
    public final float startY;
    public final float finishX;
    public final float finishY;
    public final float length;
    public final int timeSeconds;

    public final List<BonusItem> bonuses = new ArrayList<>();
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<Checkpoint> checkpoints = new ArrayList<>();

    private final Random random = new Random();

    public DuneLevel(int levelNo) {
        this.levelNo = Math.max(1, levelNo);

        this.startX = DuneGroundData.startX(this.levelNo);
        this.startY = DuneGroundData.startY(this.levelNo);
        this.finishX = DuneGroundData.endX(this.levelNo);
        this.finishY = DuneGroundData.finishY(this.levelNo);
        this.length = Math.max(1000f, finishX - startX);
        this.timeSeconds = DuneGroundData.timeSeconds(this.levelNo);

        generate();
    }

    private void generate() {
        random.setSeed(1515L + levelNo * 91L);

        float usableStart = startX + 450f;
        float usableEnd = finishX - 450f;
        float usableLength = Math.max(1000f, usableEnd - usableStart);

        // Yıldızlar orijinal oyundaki gibi yol boyunca aralıklı.
        int starCount = 34 + levelNo * 3;
        for (int i = 0; i < starCount; i++) {
            float t = i / (float)Math.max(1, starCount - 1);
            float x = usableStart + t * usableLength + random.nextInt(120) - 60;
            float y = getGroundY(x) - 120f - random.nextInt(85);
            bonuses.add(new BonusItem(x, y, BonusItem.STAR));
        }

        // Tamir bonusu.
        for (int i = 0; i < 3 + levelNo / 2; i++) {
            float x = usableStart + usableLength * (0.18f + i * 0.22f) + random.nextInt(160) - 80;
            if (x < usableEnd) bonuses.add(new BonusItem(x, getGroundY(x) - 105f, BonusItem.REPAIR));
        }

        // Büyük / küçük / slow bonusları.
        addRareBonus(0.30f, BonusItem.BIG);
        addRareBonus(0.52f, BonusItem.SMALL);
        addRareBonus(0.72f, BonusItem.SLOW);

        // Negatif bonus.
        for (int i = 0; i < 4 + levelNo / 2; i++) {
            float x = usableStart + usableLength * (0.20f + i * 0.17f) + random.nextInt(180) - 90;
            if (x < usableEnd) bonuses.add(new BonusItem(x, getGroundY(x) - 95f, BonusItem.MINUS));
        }

        // Taş/engel daha az ve küçük; eski oyundaki gibi yolu tamamen dağ yapmıyoruz.
        for (int i = 0; i < 5 + levelNo; i++) {
            float x = usableStart + usableLength * (0.12f + i * 0.15f) + random.nextInt(220) - 110;
            if (x < usableEnd) obstacles.add(new Obstacle(x, getGroundY(x) - 20f, 24f + random.nextInt(14)));
        }

        checkpoints.add(new Checkpoint(startX + length / 3f, false));
        checkpoints.add(new Checkpoint(startX + length * 2f / 3f, false));
        checkpoints.add(new Checkpoint(finishX, true));
    }

    private void addRareBonus(float ratio, int type) {
        float x = startX + length * ratio + random.nextInt(260) - 130;
        if (x > startX && x < finishX) bonuses.add(new BonusItem(x, getGroundY(x) - 110f, type));
    }

    public float getGroundY(float x) {
        return DuneGroundData.approximateGroundY(levelNo, x);
    }

    public float getGroundAngle(float x) {
        return (float)Math.atan2(getGroundY(x + 24f) - getGroundY(x - 24f), 48f);
    }

    public void drawBackground(Canvas canvas, Paint paint, float camX) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        int sky;
        if (levelNo % 4 == 1) sky = Color.rgb(222, 149, 55);
        else if (levelNo % 4 == 2) sky = Color.rgb(150, 120, 76);
        else if (levelNo % 4 == 3) sky = Color.rgb(95, 150, 205);
        else sky = Color.rgb(80, 80, 92);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sky);
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(245, 185, 70));
        canvas.drawCircle(w - 230, 175, 92, paint);

        // Uzak arka plan: düz, büyük dağ gibi değil.
        paint.setColor(Color.argb(135, 115, 78, 40));
        Path far = new Path();
        far.moveTo(0, 365);
        for (int sx = 0; sx <= w; sx += 40) {
            far.lineTo(sx, 365 + (float)Math.sin((sx + camX * 0.08f) * 0.005f) * 28);
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
        paint.setColor(Color.rgb(65, 42, 20));
        Path ground = new Path();
        ground.moveTo(0, h);

        for (int sx = 0; sx <= w + 30; sx += 12) {
            ground.lineTo(sx, getGroundY(camX + sx));
        }

        ground.lineTo(w, h);
        ground.close();
        canvas.drawPath(ground, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.rgb(34, 24, 14));

        Path top = new Path();
        top.moveTo(0, getGroundY(camX));
        for (int sx = 0; sx <= w + 30; sx += 12) {
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
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(c.finish ? Color.RED : Color.CYAN);
            canvas.drawRect(sx, gy - 150, sx + 46, gy - 116, paint);
        }

        for (BonusItem b : bonuses) b.draw(canvas, paint, camX);

        for (Obstacle o : obstacles) {
            float sx = o.x - camX;
            if (sx < -100 || sx > w + 100) continue;

            paint.setStyle(Paint.Style.FILL);
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
