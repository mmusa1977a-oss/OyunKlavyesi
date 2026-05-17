package com.araziarabasi.yildizavi;

public class CVector2D {
    public float x;
    public float y;

    public CVector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void reinit(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getUgol() {
        return (float)Math.atan2(y, x);
    }

    public CVector2D duplicate() {
        return new CVector2D(x, y);
    }

    public void copyTo(CVector2D v) {
        v.x = x;
        v.y = y;
    }

    public void minus(CVector2D v) {
        x -= v.x;
        y -= v.y;
    }

    public CVector2D minusNew(CVector2D v) {
        return new CVector2D(x - v.x, y - v.y);
    }

    public void normalize() {
        float m = (float)Math.sqrt(x * x + y * y);
        if (m < 0.00001f) return;
        x /= m;
        y /= m;
    }

    public CVector2D reverseNew() {
        return new CVector2D(-x, -y);
    }

    public float scalar(CVector2D v) {
        return x * v.x + y * v.y;
    }

    public float modul() {
        return (float)Math.sqrt(x * x + y * y);
    }

    public void reflectFromNormal(CVector2D n) {
        float u = (float)Math.atan2(n.y, n.x);
        rotate(-u);
        x = -x;
        rotate(u);
    }

    public void rotate(float u) {
        float ox = x;
        float oy = y;
        float c = (float)Math.cos(u);
        float s = (float)Math.sin(u);
        x = ox * c - oy * s;
        y = ox * s + oy * c;
    }

    public CVector2D rotateNew(float u) {
        float c = (float)Math.cos(u);
        float s = (float)Math.sin(u);
        return new CVector2D(x * c - y * s, x * s + y * c);
    }

    public void mult(float k) {
        x *= k;
        y *= k;
    }

    public CVector2D multNew(float k) {
        return new CVector2D(x * k, y * k);
    }

    public void plus(CVector2D v) {
        x += v.x;
        y += v.y;
    }

    public CVector2D plusNew(CVector2D v) {
        return new CVector2D(x + v.x, y + v.y);
    }

    public float getDistanceTo(CVector2D p1, CVector2D p2) {
        float a = p1.y - p2.y;
        float b = p2.x - p1.x;
        float c = p1.x * (p2.y - p1.y) - p1.y * (p2.x - p1.x);
        float d = (float)Math.sqrt(a * a + b * b);
        if (d < 0.00001f) return 0;
        a /= d;
        b /= d;
        c /= d;
        return a * x + b * y + c;
    }

    @Override
    public String toString() {
        return "{ " + x + ", " + y + " }";
    }
}
