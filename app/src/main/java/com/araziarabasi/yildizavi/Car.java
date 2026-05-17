package com.araziarabasi.yildizavi;

import android.graphics.*;

public class Car {
    public Wheel backWheel;
    public Wheel frontWheel;

    public float x;
    public float y;
    public float angle;
    public float angleVelocity;
    public float airSpin;

    public long bigUntil = 0;
    public long smallUntil = 0;
    public long slowUntil = 0;
    public long wheelUntil = 0;

    public boolean wasOnGround = false;

    public Car(float startX, Level level) {
        reset(startX, level);
    }

    public void reset(float startX, Level level) {
        float gy = level.getGroundY(startX);
        backWheel = new Wheel(startX - 55, gy - 45);
        frontWheel = new Wheel(startX + 55, gy - 45);
        x = startX;
        y = gy - 95;
        angle = 0;
        angleVelocity = 0;
        airSpin = 0;
        wasOnGround = false;
    }

    public float scale() {
        long now = System.currentTimeMillis();
        if (now < bigUntil) return 1.25f;
        if (now < smallUntil) return 0.76f;
        return 1.0f;
    }

    public float wheelRadius() {
        long now = System.currentTimeMillis();
        if (now < wheelUntil) return 38f * scale();
        return 28f * scale();
    }

    public boolean update(Level level, boolean gas, boolean brake, boolean left, boolean right, boolean jump) {
        long now = System.currentTimeMillis();
        float dt = now < slowUntil ? 0.68f : 1.0f;
        float sc = scale();

        backWheel.radius = wheelRadius();
        frontWheel.radius = wheelRadius();

        backWheel.solveGround(level);
        frontWheel.solveGround(level);

        boolean onGround = backWheel.onGround || frontWheel.onGround;

        if (gas) {
            backWheel.vel.x += 0.58f * dt;
            frontWheel.vel.x += 0.20f * dt;
        }

        if (brake) {
            backWheel.vel.x -= 0.42f * dt;
            frontWheel.vel.x -= 0.15f * dt;
        }

        if (jump && onGround) {
            backWheel.vel.y -= 8.5f;
            frontWheel.vel.y -= 8.5f;
        }

        if (left) angleVelocity -= (onGround ? 0.38f : 0.82f) * dt;
        if (right) angleVelocity += (onGround ? 0.38f : 0.82f) * dt;

        backWheel.update(dt);
        frontWheel.update(dt);

        backWheel.solveGround(level);
        frontWheel.solveGround(level);

        constrain(sc);
        updateBody(sc, dt, onGround);

        boolean landed = onGround && !wasOnGround;
        wasOnGround = onGround;

        if (!onGround) {
            airSpin += angleVelocity;
        } else {
            airSpin = 0;
        }

        return landed;
    }

    private void constrain(float sc) {
        float targetDist = 112f * sc;
        float dx = frontWheel.pos.x - backWheel.pos.x;
        float dy = frontWheel.pos.y - backWheel.pos.y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001f) return;

        float diff = (dist - targetDist) / dist;
        float pushX = dx * diff * 0.5f;
        float pushY = dy * diff * 0.5f;

        backWheel.pos.x += pushX;
        backWheel.pos.y += pushY;
        frontWheel.pos.x -= pushX;
        frontWheel.pos.y -= pushY;
    }

    private void updateBody(float sc, float dt, boolean onGround) {
        float dx = frontWheel.pos.x - backWheel.pos.x;
        float dy = frontWheel.pos.y - backWheel.pos.y;
        float wheelAngle = (float)Math.toDegrees(Math.atan2(dy, dx));

        float targetAngle = wheelAngle + angleVelocity;
        angle += normalizeAngle(targetAngle - angle) * (onGround ? 0.19f : 0.08f);
        angle += angleVelocity * 0.18f * dt;
        angleVelocity *= onGround ? 0.90f : 0.97f;

        float midX = (backWheel.pos.x + frontWheel.pos.x) / 2f;
        float midY = (backWheel.pos.y + frontWheel.pos.y) / 2f;

        x = midX;
        y += ((midY - 58f * sc) - y) * 0.26f;
    }

    private float normalizeAngle(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    public void draw(Canvas canvas, Paint paint, float camX) {
        drawShape(canvas, paint, x - camX, y, angle, scale());
    }

    private void drawShape(Canvas canvas, Paint paint, float sx, float sy, float rot, float sc) {
        canvas.save();
        canvas.translate(sx, sy);
        canvas.rotate(rot);
        canvas.scale(sc, sc);

        float wr = wheelRadius() / sc;

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

        drawWheel(canvas, paint, -55, 30, wr);
        drawWheel(canvas, paint, 55, 30, wr);

        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(-30, -84);
        flag.lineTo(30, -68);
        flag.lineTo(-30, -52);
        flag.close();
        canvas.drawPath(flag, paint);

        canvas.restore();
    }

    private void drawWheel(Canvas canvas, Paint paint, float x, float y, float r) {
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
}

