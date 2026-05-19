package com.araziarabasi.yildizavi;

import android.content.Context;
import android.graphics.*;
import android.view.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

    private int gameState = STATE_MENU;
    private int levelNo = 1;

    private DuneLevel level;
    private CParticleEngine engine;
    private CCarSynchronizer car;

    private boolean gas, brake, left, right, jump;

    private int score = 0;
    private int starsCollected = 0;
    private int checkpointIndex = 0;

    private float camX = 0;
    private float camTargetX = 0;

    private long levelStartTime;
    private long lastDamageTime = 0;
    private long bonusTextTime = 0;
    private String bonusText = "";

    private long bigUntil = 0;
    private long smallUntil = 0;
    private long slowUntil = 0;

    private RectF playBtn, howBtn, backBtn;
    private RectF gasBtn, brakeBtn, leftBtn, rightBtn, jumpBtn;

    private final List<DuneParticle> particles = new ArrayList<>();

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        resetLevel(1);
    }

    private void resetLevel(int newLevel) {
        levelNo = Math.max(1, newLevel);

        if (levelNo > DuneGroundData.maxLevel()) {
            levelNo = 1;
        }

        level = new DuneLevel(levelNo);
        engine = new CParticleEngine();
        car = new CCarSynchronizer();

        float startX = level.startX;
        float startY = level.getGroundY(startX) - 95f;
        car.init(startX, startY, engine);

        score = 0;
        starsCollected = 0;
        checkpointIndex = 0;

        gas = brake = left = right = jump = false;

        camX = Math.max(0, startX - 250f);
        camTargetX = camX;

        bigUntil = 0;
        smallUntil = 0;
        slowUntil = 0;

        bonusText = "";
        particles.clear();

        levelStartTime = System.currentTimeMillis();
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

        if (now > bigUntil && now > smallUntil) {
            car.makeNormalCar();
        }

        engine.timeMultiplier = now < slowUntil ? 0.68f : 1f;
        engine.process(level, gas, brake, jump);
        car.process(left, right);

        if (car.consumeFlipBonus()) {
            score += 100;
            showBonus("FLIP +100");
        }

        if (getSecondsLeft() <= 0 || car.health <= 0) {
            gameState = STATE_GAME_OVER;
            return;
        }

        collectBonuses();
        hitObstacles();
        updateCheckpointAndFinish();
        updateParticles();

        if (car.y > getHeight() + 450) {
            damageCar(40, "DÜŞTÜN");
            respawnAtCheckpoint();
        }

        camTargetX = car.x - getWidth() * 0.35f;
        if (camTargetX < 0) camTargetX = 0;
        camX += (camTargetX - camX) * 0.10f;
    }

    private void updateCheckpointAndFinish() {
        for (int i = 0; i < level.checkpoints.size(); i++) {
            DuneLevel.Checkpoint c = level.checkpoints.get(i);

            if (!c.passed && car.x >= c.x) {
                c.passed = true;
                checkpointIndex = i;

                if (c.finish) {
                    score += getSecondsLeft() * 3;
                    showBonus("LEVEL COMPLETE");
                    gameState = STATE_LEVEL_COMPLETE;
                } else {
                    score += 50;
                    showBonus("CHECKPOINT");
                }
            }
        }
    }

    private void collectBonuses() {
        for (BonusItem b : level.bonuses) {
            if (b.taken) continue;

            float dx = b.x - car.x;
            float dy = b.y - car.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);

            if (d < 85 * car.scaleFactor) {
                b.taken = true;
                applyBonus(b.type);
            }
        }
    }

    private void applyBonus(int type) {
        long now = System.currentTimeMillis();

        if (type == BonusItem.STAR) {
            score += 10;
            starsCollected++;
            showBonus("+10");
            addStarParticles(car.x, car.y, 10);
        } else if (type == BonusItem.REPAIR) {
            car.repairCar(30);
            showBonus("+30 CAN");
        } else if (type == BonusItem.BIG) {
            car.makeBigCar();
            bigUntil = now + 9000;
            smallUntil = 0;
            showBonus("BÜYÜK ARABA");
        } else if (type == BonusItem.SMALL) {
            car.makeSmallCar();
            smallUntil = now + 9000;
            bigUntil = 0;
            showBonus("KÜÇÜK ARABA");
        } else if (type == BonusItem.SLOW) {
            slowUntil = now + 7000;
            showBonus("YAVAŞ ZAMAN");
        } else if (type == BonusItem.MINUS) {
            score = Math.max(0, score - 25);
            showBonus("-25");
            addSparks(car.x, car.y, 14);
        }
    }

    private void hitObstacles() {
        long now = System.currentTimeMillis();

        for (DuneLevel.Obstacle o : level.obstacles) {
            float dx = o.x - car.x;
            float dy = o.y - car.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);

            if (d < (o.size + 55) * car.scaleFactor && now - lastDamageTime > 700) {
                damageCar(10, "-10 CAN");

                float kick = car.x < o.x ? -1 : 1;

                car.pWl.addVelocity(kick * -7f, -8f);
                car.pWr.addVelocity(kick * -7f, -8f);
                car.pLd.addVelocity(kick * -4f, -5f);
                car.pRd.addVelocity(kick * -4f, -5f);

                addSparks(o.x, o.y, 18);
            }
        }
    }

    private void damageCar(int damage, String text) {
        car.damageCar(damage);
        lastDamageTime = System.currentTimeMillis();
        showBonus(text);
    }

    private void respawnAtCheckpoint() {
        float x = level.startX;

        if (checkpointIndex >= 0 && checkpointIndex < level.checkpoints.size()) {
            x = Math.max(level.startX, level.checkpoints.get(checkpointIndex).x - 220f);
        }

        car.init(x, level.getGroundY(x) - 95f, engine);
    }

    private void updateParticles() {
        Iterator<DuneParticle> it = particles.iterator();

        while (it.hasNext()) {
            if (!it.next().update()) {
                it.remove();
            }
        }
    }

    private int getSecondsLeft() {
        int elapsed = (int) ((System.currentTimeMillis() - levelStartTime) / 1000);
        return Math.max(level.timeSeconds - elapsed, 0);
    }

    private String getTimeText() {
        int left = getSecondsLeft();
        return String.format("%02d:%02d", left / 60, left % 60);
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

        if (car != null) {
            car.draw(canvas, paint, car.x - w / 2f);
        }

        playBtn = new RectF(w / 2f - 190, h / 2f + 70, w / 2f + 190, h / 2f + 142);
        howBtn = new RectF(w / 2f - 190, h / 2f + 160, w / 2f + 190, h / 2f + 225);

        drawMetalButton(canvas, playBtn, "PLAY ▶", 40);
        drawMetalButton(canvas, howBtn, "NASIL OYNANIR", 28);

        paint.setTextSize(23);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Bölüm " + levelNo, w / 2f, h - 75, paint);
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

        backBtn = new RectF(w / 2f - 170, h - 125, w / 2f + 170, h - 60);
        drawMetalButton(canvas, backBtn, "GERİ", 34);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPlaying(Canvas canvas, int w, int h) {
        level.drawBackground(canvas, paint, camX);
        level.drawObjects(canvas, paint, camX);
        level.drawGround(canvas, paint, camX);

        for (DuneParticle p : particles) {
            p.draw(canvas, paint, camX);
        }

        car.draw(canvas, paint, camX);

        drawHud(canvas, w);
        drawStartText(canvas);
        drawBonusText(canvas, w);
        drawButtons(canvas, w, h);
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
        paint.setStyle(Paint.Style.FILL);
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
        canvas.drawRect(
                x + 3,
                y + 3,
                x + 3 + (width - 6) * Math.max(car.health, 0) / 100f,
                y + height - 3,
                paint
        );
    }

    private void drawProgressBar(Canvas canvas, int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setColor(Color.rgb(0, 180, 220));

        float progress = (car.x - level.startX) / level.length;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        canvas.drawRect(
                x + 3,
                y + 3,
                x + 3 + (width - 6) * progress,
                y + height - 3,
                paint
        );
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
        canvas.drawText("LEVEL " + levelNo, 80, 215, paint);
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
        paint.setStyle(Paint.Style.FILL);
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

    private void drawButtons(Canvas canvas, int w, int h) {
        leftBtn = new RectF(25, h - 140, 125, h - 35);
        rightBtn = new RectF(145, h - 140, 245, h - 35);
        jumpBtn = new RectF(265, h - 140, 365, h - 35);

        brakeBtn = new RectF(w - 320, h - 140, w - 210, h - 35);
        gasBtn = new RectF(w - 180, h - 140, w - 55, h - 35);

        paint.setStyle(Paint.Style.FILL);
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
            resetLevel(levelNo + 1);
            gameState = STATE_PLAYING;
            return true;
        }

        if (gameState == STATE_HOW && action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (backBtn != null && backBtn.contains(x, y)) {
                gameState = STATE_MENU;
            }

            return true;
        }

        if (gameState == STATE_MENU && action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (playBtn != null && playBtn.contains(x, y)) {
                resetLevel(levelNo);
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

    private void addSparks(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new DuneParticle(
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
            particles.add(new DuneParticle(
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
}
