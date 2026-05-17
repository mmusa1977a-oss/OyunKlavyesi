package com.araziarabasi.yildizavi;

import android.content.Context;
import android.graphics.*;
import android.view.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Dune Buggy tarzı gelişmiş Android GameView.
 * Mevcut GameView.java dosyanın tamamını bununla değiştir.
 */
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean running = false;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private static final int STATE_MENU = 0;
    private static final int STATE_HOW = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_GAME_OVER = 3;
    private static final int STATE_LEVEL_COMPLETE = 4;

    private static final int ITEM_STAR = 1;
    private static final int ITEM_REPAIR = 2;
    private static final int ITEM_BIG = 3;
    private static final int ITEM_SMALL = 4;
    private static final int ITEM_SLOW = 5;
    private static final int ITEM_MINUS = 6;
    private static final int ITEM_WHEEL = 7;

    private int gameState = STATE_MENU;
    private int level = 1;

    private float levelLength;
    private int levelTimeSeconds;
    private long levelStartTime;

    private Wheel backWheel;
    private Wheel frontWheel;

    private float bodyX;
    private float bodyY;
    private float bodyAngle;
    private float bodyAngleVelocity;

    private float camX;
    private float camTargetX;

    private boolean gas, brake, left, right, jump;
    private boolean wasOnGround = false;

    private int score = 0;
    private int health = 10;
    private int starsCollected = 0;

    private long lastDamageTime = 0;
    private long bonusTextTime = 0;
    private String bonusText = "";

    private long bigUntil = 0;
    private long smallUntil = 0;
    private long slowUntil = 0;
    private long wheelUntil = 0;

    private float airSpin = 0;
    private int checkpointIndex = 0;

    private RectF playBtn, howBtn, backBtn;
    private RectF gasBtn, brakeBtn, leftBtn, rightBtn, jumpBtn;

    private final List<Item> items = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Checkpoint> checkpoints = new ArrayList<>();

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        resetLevel(1);
    }

    private void resetLevel(int newLevel) {
        level = Math.max(1, newLevel);
        levelLength = 7800 + level * 1550;
        levelTimeSeconds = Math.max(120, 245 - level * 8);

        score = 0;
        health = 10;
        starsCollected = 0;
        checkpointIndex = 0;

        gas = brake = left = right = jump = false;
        wasOnGround = false;

        bigUntil = 0;
        smallUntil = 0;
        slowUntil = 0;
        wheelUntil = 0;
        lastDamageTime = 0;
        bonusText = "";
        airSpin = 0;

        float startX = 260;
        float gy = getGroundY(startX);
        backWheel = new Wheel(startX - 55, gy - 45);
        frontWheel = new Wheel(startX + 55, gy - 45);

        bodyX = startX;
        bodyY = gy - 95;
        bodyAngle = 0;
        bodyAngleVelocity = 0;

        camX = 0;
        camTargetX = 0;

        items.clear();
        obstacles.clear();
        particles.clear();
        checkpoints.clear();

        generateLevel();
        levelStartTime = System.currentTimeMillis();
    }

    private void generateLevel() {
        random.setSeed(9000L + level * 131L);

        for (int i = 0; i < 42 + level * 5; i++) {
            float x = 520 + i * 215 + random.nextInt(130);
            float y = getGroundY(x) - 130 - random.nextInt(120);
            items.add(new Item(x, y, ITEM_STAR));
        }

        for (int i = 0; i < 4 + level; i++) {
            float x = 1150 + i * 1150 + random.nextInt(260);
            items.add(new Item(x, getGroundY(x) - 105, ITEM_REPAIR));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 1750 + i * 1650 + random.nextInt(420);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_BIG));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 2350 + i * 1550 + random.nextInt(460);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_SMALL));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 3050 + i * 1700 + random.nextInt(500);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_SLOW));
        }

        for (int i = 0; i < 2 + level / 3; i++) {
            float x = 3900 + i * 1850 + random.nextInt(500);
            items.add(new Item(x, getGroundY(x) - 110, ITEM_WHEEL));
        }

        for (int i = 0; i < 5 + level; i++) {
            float x = 1500 + i * 980 + random.nextInt(300);
            items.add(new Item(x, getGroundY(x) - 105, ITEM_MINUS));
        }

        for (int i = 0; i < 8 + level * 2; i++) {
            float x = 950 + i * 760 + random.nextInt(320);
            obstacles.add(new Obstacle(x, getGroundY(x) - 20, 30 + random.nextInt(16)));
        }

        checkpoints.add(new Checkpoint(levelLength / 3f, false));
        checkpoints.add(new Checkpoint(levelLength * 2f / 3f, false));
        checkpoints.add(new Checkpoint(levelLength, true));
    }

    @Override
    public void run() {
        while (running) {
            update();
            drawGame();
            sleep();
        }
    }

    private void update() {
        if (gameState != STATE_PLAYING) return;

        long now = System.currentTimeMillis();

        if (getSecondsLeft() <= 0 || health <= 0) {
            gameState = STATE_GAME_OVER;
            return;
        }

        float timeScale = now < slowUntil ? 0.68f : 1.0f;
        float scale = getCarScale();

        updateCarPhysics(timeScale, scale);
        updateCheckpointAndFinish();
        collectItems();
        hitObstacles();
        updateParticles();

        camTargetX = bodyX - getWidth() * 0.35f;
        if (camTargetX < 0) camTargetX = 0;
        camX += (camTargetX - camX) * 0.10f;
    }

    private void updateCarPhysics(float timeScale, float scale) {
        boolean backGround = solveWheelGround(backWheel, scale);
        boolean frontGround = solveWheelGround(frontWheel, scale);
        boolean onGround = backGround || frontGround;

        float engine = 0.58f * timeScale;
        float reverse = 0.42f * timeScale;
        float wheelGrip = onGround ? 0.985f : 0.998f;

        if (gas) {
            backWheel.vx += engine;
            frontWheel.vx += engine * 0.35f;
        }

        if (brake) {
            backWheel.vx -= reverse;
            frontWheel.vx -= reverse * 0.35f;
        }

        if (jump && onGround) {
            backWheel.vy -= 8.5f;
            frontWheel.vy -= 8.5f;
            addDust(bodyX, bodyY + 55, 12);
        }

        if (left) bodyAngleVelocity -= (onGround ? 0.38f : 0.82f) * timeScale;
        if (right) bodyAngleVelocity += (onGround ? 0.38f : 0.82f) * timeScale;

        applyWheelPhysics(backWheel, wheelGrip, timeScale);
        applyWheelPhysics(frontWheel, wheelGrip, timeScale);

        constrainWheels(scale);
        updateBodyFromWheels(scale, timeScale, onGround);

        if (!onGround) {
            airSpin += bodyAngleVelocity;
            while (Math.abs(airSpin) >= 360f) {
                score += 100;
                showBonus("FLIP +100");
                airSpin += airSpin > 0 ? -360f : 360f;
            }
        } else {
            if (!wasOnGround) handleLandingDamage();
            airSpin = 0;
        }

        wasOnGround = onGround;

        if (bodyY > getHeight() + 450) {
            damageCar(4, "DÜŞTÜN");
            respawnAtCheckpoint();
        }
    }

    private void applyWheelPhysics(Wheel w, float grip, float timeScale) {
        w.vy += 0.63f * timeScale;
        w.vx *= grip;
        w.vy *= 0.995f;

        if (Math.abs(w.vx) > 23f) w.vx = Math.signum(w.vx) * 23f;

        w.x += w.vx * timeScale;
        w.y += w.vy * timeScale;

        w.rotation += w.vx * 4.3f * timeScale;
    }

    private boolean solveWheelGround(Wheel w, float scale) {
        float radius = getWheelRadius(scale);
        float gy = getGroundY(w.x);
        boolean hit = false;

        if (w.y + radius > gy) {
            float impact = w.vy;
            w.y = gy - radius;
            w.vy = -impact * 0.12f;
            if (Math.abs(w.vy) < 1.2f) w.vy = 0;

            float slope = getGroundAngle(w.x);
            w.vx += (float)Math.sin(Math.toRadians(slope)) * 0.22f;
            hit = true;
        }

        return hit;
    }

    private void constrainWheels(float scale) {
        float targetDist = 112f * scale;
        float dx = frontWheel.x - backWheel.x;
        float dy = frontWheel.y - backWheel.y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001f) return;

        float diff = (dist - targetDist) / dist;
        float pushX = dx * diff * 0.5f;
        float pushY = dy * diff * 0.5f;

        backWheel.x += pushX;
        backWheel.y += pushY;
        frontWheel.x -= pushX;
        frontWheel.y -= pushY;

        float springY = ((backWheel.y + frontWheel.y) / 2f) - (70f * scale);
        bodyY += (springY - bodyY) * 0.34f;

        float midX = (backWheel.x + frontWheel.x) / 2f;
        bodyX += (midX - bodyX) * 0.45f;
    }

    private void updateBodyFromWheels(float scale, float timeScale, boolean onGround) {
        float dx = frontWheel.x - backWheel.x;
        float dy = frontWheel.y - backWheel.y;
        float wheelAngle = (float)Math.toDegrees(Math.atan2(dy, dx));

        float targetAngle = wheelAngle + bodyAngleVelocity;
        bodyAngle += normalizeAngle(targetAngle - bodyAngle) * (onGround ? 0.19f : 0.08f);
        bodyAngle += bodyAngleVelocity * 0.18f * timeScale;
        bodyAngleVelocity *= onGround ? 0.90f : 0.97f;

        float midX = (backWheel.x + frontWheel.x) / 2f;
        float midY = (backWheel.y + frontWheel.y) / 2f;

        bodyX = midX;
        bodyY += ((midY - 58f * scale) - bodyY) * 0.26f;
    }

    private void handleLandingDamage() {
        long now = System.currentTimeMillis();
        if (now - lastDamageTime < 650) return;

        float impact = Math.max(backWheel.vy, frontWheel.vy);
        float angle = Math.abs(normalizeAngle(bodyAngle));
        int damage = 0;

        if (impact > 18) damage += 3;
        else if (impact > 13) damage += 2;
        else if (impact > 9) damage += 1;

        if (angle > 145) damage += 3;
        else if (angle > 105) damage += 2;
        else if (angle > 75) damage += 1;

        if (damage > 0) {
            damageCar(damage, "-" + damage + " CAN");
            addSparks(bodyX, bodyY, 20);
        }
    }

    private void updateCheckpointAndFinish() {
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint c = checkpoints.get(i);
            if (!c.passed && bodyX >= c.x) {
                c.passed = true;
                checkpointIndex = i;
                if (c.finish) {
                    gameState = STATE_LEVEL_COMPLETE;
                    showBonus("LEVEL COMPLETE");
                    score += getSecondsLeft() * 3;
                } else {
                    showBonus("CHECKPOINT");
                    score += 50;
                }
            }
        }
    }

    private void respawnAtCheckpoint() {
        float x = 260;
        if (checkpointIndex >= 0 && checkpointIndex < checkpoints.size()) {
            x = Math.max(260, checkpoints.get(checkpointIndex).x - 220);
        }

        float gy = getGroundY(x);
        backWheel = new Wheel(x - 55, gy - 45);
        frontWheel = new Wheel(x + 55, gy - 45);
        bodyX = x;
        bodyY = gy - 95;
        bodyAngle = 0;
        bodyAngleVelocity = 0;
    }

    private void collectItems() {
        Iterator<Item> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            float dx = item.x - bodyX;
            float dy = item.y - bodyY;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < 72 * getCarScale()) {
                applyItem(item.type);
                it.remove();
            }
        }
    }

    private void applyItem(int type) {
        long now = System.currentTimeMillis();

        if (type == ITEM_STAR) {
            score += 10;
            starsCollected++;
            showBonus("+10");
            addStarParticles(bodyX, bodyY, 10);
        } else if (type == ITEM_REPAIR) {
            health = Math.min(10, health + 3);
            showBonus("+3 CAN");
        } else if (type == ITEM_BIG) {
            bigUntil = now + 9000;
            smallUntil = 0;
            showBonus("BÜYÜK ARABA");
        } else if (type == ITEM_SMALL) {
            smallUntil = now + 9000;
            bigUntil = 0;
            showBonus("KÜÇÜK ARABA");
        } else if (type == ITEM_SLOW) {
            slowUntil = now + 7000;
            showBonus("YAVAŞ ZAMAN");
        } else if (type == ITEM_MINUS) {
            score = Math.max(0, score - 25);
            showBonus("-25");
            addSparks(bodyX, bodyY, 14);
        } else if (type == ITEM_WHEEL) {
            wheelUntil = now + 8500;
            showBonus("TEKER MODU");
        }
    }

    private void hitObstacles() {
        long now = System.currentTimeMillis();

        for (Obstacle o : obstacles) {
            float dx = o.x - bodyX;
            float dy = o.y - bodyY;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < (o.size + 45) * getCarScale() && now - lastDamageTime > 700) {
                damageCar(1, "-1 CAN");
                float kick = bodyX < o.x ? -1 : 1;
                backWheel.vx += kick * -8f;
                frontWheel.vx += kick * -8f;
                backWheel.vy = -8f;
                frontWheel.vy = -8f;
                bodyAngleVelocity += kick * 7f;
                addSparks(o.x, o.y, 18);
            }
        }
    }

    private void damageCar(int damage, String text) {
        long now = System.currentTimeMillis();
        health -= damage;
        lastDamageTime = now;
        showBonus(text);
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.24f;
            p.life--;
            if (p.life <= 0) it.remove();
        }
    }

    private float getCarScale() {
        long now = System.currentTimeMillis();
        if (now < bigUntil) return 1.25f;
        if (now < smallUntil) return 0.76f;
        return 1.0f;
    }

    private float getWheelRadius(float scale) {
        long now = System.currentTimeMillis();
        if (now < wheelUntil) return 38f * scale;
        return 28f * scale;
    }

    private int getSecondsLeft() {
        int elapsed = (int)((System.currentTimeMillis() - levelStartTime) / 1000);
        return Math.max(levelTimeSeconds - elapsed, 0);
    }

    private String getTimeText() {
        int left = getSecondsLeft();
        return String.format("%02d:%02d", left / 60, left % 60);
    }

    private float getGroundY(float x) {
        float base = 525
                + (float)Math.sin(x * (0.0068f + level * 0.00035f)) * 80
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

    private float getGroundAngle(float x) {
        float y1 = getGroundY(x - 28);
        float y2 = getGroundY(x + 28);
        return (float)Math.toDegrees(Math.atan2(y2 - y1, 56));
    }

    private float normalizeAngle(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    private void showBonus(String text) {
        bonusText = text;
        bonusTextTime = System.currentTimeMillis();
    }

    private void drawGame() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        if (gameState == STATE_MENU) {
            drawMenu(canvas, w, h);
        } else if (gameState == STATE_HOW) {
            drawHow(canvas, w, h);
        } else {
            drawPlaying(canvas, w, h);
            if (gameState == STATE_GAME_OVER) {
                drawCenterMessage(canvas, w, h, "GAME OVER", "Ekrana dokun: Menü");
            } else if (gameState == STATE_LEVEL_COMPLETE) {
                drawCenterMessage(canvas, w, h, "LEVEL COMPLETE", "Ekrana dokun: Sonraki bölüm");
            }
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawMenu(Canvas canvas, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(43, 43, 43));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(95, 95, 95));
        canvas.drawRect(60, 40, w - 60, h - 40, paint);

        paint.setColor(Color.rgb(25, 25, 25));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(12);
        canvas.drawRect(70, 50, w - 70, h - 50, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(68);
        paint.setColor(Color.BLACK);
        canvas.drawText("ARAZİ ARABASI", w / 2f, 145, paint);

        paint.setTextSize(38);
        paint.setColor(Color.YELLOW);
        canvas.drawText("YILDIZ AVI", w / 2f, 195, paint);

        drawCarShape(canvas, w / 2f, h / 2f - 45, -8, 1.25f);

        playBtn = new RectF(w / 2f - 190, h / 2f + 70, w / 2f + 190, h / 2f + 142);
        howBtn = new RectF(w / 2f - 190, h / 2f + 160, w / 2f + 190, h / 2f + 225);

        drawMetalButton(canvas, playBtn, "PLAY ▶", 40);
        drawMetalButton(canvas, howBtn, "NASIL OYNANIR", 28);

        paint.setTextSize(23);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Bölüm " + level, w / 2f, h - 75, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHow(Canvas canvas, int w, int h) {
        paint.setColor(Color.rgb(48, 48, 48));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.WHITE);
        paint.setTextSize(52);
        canvas.drawText("NASIL OYNANIR", w / 2f, 90, paint);

        paint.setTextSize(29);
        canvas.drawText("Gaz: hızlan", w / 2f, 165, paint);
        canvas.drawText("Fren: yavaşla / geri bas", w / 2f, 215, paint);
        canvas.drawText("Sol / Sağ: havada denge ve takla", w / 2f, 265, paint);
        canvas.drawText("Zıpla: rampada arabayı kaldır", w / 2f, 315, paint);
        canvas.drawText("Yıldız topla, bonusları kap, finish çizgisine ulaş.", w / 2f, 380, paint);
        canvas.drawText("Takla: FLIP +100", w / 2f, 430, paint);

        backBtn = new RectF(w / 2f - 170, h - 125, w / 2f + 170, h - 60);
        drawMetalButton(canvas, backBtn, "GERİ", 34);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPlaying(Canvas canvas, int w, int h) {
        drawBackground(canvas, w, h);
        drawCheckpoints(canvas, w);
        drawItems(canvas, w);
        drawObstacles(canvas, w);
        drawGround(canvas, w, h);
        drawParticles(canvas);
        drawCar(canvas);
        drawHud(canvas, w);
        drawStartText(canvas);
        drawBonusText(canvas, w);
        drawButtons(canvas, w, h);
    }

    private void drawBackground(Canvas canvas, int w, int h) {
        int sky;
        if (level % 4 == 1) sky = Color.rgb(225, 150, 55);
        else if (level % 4 == 2) sky = Color.rgb(95, 150, 205);
        else if (level % 4 == 3) sky = Color.rgb(190, 110, 70);
        else sky = Color.rgb(70, 70, 90);

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

    private void drawGround(Canvas canvas, int w, int h) {
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

    private void drawCheckpoints(Canvas canvas, int w) {
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
    }

    private void drawItems(Canvas canvas, int w) {
        for (Item item : items) {
            float sx = item.x - camX;
            if (sx < -80 || sx > w + 80) continue;

            if (item.type == ITEM_STAR) drawStar(canvas, sx, item.y, 22);
            else if (item.type == ITEM_REPAIR) drawCircleItem(canvas, sx, item.y, Color.GREEN, "+");
            else if (item.type == ITEM_BIG) drawCircleItem(canvas, sx, item.y, Color.MAGENTA, "B");
            else if (item.type == ITEM_SMALL) drawCircleItem(canvas, sx, item.y, Color.CYAN, "S");
            else if (item.type == ITEM_SLOW) drawCircleItem(canvas, sx, item.y, Color.BLUE, "T");
            else if (item.type == ITEM_MINUS) drawCircleItem(canvas, sx, item.y, Color.RED, "-");
            else if (item.type == ITEM_WHEEL) drawCircleItem(canvas, sx, item.y, Color.rgb(255, 145, 0), "W");
        }
    }

    private void drawCircleItem(Canvas canvas, float x, float y, int color, String text) {
        paint.setColor(Color.BLACK);
        canvas.drawCircle(x + 3, y + 3, 25, paint);
        paint.setColor(color);
        canvas.drawCircle(x, y, 25, paint);

        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(28);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y + 10, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawObstacles(Canvas canvas, int w) {
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

    private void drawParticles(Canvas canvas) {
        for (Particle p : particles) {
            paint.setColor(p.color);
            canvas.drawCircle(p.x - camX, p.y, p.size, paint);
        }
    }

    private void drawCar(Canvas canvas) {
        drawCarShape(canvas, bodyX - camX, bodyY, bodyAngle, getCarScale());
    }

    private void drawCarShape(Canvas canvas, float x, float y, float angle, float scale) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle);
        canvas.scale(scale, scale);

        float wr = getWheelRadius(1f);

        paint.setStrokeWidth(8);
        paint.setColor(Color.rgb(70, 70, 65));
        canvas.drawLine(-52, 24, 52, 24, paint);
        canvas.drawLine(-52, 24, -20, -30, paint);
        canvas.drawLine(52, 24, 25, -30, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(214, 214, 190));
        canvas.drawRoundRect(new RectF(-78, -42, 78, 20), 18, 18, paint);

        paint.setColor(Color.rgb(135, 135, 120));
        canvas.drawRect(-42, -67, 32, -39, paint);

        paint.setColor(Color.rgb(60, 60, 50));
        canvas.drawRect(-62, -54, -20, -41, paint);
        canvas.drawRect(12, -54, 56, -41, paint);

        drawWheel(canvas, -55, 30, wr);
        drawWheel(canvas, 55, 30, wr);

        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(-30, -84);
        flag.lineTo(30, -68);
        flag.lineTo(-30, -52);
        flag.close();
        canvas.drawPath(flag, paint);

        canvas.restore();
    }

    private void drawWheel(Canvas canvas, float x, float y, float r) {
        paint.setColor(Color.rgb(22, 22, 22));
        canvas.drawCircle(x, y, r, paint);

        paint.setColor(Color.rgb(70, 70, 70));
        canvas.drawCircle(x, y, r * 0.68f, paint);

        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(x, y, r * 0.33f, paint);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        canvas.drawLine(x - r * 0.55f, y, x + r * 0.55f, y, paint);
        canvas.drawLine(x, y - r * 0.55f, x, y + r * 0.55f, paint);
    }

    private void drawHud(Canvas canvas, int w) {
        int y = 18;
        drawHudBox(canvas, 15, y, 210, 50, "TIME", getTimeText());
        drawHudBox(canvas, 240, y, 300, 50, "HEALTH", "");
        drawHealthBar(canvas, 355, y + 15, 150, 20);
        drawHudBox(canvas, 555, y, 180, 50, "SCORE", String.valueOf(score));
        drawHudBox(canvas, w - 330, y, 310, 50, "PROGRESS", "");
        drawProgressBar(canvas, w - 160, y + 15, 130, 20);
    }

    private void drawHudBox(Canvas canvas, int x, int y, int width, int height, String title, String value) {
        paint.setColor(Color.rgb(40, 40, 40));
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setColor(Color.rgb(75, 75, 75));
        canvas.drawRect(x + 5, y + 5, x + width - 5, y + height - 5, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(24);
        paint.setColor(Color.WHITE);
        canvas.drawText(title, x + 15, y + 33, paint);

        if (!value.isEmpty()) {
            paint.setColor(Color.BLACK);
            canvas.drawRect(x + width - 100, y + 8, x + width - 12, y + height - 8, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(value, x + width - 92, y + 34, paint);
        }
    }

    private void drawHealthBar(Canvas canvas, int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setColor(Color.RED);
        canvas.drawRect(x + 3, y + 3, x + 3 + (width - 6) * Math.max(health, 0) / 10f, y + height - 3, paint);
    }

    private void drawProgressBar(Canvas canvas, int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setColor(Color.rgb(0, 180, 220));
        float progress = Math.min(bodyX / levelLength, 1f);
        canvas.drawRect(x + 3, y + 3, x + 3 + (width - 6) * progress, y + height - 3, paint);
    }

    private void drawStartText(Canvas canvas) {
        long elapsed = System.currentTimeMillis() - levelStartTime;
        if (elapsed > 2200) return;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        paint.setTextSize(70);
        canvas.drawText("START", 45, 160, paint);
        paint.setTextSize(48);
        canvas.drawText("LEVEL " + level, 80, 215, paint);
    }

    private void drawBonusText(Canvas canvas, int w) {
        if (bonusText.isEmpty()) return;
        if (System.currentTimeMillis() - bonusTextTime > 1300) return;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(48);
        paint.setColor(Color.WHITE);
        canvas.drawText(bonusText, w / 2f, 150, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCenterMessage(Canvas canvas, int w, int h, String title, String sub) {
        paint.setColor(Color.argb(185, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(68);
        paint.setColor(Color.WHITE);
        canvas.drawText(title, w / 2f, h / 2f - 25, paint);

        paint.setTextSize(30);
        canvas.drawText(sub, w / 2f, h / 2f + 45, paint);

        paint.setTextSize(26);
        canvas.drawText("Skor: " + score + "  Yıldız: " + starsCollected, w / 2f, h / 2f + 90, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawMetalButton(Canvas canvas, RectF r, String text, int size) {
        paint.setColor(Color.rgb(30, 30, 30));
        canvas.drawRoundRect(r, 8, 8, paint);

        paint.setColor(Color.rgb(86, 86, 86));
        canvas.drawRect(r.left + 8, r.top + 8, r.right - 8, r.bottom - 8, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, r.centerX(), r.centerY() + size / 3f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStar(Canvas canvas, float x, float y, float r) {
        paint.setColor(Color.YELLOW);
        Path p = new Path();

        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 5 * i - Math.PI / 2;
            float rr = (i % 2 == 0) ? r : r / 2;
            float px = x + (float)Math.cos(a) * rr;
            float py = y + (float)Math.sin(a) * rr;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }

        p.close();
        canvas.drawPath(p, paint);
    }

    private void drawButtons(Canvas canvas, int w, int h) {
        leftBtn = new RectF(25, h - 140, 125, h - 35);
        rightBtn = new RectF(145, h - 140, 245, h - 35);
        jumpBtn = new RectF(265, h - 140, 365, h - 35);

        brakeBtn = new RectF(w - 320, h - 140, w - 210, h - 35);
        gasBtn = new RectF(w - 180, h - 140, w - 55, h - 35);

        paint.setColor(Color.argb(155, 0, 0, 0));
        canvas.drawRoundRect(leftBtn, 20, 20, paint);
        canvas.drawRoundRect(rightBtn, 20, 20, paint);
        canvas.drawRoundRect(jumpBtn, 20, 20, paint);
        canvas.drawRoundRect(brakeBtn, 20, 20, paint);
        canvas.drawRoundRect(gasBtn, 20, 20, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(32);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("←", 62, h - 76, paint);
        canvas.drawText("→", 182, h - 76, paint);
        canvas.drawText("Zıp", 292, h - 76, paint);
        canvas.drawText("Fren", w - 312, h - 76, paint);
        canvas.drawText("Gaz", w - 160, h - 76, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (gameState == STATE_GAME_OVER && action == MotionEvent.ACTION_DOWN) {
            gameState = STATE_MENU;
            return true;
        }

        if (gameState == STATE_LEVEL_COMPLETE && action == MotionEvent.ACTION_DOWN) {
            resetLevel(level + 1);
            gameState = STATE_PLAYING;
            return true;
        }

        if (gameState == STATE_HOW && action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            if (backBtn != null && backBtn.contains(x, y)) gameState = STATE_MENU;
            return true;
        }

        if (gameState == STATE_MENU && action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (playBtn != null && playBtn.contains(x, y)) {
                resetLevel(level);
                gameState = STATE_PLAYING;
                return true;
            }

            if (howBtn != null && howBtn.contains(x, y)) {
                gameState = STATE_HOW;
                return true;
            }

            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            gas = brake = left = right = jump = false;
            return true;
        }

        gas = brake = left = right = jump = false;

        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);

            if (gasBtn != null && gasBtn.contains(x, y)) gas = true;
            if (brakeBtn != null && brakeBtn.contains(x, y)) brake = true;
            if (leftBtn != null && leftBtn.contains(x, y)) left = true;
            if (rightBtn != null && rightBtn.contains(x, y)) right = true;
            if (jumpBtn != null && jumpBtn.contains(x, y)) jump = true;
        }

        return true;
    }

    private void addDust(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    x + random.nextInt(60) - 30,
                    y,
                    random.nextFloat() * 4f - 2f,
                    -random.nextFloat() * 3f,
                    26 + random.nextInt(20),
                    4 + random.nextFloat() * 5,
                    Color.rgb(120, 90, 60)
            ));
        }
    }

    private void addSparks(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    x,
                    y,
                    random.nextFloat() * 10f - 5f,
                    random.nextFloat() * -7f,
                    20 + random.nextInt(16),
                    3 + random.nextFloat() * 3,
                    Color.YELLOW
            ));
        }
    }

    private void addStarParticles(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    x,
                    y,
                    random.nextFloat() * 6f - 3f,
                    random.nextFloat() * -5f,
                    18 + random.nextInt(12),
                    3 + random.nextFloat() * 3,
                    Color.YELLOW
            ));
        }
    }

    private void sleep() {
        try {
            Thread.sleep(16);
        } catch (InterruptedException ignored) {}
    }

    public void pause() {
        running = false;
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException ignored) {}
    }

    public void resume() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    static class Wheel {
        float x, y, vx, vy, rotation;

        Wheel(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Item {
        float x, y;
        int type;

        Item(float x, float y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    static class Obstacle {
        float x, y, size;

        Obstacle(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    static class Particle {
        float x, y, vx, vy, size;
        int life;
        int color;

        Particle(float x, float y, float vx, float vy, int life, float size, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.size = size;
            this.color = color;
        }
    }

    static class Checkpoint {
        float x;
        boolean finish;
        boolean passed;

        Checkpoint(float x, boolean finish) {
            this.x = x;
            this.finish = finish;
            this.passed = false;
        }
    }
}
