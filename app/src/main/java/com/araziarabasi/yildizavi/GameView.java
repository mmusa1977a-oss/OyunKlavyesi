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

    private float carX = 250;
    private float carY = 300;
    private float carVX = 0;
    private float carVY = 0;
    private float carAngle = 0;

    private boolean gas = false;
    private boolean brake = false;
    private boolean left = false;
    private boolean right = false;

    private int score = 0;
    private final List<Star> stars = new ArrayList<>();

    private RectF gasBtn, brakeBtn, leftBtn, rightBtn;

    public GameView(Context context) {
        super(context);
        setFocusable(true);

        for (int i = 0; i < 25; i++) {
            stars.add(new Star(600 + i * 350, 180 + random.nextInt(180)));
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

        float camX = carX - w * 0.35f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(120, 200, 255));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.rgb(110, 75, 35));
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
            if (sx > -50 && sx < w + 50) {
                drawStar(canvas, sx, s.y, 22);
            }
        }

        drawCar(canvas, carX - camX, carY);

        paint.setColor(Color.BLACK);
        paint.setTextSize(42);
        canvas.drawText("Skor: " + score, 30, 55, paint);

        drawButtons(canvas, w, h);

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawCar(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.rotate(carAngle, x, y);

        paint.setColor(Color.RED);
        RectF body = new RectF(x - 70, y - 35, x + 70, y + 20);
        canvas.drawRoundRect(body, 18, 18, paint);

        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(x - 45, y + 25, 24, paint);
        canvas.drawCircle(x + 45, y + 25, 24, paint);

        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(x - 45, y + 25, 10, paint);
        canvas.drawCircle(x + 45, y + 25, 10, paint);

        canvas.restore();
    }

    private void drawStar(Canvas canvas, float x, float y, float r) {
        paint.setColor(Color.YELLOW);
        Path p = new Path();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 5 * i - Math.PI / 2;
            float rr = (i % 2 == 0) ? r : r / 2;
            float px = x + (float)Math.cos(angle) * rr;
            float py = y + (float)Math.sin(angle) * rr;
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
        canvas.drawText("←", 70, h - 72, paint);
        canvas.drawText("→", 200, h - 72, paint);
        canvas.drawText("Fren", w - 292, h - 72, paint);
        canvas.drawText("Gaz", w - 145, h - 72, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
