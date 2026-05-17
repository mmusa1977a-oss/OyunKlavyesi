package com.araziarabasi.yildizavi;

public class Vec2 {
    public float x;
    public float y;

    public Vec2() {
        this(0, 0);
    }

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vec2 add(float ax, float ay) {
        x += ax;
        y += ay;
        return this;
    }

    public Vec2 mul(float m) {
        x *= m;
        y *= m;
        return this;
    }

    public float dst(Vec2 o) {
        float dx = x - o.x;
        float dy = y - o.y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    public float dst(float ox, float oy) {
        float dx = x - ox;
        float dy = y - oy;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }
}
