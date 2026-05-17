package com.araziarabasi.yildizavi;

public class CParticle {
    public CVector2D pos;
    public CVector2D prevPos;
    public float mass;
    public CVector2D acc;

    public CParticle(float x, float y, float mass) {
        pos = new CVector2D(x, y);
        prevPos = pos.duplicate();
        this.mass = mass;
        acc = new CVector2D(0, 0);
    }

    public void verlet(float dTime, float koeffDamp) {
        CVector2D old = pos.duplicate();
        pos.x += (pos.x - prevPos.x) * koeffDamp + acc.x * dTime;
        pos.y += (pos.y - prevPos.y) * koeffDamp + acc.y * dTime;
        old.copyTo(prevPos);
    }

    public CVector2D getVelocityVector() {
        return new CVector2D(pos.x - prevPos.x, pos.y - prevPos.y);
    }

    public void setVelocity(float vx, float vy) {
        prevPos.x = pos.x - vx;
        prevPos.y = pos.y - vy;
    }

    public void addVelocity(float vx, float vy) {
        prevPos.x -= vx;
        prevPos.y -= vy;
    }
}
