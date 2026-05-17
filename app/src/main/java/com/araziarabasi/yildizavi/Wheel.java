package com.araziarabasi.yildizavi;

public class Wheel {
    public final Vec2 pos = new Vec2();
    public final Vec2 vel = new Vec2();
    public float radius = 28f;
    public float rotation = 0f;
    public boolean onGround = false;

    public Wheel(float x, float y) {
        pos.set(x, y);
    }

    public void update(float dt) {
        vel.y += 0.70f * dt;
        vel.x *= onGround ? 0.986f : 0.998f;
        vel.y *= 0.995f;

        if (vel.x > 24f) vel.x = 24f;
        if (vel.x < -18f) vel.x = -18f;

        pos.x += vel.x * dt;
        pos.y += vel.y * dt;
        rotation += vel.x * 4.2f * dt;
    }

    public void solveGround(Level level) {
        onGround = false;
        float gy = level.getGroundY(pos.x);

        if (pos.y + radius > gy) {
            float hit = vel.y;
            pos.y = gy - radius;
            vel.y = -hit * 0.10f;
            if (Math.abs(vel.y) < 1.2f) vel.y = 0f;
            float slope = level.getGroundAngle(pos.x);
            vel.x += (float)Math.sin(Math.toRadians(slope)) * 0.25f;
            onGround = true;
        }
    }
}
