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

    private static final int ITEM_STAR = 1;
    private static final int ITEM_REPAIR = 2;
    private static final int ITEM_BIG = 3;
    private static final int ITEM_SMALL = 4;
    private static final int ITEM_SLOW = 5;

    private int gameState = STATE_MENU;
    private int level = 1;

    private float carX, carY, carVX, carVY, carAngle, angleVelocity;
    private boolean gas, brake, left, right, jump;
    private boolean onGround = false;

    private int score = 0;
    private int health = 10;
    private int starsCollected = 0;

    private long levelStartTime;
    private int levelTimeSeconds;
    private float levelLength;

    private float airSpin = 0;
    private long bonusTextTime = 0;
    private String bonusText = "";

    private long bigUntil = 0;
    private long smallUntil = 0;
    private long slowUntil = 0;
    private long lastDamageTime = 0;

    private final List<Item> items = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private RectF playBtn, howBtn, backBtn;
    private RectF gasBtn, brakeBtn, leftBtn, rightBtn, jumpBtn;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        resetLevel(1);
    }

    private void resetLevel(int newLevel) {
        level = newLevel;
        levelLength = 7600 + level * 1400;
        levelTimeSeconds = Math.max(120, 240 - level * 10);

        carX = 250;
        carY = 300;
        carVX = 0;
        carVY = 0;
        carAngle = 0;
        angleVelocity = 0;

        gas = brake = left = right = jump = false;
        onGround = false;
        airSpin = 0;

        score = 0;
        health = 10;
        starsCollected = 0;

        bigUntil = 0;
        smallUntil = 0;
        slowUntil = 0;
        lastDamageTime = 0;
        bonusText = "";

        items.clear();
        obstacles.clear();
        particles.clear();

        generateLevel();

        levelStartTime = System.currentTimeMillis();
    }

    private void generateLevel() {
        random.setSeed(1000L + level * 77L);

        for (int i = 0; i < 38 + level * 4; i++) {
            float x = 550 + i * 230 + random.nextInt(90);
            float y = getGroundY(x) - 130 - random.nextInt(110);
            items.add(new Item(x, y, ITEM_STAR));
        }

        for (int i = 0; i < 4 + level; i++) {
            float x = 1200 + i * 1100 + random.nextInt(250);
            items.add(new Item(x, getGroundY(x) - 105, ITEM_REPAIR));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 1700 + i * 1600 + random.nextInt(400);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_BIG));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 2300 + i * 1500 + random.nextInt(500);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_SMALL));
        }

        for (int i = 0; i < 2 + level / 2; i++) {
            float x = 3000 + i * 1700 + random.nextInt(500);
            items.add(new Item(x, getGroundY(x) - 115, ITEM_SLOW));
        }

        for (int i = 0; i < 7 + level * 2; i++) {
            float x = 1000 + i * 800 + random.nextInt(260);
            obstacles.add(new Obstacle(x, getGroundY(x) - 18));
        }
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
        if (gameState != STATE_PLAYING) {
            return;
        }

        long now = System.currentTimeMillis();

        if (getSecondsLeft() <= 0 || health <= 0) {
            gameState = STATE_GAME_OVER;
            return;
        }

        if (carX >= levelLength) {
            gameState = STATE_LEVEL_COMPLETE;
            showBonus("LEVEL COMPLETE");
            return;
        }

        float timeScale = now < slowUntil ? 0.70f : 1.0f;
        float oldVY = carVY;
        boolean wasOnGround = onGround;

        float scale = getCarScale();
        float enginePower = onGround ? 0.44f : 0.13f;
        float brakePower = onGround ? 0.36f : 0.10f;

        if (gas) carVX += enginePower * timeScale;
        if (brake) carVX -= brakePower * timeScale;

        if (left) angleVelocity -= (onGround ? 0.45f : 0.85f) * timeScale;
        if (right) angleVelocity += (onGround ? 0.45f : 0.85f) * timeScale;

        if (jump && onGround) {
            carVY = -12.5f;
            onGround = false;
            addDust(carX, carY + 35, 8);
        }

        carVX *= 0.986f;
        carVY += 0.62f * timeScale;

        carX += carVX * timeScale;
        carY += carVY * timeScale;

        carAngle += angleVelocity * timeScale;
        angleVelocity *= 0.94f;

        float groundY = getGroundY(carX);
        float targetY = groundY - 45 * scale;

        onGround = false;

        if (carY > targetY) {
            carY = targetY;
            onGround = true;

            float slope = getGroundAngle(carX);

            if (!wasOnGround) {
                handleLandingDamage(oldVY);
                addDust(carX, carY + 40, 14);
            }

            carVY = 0;
            carAngle += (slope - carAngle) * 0.075f;
            angleVelocity *= 0.52f;
            airSpin = 0;
        } else {
            handleFlipBonus();
        }

        collectItems();
        hitObstacles();
        updateParticles();
    }

    private void handleLandingDamage(float oldVY) {
        long now = System.currentTimeMillis();
        if (now - lastDamageTime < 600) return;

        int damage = 0;

        if (oldVY > 21) damage = 3;
        else if (oldVY > 16) damage = 2;
        else if (oldVY > 12) damage = 1;

        float normalized = Math.abs(normalizeAngle(carAngle));
        if (normalized > 120) damage += 2;
        else if (normalized > 85) damage += 1;

        if (damage > 0) {
            health -= damage;
            lastDamageTime = now;
            showBonus("-" + damage + " CAN");
            addSparks(carX, carY, 18);
        }
    }

    private void handleFlipBonus() {
        airSpin += angleVelocity;

        while (Math.abs(airSpin) >= 360f) {
            score += 100;
            showBonus("FLIP +100");
            airSpin += airSpin > 0 ? -360f : 360f;
        }
    }

    private void collectItems() {
        Iterator<Item> it = items.iterator();

        while (it.hasNext()) {
            Item item = it.next();

            float dx = item.x - carX;
            float dy = item.y - carY;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < 70 * getCarScale()) {
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
            addStarParticles(carX, carY, 10);
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
        }
    }

    private void hitObstacles() {
        long now = System.currentTimeMillis();

        for (Obstacle o : obstacles) {
            float dx = o.x - carX;
            float dy = o.y - carY;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < 58 * getCarScale() && now - lastDamageTime > 700) {
                health -= 1;
                lastDamageTime = now;
                carVX *= -0.35f;
                carVY = -9f;
                angleVelocity += carVX > 0 ? -6f : 6f;
                showBonus("-1 CAN");
                addSparks(o.x, o.y, 16);
            }
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();

        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.25f;
            p.life--;

            if (p.life <= 0) {
                it.remove();
            }
        }
    }

    private void showBonus(String text) {
        bonusText = text;
        bonusTextTime = System.currentTimeMillis();
    }

    private int getSecondsLeft() {
        int elapsed = (int)((System.currentTimeMillis() - levelStartTime) / 1000);
        return Math.max(levelTimeSeconds - elapsed, 0);
    }

    private float getCarScale() {
        long now = System.currentTimeMillis();
        if (now < bigUntil) return 1.25f;
        if (now < smallUntil) return 0.76f;
        return 1.0f;
    }

    private float getGroundY(float x) {
        return 520
                + (float)Math.sin(x * (0.0075f + level * 0.0004f)) * 72
                + (float)Math.sin(x * 0.020f) * 35
                + (float)Math.sin(x * 0.041f) * 14;
    }

    private float getGroundAngle(float x) {
        float y1 = getGroundY(x - 25);
        float y2 = getGroundY(x + 25);
        return (float)Math.toDegrees(Math.atan2(y2 - y1, 50));
    }

    private float normalizeAngle(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    private void drawGame() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
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
        paint.setColor(Color.rgb(48, 48, 48));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(96, 96, 96));
        canvas.drawRect(60, 40, w - 60, h - 40, paint);

        paint.setColor(Color.rgb(28, 28, 28));
        paint.setStrokeWidth(12);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(70, 50, w - 70, h - 50, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(70);
        paint.setColor(Color.BLACK);
        canvas.drawText("ARAZİ ARABASI", w / 2f, 145, paint);

        paint.setTextSize(38);
        paint.setColor(Color.YELLOW);
        canvas.drawText("YILDIZ AVI", w / 2f, 195, paint);

        drawMenuCar(canvas, w / 2f, h / 2f - 40);

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
        paint.setColor(Color.rgb(50, 50, 50));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.WHITE);
        paint.setTextSize(52);
        canvas.drawText("NASIL OYNANIR", w / 2f, 90, paint);

        paint.setTextSize(30);
        canvas.drawText("Gaz: hızlan", w / 2f, 170, paint);
        canvas.drawText("Fren: yavaşla / geri bas", w / 2f, 220, paint);
        canvas.drawText("Sol / Sağ: havada denge ve takla", w / 2f, 270, paint);
        canvas.drawText("Zıpla: rampadan çıkarken kullan", w / 2f, 320, paint);
        canvas.drawText("Yıldız topla, hasar alma, finish çizgisine ulaş.", w / 2f, 385, paint);
        canvas.drawText("Takla atarsan FLIP +100 puan.", w / 2f, 435, paint);

        backBtn = new RectF(w / 2f - 170, h - 125, w / 2f + 170, h - 60);
        drawMetalButton(canvas, backBtn, "GERİ", 34);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPlaying(Canvas canvas, int w, int h) {
        float camX = carX - w * 0.35f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(225, 150, 55));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(245, 185, 70));
        canvas.drawCircle(w - 230, 180, 95, paint);

        paint.setColor(Color.rgb(150, 95, 38));
        Path farHill = new Path();
        farHill.moveTo(0, 390);
        for (int x = 0; x <= w; x += 40) {
            farHill.lineTo(x, 390 + (float)Math.sin((x + camX * 0.15f) * 0.007f) * 50);
        }
        farHill.lineTo(w, h);
        farHill.lineTo(0, h);
        farHill.close();
        canvas.drawPath(farHill, paint);

        drawFinishAndCheckpoints(canvas, w, h, camX);
        drawItems(canvas, camX, w);
        drawObstacles(canvas, camX, w);

        drawGround(canvas, w, h, camX);
        drawParticles(canvas, camX);

        drawCar(canvas, carX - camX, carY);
        drawHud(canvas, w);
        drawStartText(canvas);
        drawBonusText(canvas, w);
        drawButtons(canvas, w, h);
    }

    private void drawGround(Canvas canvas, int w, int h, float camX) {
        paint.setColor(Color.rgb(70, 45, 20));

        Path ground = new Path();
        ground.moveTo(0, h);

        for (int sx = 0; sx <= w + 20; sx += 20) {
            float worldX = camX + sx;
            ground.lineTo(sx, getGroundY(worldX));
        }

        ground.lineTo(w, h);
        ground.close();
        canvas.drawPath(ground, paint);

        paint.setColor(Color.rgb(95, 65, 30));
        for (int sx = 0; sx <= w; sx += 80) {
            float worldX = camX + sx;
            float gy = getGroundY(worldX);
            canvas.drawCircle(sx, gy + 35, 10, paint);
        }
    }

    private void drawFinishAndCheckpoints(Canvas canvas, int w, int h, float camX) {
        float[] marks = {levelLength / 3f, levelLength * 2f / 3f, levelLength};

        for (int i = 0; i < marks.length; i++) {
            float sx = marks[i] - camX;
            if (sx < -80 || sx > w + 80) continue;

            float gy = getGroundY(marks[i]);

            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            canvas.drawLine(sx, gy - 150, sx, gy, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == 2 ? Color.RED : Color.CYAN);
            canvas.drawRect(sx, gy - 150, sx + 45, gy - 115, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawItems(Canvas canvas, float camX, int w) {
        for (Item item : items) {
            float sx = item.x - camX;
            if (sx < -80 || sx > w + 80) continue;

            if (item.type == ITEM_STAR) {
                drawStar(canvas, sx, item.y, 22);
            } else if (item.type == ITEM_REPAIR) {
                drawCircleItem(canvas, sx, item.y, Color.GREEN, "+");
            } else if (item.type == ITEM_BIG) {
                drawCircleItem(canvas, sx, item.y, Color.MAGENTA, "B");
            } else if (item.type == ITEM_SMALL) {
                drawCircleItem(canvas, sx, item.y, Color.CYAN, "S");
            } else if (item.type == ITEM_SLOW) {
                drawCircleItem(canvas, sx, item.y, Color.BLUE, "T");
            }
        }
    }

    private void drawCircleItem(Canvas canvas, float x, float y, int color, String text) {
        paint.setColor(color);
        canvas.drawCircle(x, y, 24, paint);

        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(28);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y + 10, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawObstacles(Canvas canvas, float camX, int w) {
        paint.setColor(Color.rgb(40, 40, 40));

        for (Obstacle o : obstacles) {
            float sx = o.x - camX;
            if (sx < -80 || sx > w + 80) continue;

            Path p = new Path();
            p.moveTo(sx - 28, o.y + 22);
            p.lineTo(sx, o.y - 28);
            p.lineTo(sx + 28, o.y + 22);
            p.close();
            canvas.drawPath(p, paint);
        }
    }

    private void drawParticles(Canvas canvas, float camX) {
        for (Particle p : particles) {
            paint.setColor(p.color);
            canvas.drawCircle(p.x - camX, p.y, p.size, paint);
        }
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
        float progress = Math.min(carX / levelLength, 1f);
        canvas.drawRect(x + 3, y + 3, x + 3 + (width - 6) * progress, y + height - 3, paint);
    }

    private String getTimeText() {
        int left = getSecondsLeft();
        int m = left / 60;
        int s = left % 60;
        return String.format("%02d:%02d", m, s);
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

    private void drawMenuCar(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.scale(1.2f, 1.2f, x, y);
        drawCarShape(canvas, x, y, 0, 1.0f);
        canvas.restore();
    }

    private void drawMetalButton(Canvas canvas, RectF r, String text, int size) {
        paint.setColor(Color.rgb(30, 30, 30));
        canvas.drawRoundRect(r, 6, 6, paint);

        paint.setColor(Color.rgb(85, 85, 85));
        canvas.drawRect(r.left + 8, r.top + 8, r.right - 8, r.bottom - 8, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, r.centerX(), r.centerY() + size / 3f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCar(Canvas canvas, float x, float y) {
        drawCarShape(canvas, x, y, carAngle, getCarScale());
    }

    private void drawCarShape(Canvas canvas, float x, float y, float angle, float scale) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle);
        canvas.scale(scale, scale);

        paint.setColor(Color.rgb(215, 215, 190));
        canvas.drawRoundRect(new RectF(-78, -38, 78, 20), 18, 18, paint);

        paint.setColor(Color.rgb(130, 130, 115));
        canvas.drawRect(-38, -62, 32, -35, paint);

        paint.setColor(Color.rgb(80, 80, 65));
        canvas.drawRect(-60, -50, -20, -38, paint);
        canvas.drawRect(15, -50, 55, -38, paint);

        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(-50, 28, 27, paint);
        canvas.drawCircle(50, 28, 27, paint);

        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(-50, 28, 11, paint);
        canvas.drawCircle(50, 28, 11, paint);

        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(-30, -78);
        flag.lineTo(28, -63);
        flag.lineTo(-30, -48);
        flag.close();
        canvas.drawPath(flag, paint);

        canvas.restore();
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
            if (backBtn != null && backBtn.contains(x, y)) {
                gameState = STATE_MENU;
            }
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
        running = true;
        thread = new Thread(this);
        thread.start();
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
        float x, y;

        Obstacle(float x, float y) {
            this.x = x;
            this.y = y;
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
}
