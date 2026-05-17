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
    private static final int STATE_PLAYING = 1;
    private int gameState = STATE_MENU;

    private float carX = 250, carY = 300, carVX = 0, carVY = 0, carAngle = 0;
    private boolean gas = false, brake = false, left = false, right = false;

    private int score = 0;
    private int health = 10;
    private long levelStartTime = 0;
    private final int levelTimeSeconds = 240;

    private final List<Star> stars = new ArrayList<>();

    private RectF playBtn, howBtn;
    private RectF gasBtn, brakeBtn, leftBtn, rightBtn;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        resetLevel();
    }

    private void resetLevel() {
        carX = 250;
        carY = 300;
        carVX = 0;
        carVY = 0;
        carAngle = 0;
        score = 0;
        health = 10;
        stars.clear();

        for (int i = 0; i < 35; i++) {
            stars.add(new Star(650 + i * 290, 140 + random.nextInt(210)));
        }

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

        if (gas) carVX += 0.35f;
        if (brake) carVX -= 0.28f;
        if (left) carAngle -= 3f;
        if (right) carAngle += 3f;

        carVX *= 0.985f;
        carVY += 0.65f;

        carX += carVX;
        carY += carVY;

        float groundY = getGroundY(carX);
        if (carY > groundY - 45) {
            carY = groundY - 45;
            carVY = 0;
            carAngle *= 0.94f;
        }

        Iterator<Star> it = stars.iterator();
        while (it.hasNext()) {
            Star s = it.next();
            float dx = s.x - carX;
            float dy = s.y - carY;
            if (Math.sqrt(dx * dx + dy * dy) < 70) {
                score++;
                it.remove();
            }
        }
    }

    private float getGroundY(float x) {
        return 520 + (float)Math.sin(x * 0.008f) * 70 + (float)Math.sin(x * 0.021f) * 35;
    }

    private void drawGame() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        if (gameState == STATE_MENU) {
            drawMenu(canvas, w, h);
        } else {
            drawPlaying(canvas, w, h);
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawMenu(Canvas canvas, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(55, 55, 55));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(90, 90, 90));
        canvas.drawRect(60, 40, w - 60, h - 40, paint);

        paint.setColor(Color.rgb(35, 35, 35));
        paint.setStrokeWidth(12);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(70, 50, w - 70, h - 50, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(74);
        canvas.drawText("ARAZİ", w / 2f, 150, paint);
        canvas.drawText("ARABASI", w / 2f, 225, paint);

        drawMenuCar(canvas, w / 2f, h / 2f - 40);

        playBtn = new RectF(w / 2f - 180, h / 2f + 65, w / 2f + 180, h / 2f + 135);
        howBtn = new RectF(w / 2f - 180, h / 2f + 155, w / 2f + 180, h / 2f + 215);

        drawMetalButton(canvas, playBtn, "PLAY ▶", 38);
        drawMetalButton(canvas, howBtn, "NASIL OYNANIR", 28);

        paint.setTextSize(24);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("YILDIZ AVI", w / 2f, h - 85, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPlaying(Canvas canvas, int w, int h) {
        float camX = carX - w * 0.35f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(225, 150, 55));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(190, 120, 40));
        canvas.drawCircle(w - 260, 260, 210, paint);

        paint.setColor(Color.rgb(130, 90, 35));
        Path bgHill = new Path();
        bgHill.moveTo(0, 420);
        for (int x = 0; x <= w; x += 40) {
            bgHill.lineTo(x, 420 + (float)Math.sin(x * 0.009f) * 40);
        }
        bgHill.lineTo(w, h);
        bgHill.lineTo(0, h);
        bgHill.close();
        canvas.drawPath(bgHill, paint);

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

        for (Star s : stars) {
            float sx = s.x - camX;
            if (sx > -50 && sx < w + 50) drawStar(canvas, sx, s.y, 22);
        }

        drawCar(canvas, carX - camX, carY);
        drawHud(canvas, w);
        drawStartText(canvas, w);
        drawButtons(canvas, w, h);
    }

    private void drawHud(Canvas canvas, int w) {
        int y = 18;
        drawHudBox(canvas, 15, y, 210, 50, "TIME", getTimeText());
        drawHudBox(canvas, 240, y, 300, 50, "HEALTH", "");
        drawHealthBar(canvas, 355, y + 15, 150, 20);
        drawHudBox(canvas, 555, y, 150, 50, "★", String.valueOf(score));
        drawHudBox(canvas, w - 320, y, 300, 50, "PROGRESS", "");
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
            canvas.drawRect(x + width - 80, y + 8, x + width - 12, y + height - 8, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(value, x + width - 70, y + 34, paint);
        }
    }

    private void drawHealthBar(Canvas canvas, int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setColor(Color.RED);
        canvas.drawRect(x + 3, y + 3, x + 3 + (width - 6) * health / 10f, y + height - 3, paint);
    }

    private void drawProgressBar(Canvas canvas, int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setColor(Color.rgb(0, 180, 220));
        float progress = Math.min(carX / 9000f, 1f);
        canvas.drawRect(x + 3, y + 3, x + 3 + (width - 6) * progress, y + height - 3, paint);
    }

    private String getTimeText() {
        int elapsed = (int)((System.currentTimeMillis() - levelStartTime) / 1000);
        int left = Math.max(levelTimeSeconds - elapsed, 0);
        int m = left / 60;
        int s = left % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void drawStartText(Canvas canvas, int w) {
        long elapsed = System.currentTimeMillis() - levelStartTime;
        if (elapsed > 2200) return;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.WHITE);
        paint.setTextSize(78);
        canvas.drawText("START", 45, 170, paint);

        paint.setTextSize(55);
        canvas.drawText("LEVEL 1", 80, 230, paint);
    }

    private void drawMenuCar(Canvas canvas, float x, float y) {
        paint.setColor(Color.rgb(210, 210, 190));
        canvas.drawRoundRect(new RectF(x - 110, y - 45, x + 110, y + 35), 22, 22, paint);
        paint.setColor(Color.rgb(80, 80, 80));
        canvas.drawCircle(x - 70, y + 45, 38, paint);
        canvas.drawCircle(x + 70, y + 45, 38, paint);
        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(x - 70, y + 45, 17, paint);
        canvas.drawCircle(x + 70, y + 45, 17, paint);
        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(x - 30, y - 80);
        flag.lineTo(x + 35, y - 65);
        flag.lineTo(x - 30, y - 45);
        flag.close();
        canvas.drawPath(flag, paint);
    }

    private void drawMetalButton(Canvas canvas, RectF r, String text, int size) {
        paint.setColor(Color.rgb(30, 30, 30));
        canvas.drawRoundRect(r, 4, 4, paint);
        paint.setColor(Color.rgb(80, 80, 80));
        canvas.drawRect(r.left + 8, r.top + 8, r.right - 8, r.bottom - 8, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, r.centerX(), r.centerY() + size / 3f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCar(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.rotate(carAngle, x, y);

        paint.setColor(Color.rgb(215, 215, 190));
        canvas.drawRoundRect(new RectF(x - 75, y - 38, x + 75, y + 20), 18, 18, paint);

        paint.setColor(Color.rgb(130, 130, 115));
        canvas.drawRect(x - 35, y - 60, x + 30, y - 35, paint);

        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(x - 48, y + 27, 26, paint);
        canvas.drawCircle(x + 48, y + 27, 26, paint);

        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(x - 48, y + 27, 11, paint);
        canvas.drawCircle(x + 48, y + 27, 11, paint);

        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(x - 30, y - 75);
        flag.lineTo(x + 25, y - 62);
        flag.lineTo(x - 30, y - 48);
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
        leftBtn = new RectF(30, h - 140, 140, h - 30);
        rightBtn = new RectF(160, h - 140, 270, h - 30);
        brakeBtn = new RectF(w - 300, h - 140, w - 190, h - 30);
        gasBtn = new RectF(w - 160, h - 140, w - 50, h - 30);

        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRoundRect(leftBtn, 20, 20, paint);
        canvas.drawRoundRect(rightBtn, 20, 20, paint);
        canvas.drawRoundRect(brakeBtn, 20, 20, paint);
        canvas.drawRoundRect(gasBtn, 20, 20, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("←", 70, h - 72, paint);
        canvas.drawText("→", 200, h - 72, paint);
        canvas.drawText("Fren", w - 292, h - 72, paint);
        canvas.drawText("Gaz", w - 145, h - 72, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameState == STATE_MENU && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (playBtn != null && playBtn.contains(x, y)) {
                resetLevel();
                gameState = STATE_PLAYING;
                return true;
            }
            return true;
        }

        gas = brake = left = right = false;

        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);

            if (gasBtn != null && gasBtn.contains(x, y)) gas = true;
            if (brakeBtn != null && brakeBtn.contains(x, y)) brake = true;
            if (leftBtn != null && leftBtn.contains(x, y)) left = true;
            if (rightBtn != null && rightBtn.contains(x, y)) right = true;
        }

        return true;
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

    static class Star {
        float x, y;
        Star(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
