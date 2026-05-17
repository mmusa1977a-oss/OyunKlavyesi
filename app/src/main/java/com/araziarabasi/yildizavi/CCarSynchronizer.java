package com.araziarabasi.yildizavi;

import android.graphics.*;

public class CCarSynchronizer {
    public static final int MAX_HEALTH = 100;
    public static final int ST_GO = 1;
    public static final int ST_CRUSH = 2;

    public float koeffSpring = 0.5f;
    public CVector2D carOldPos = new CVector2D(0, 0);
    public int downDamage = 1;
    public int upDamage = 8;

    public CWheel mcBackWheel = new CWheel();
    public CWheel mcForwardWheel = new CWheel();

    public CParticle pWl;
    public CParticle pWr;
    public CParticle pLd;
    public CParticle pRd;

    public int state = ST_GO;
    public float prevFrameAngle = 0;
    public float totalFlipAngle = 0;

    public float x;
    public float y;
    public float rotation;
    public float scaleTarget = 100f;
    public float scaleFactor = 1f;
    public boolean wheelMode = false;

    public int health = MAX_HEALTH;

    public void init(float sX, float sY, CParticleEngine engine) {
        wheelMode = false;
        engine.clear();

        x = sX;
        y = sY;
        rotation = 0;
        carOldPos.reinit(x, y);

        pWl = new CParticle(sX - 55, sY + 30, 20);
        pWr = new CParticle(sX + 55, sY + 30, 20);
        pLd = new CParticle(sX - 43, sY - 36, 3);
        pRd = new CParticle(sX + 43, sY - 36, 3);

        engine.addParticle(pWl);
        engine.addParticle(pWr);
        engine.addParticle(pLd);
        engine.addParticle(pRd);

        engine.addWheel(mcBackWheel, pWl, 28.5f, 27.5f);
        engine.addWheel(mcForwardWheel, pWr, 28.5f, 27.5f);

        engine.addSpringConstraint(pWr, pWl, 0.4f, 0.4f);
        engine.addSpringConstraint(pLd, pRd, 1f, 1f);
        engine.addSpringConstraint(pWr, pLd, 1f, 0.5f);
        engine.addSpringConstraint(pWl, pRd, 1f, 0.5f);
        engine.addSpringConstraint(pWr, pRd, 0.2f, 0.1f);
        engine.addSpringConstraint(pWl, pLd, 0.2f, 0.1f);

        engine.addPenetrationConstraint(pWr, pRd, pLd);
        engine.addPenetrationConstraint(pWl, pLd, pRd);

        state = ST_GO;
        prevFrameAngle = 0;
        totalFlipAngle = 0;
        scaleTarget = 100;
        health = MAX_HEALTH;
    }

    public void process(boolean left, boolean right) {
        if (wheelMode) {
            x = pWr.pos.x;
            y = pWr.pos.y;
            pWl.pos.reinit(pWr.pos.x, pWr.pos.y);
            return;
        }

        CVector2D body = new CVector2D(pRd.pos.x - pLd.pos.x, pRd.pos.y - pLd.pos.y);
        float ang = (float)Math.atan2(body.y, body.x);

        if (left) {
            pLd.addVelocity(0.8f, -0.8f);
            pRd.addVelocity(-0.8f, 0.8f);
        }

        if (right) {
            pLd.addVelocity(-0.8f, 0.8f);
            pRd.addVelocity(0.8f, -0.8f);
        }

        rotation = (float)Math.toDegrees(ang);
        x = (pLd.pos.x + pRd.pos.x + pWl.pos.x + pWr.pos.x) / 4f;
        y = (pLd.pos.y + pRd.pos.y + pWl.pos.y + pWr.pos.y) / 4f - 10f;

        updateFlip(ang);
    }

    private void updateFlip(float ang) {
        if (mcBackWheel.intersectionFlag || mcForwardWheel.intersectionFlag) {
            totalFlipAngle = 0;
        } else {
            float d = ang - prevFrameAngle;
            if (d > Math.PI) d -= (float)(Math.PI * 2);
            if (d < -Math.PI) d += (float)(Math.PI * 2);
            totalFlipAngle += d;
        }

        prevFrameAngle = ang;
    }

    public boolean consumeFlipBonus() {
        if (Math.abs(totalFlipAngle) >= 3.9269908f) {
            totalFlipAngle = 0;
            return true;
        }
        return false;
    }

    public void makeBigCar() {
        scaleTarget = 125;
        scaleFactor = 1.25f;
        mcBackWheel.rad = 34.5f;
        mcForwardWheel.rad = 34.5f;
    }

    public void makeSmallCar() {
        scaleTarget = 76;
        scaleFactor = 0.76f;
        mcBackWheel.rad = 22f;
        mcForwardWheel.rad = 22f;
    }

    public void makeNormalCar() {
        scaleTarget = 100;
        scaleFactor = 1f;
        mcBackWheel.rad = 27.5f;
        mcForwardWheel.rad = 27.5f;
    }

    public void damageCar(int amount) {
        health -= amount;
        if (health < 0) health = 0;
    }

    public void repairCar(int amount) {
        health += amount;
        if (health > MAX_HEALTH) health = MAX_HEALTH;
    }

    public void draw(Canvas canvas, Paint paint, float camX) {
        drawBody(canvas, paint, camX);
        mcBackWheel.draw(canvas, paint, camX);
        mcForwardWheel.draw(canvas, paint, camX);
    }

    private void drawBody(Canvas canvas, Paint paint, float camX) {
        float sx = x - camX;
        float sy = y;

        canvas.save();
        canvas.translate(sx, sy);
        canvas.rotate(rotation);
        canvas.scale(scaleFactor, scaleFactor);

        paint.setStyle(Paint.Style.FILL);

        paint.setStrokeWidth(8);
        paint.setColor(Color.rgb(70, 70, 65));
        canvas.drawLine(-55, 40, 55, 40, paint);
        canvas.drawLine(-55, 40, -42, -25, paint);
        canvas.drawLine(55, 40, 42, -25, paint);
        canvas.drawLine(-42, -25, 42, -25, paint);

        paint.setColor(Color.rgb(214, 214, 190));
        canvas.drawRoundRect(new RectF(-78, -42, 78, 20), 18, 18, paint);

        paint.setColor(Color.rgb(135, 135, 120));
        canvas.drawRect(-42, -67, 32, -39, paint);

        paint.setColor(Color.rgb(60, 60, 50));
        canvas.drawRect(-62, -54, -20, -41, paint);
        canvas.drawRect(12, -54, 56, -41, paint);

        paint.setColor(Color.YELLOW);
        Path flag = new Path();
        flag.moveTo(-30, -84);
        flag.lineTo(30, -68);
        flag.lineTo(-30, -52);
        flag.close();
        canvas.drawPath(flag, paint);

        canvas.restore();
    }
}

