package com.araziarabasi.yildizavi;

import android.graphics.*;

public class CWheel {
    public CParticle particle;
    public float rad;
    public float maxAccSpeed;
    public float speed;
    public float accSpeed;
    public CVector2D rot = new CVector2D(1, 0);
    public boolean intersectionFlag;
    public float lastYSpeed;

    public float maxSpeed = 180f;
    public float koeffSpring = 0.03f;
    public float koeffSlip = 0.15f;
    public float koeffFriction = 0.97f;

    public void init(CParticle particle, float maxAccSpeed, float rad) {
        this.particle = particle;
        this.rad = rad;
        this.maxAccSpeed = maxAccSpeed * 4f;
        this.speed = 0;
        this.accSpeed = 0;
        this.rot.reinit(1, 0);
        this.intersectionFlag = false;
        this.lastYSpeed = 0;
    }

    public void process(float dTime, float acc, float koeffDamp) {
        accSpeed = clamp(accSpeed + acc * dTime, -maxAccSpeed, maxAccSpeed);
        speed = clamp(speed * koeffDamp + accSpeed, -maxSpeed * dTime, maxSpeed * dTime);
        rot.rotate(speed / rad);
        lastYSpeed = particle.pos.y - particle.prevPos.y;
    }

    public void checkCollision(DuneLevel level) {
        intersectionFlag = false;

        float groundY = level.getGroundY(particle.pos.x);

        if (particle.pos.y + rad > groundY) {
            intersectionFlag = true;

            float oldY = particle.pos.y;
            particle.pos.y = groundY - rad;

            float vx = particle.pos.x - particle.prevPos.x;
            float vy = oldY - particle.prevPos.y;

            float slope = level.getGroundAngle(particle.pos.x);
            float nx = (float)-Math.sin(slope);
            float ny = (float)Math.cos(slope);

            float dot = vx * nx + vy * ny;
            if (dot > 0) {
                vx -= dot * nx * 1.15f;
                vy -= dot * ny * 1.15f;
            }

            vx *= koeffFriction;
            vy *= 0.25f;

            particle.prevPos.x = particle.pos.x - vx;
            particle.prevPos.y = particle.pos.y - vy;

            float tangent = (float)Math.cos(slope);
            particle.addVelocity(tangent * speed * koeffSpring, 0);
        }
    }

    public void draw(Canvas canvas, Paint paint, float camX) {
        float sx = particle.pos.x - camX;
        float sy = particle.pos.y;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(22, 22, 22));
        canvas.drawCircle(sx, sy, rad, paint);

        paint.setColor(Color.rgb(70, 70, 70));
        canvas.drawCircle(sx, sy, rad * 0.68f, paint);

        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(sx, sy, rad * 0.33f, paint);

        float ang = (float)Math.atan2(rot.y, rot.x);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        canvas.drawLine(
                sx + (float)Math.cos(ang) * rad * 0.55f,
                sy + (float)Math.sin(ang) * rad * 0.55f,
                sx - (float)Math.cos(ang) * rad * 0.55f,
                sy - (float)Math.sin(ang) * rad * 0.55f,
                paint
        );
        canvas.drawLine(
                sx + (float)Math.cos(ang + Math.PI / 2) * rad * 0.55f,
                sy + (float)Math.sin(ang + Math.PI / 2) * rad * 0.55f,
                sx - (float)Math.cos(ang + Math.PI / 2) * rad * 0.55f,
                sy - (float)Math.sin(ang + Math.PI / 2) * rad * 0.55f,
                paint
        );
    }

    private float clamp(float v, float mn, float mx) {
        return Math.max(mn, Math.min(mx, v));
    }
}
