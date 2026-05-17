package com.araziarabasi.yildizavi;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Particle {
    public float x, y, vx, vy, size;
    public int life;
    public int color;

    public Particle(float x, float y, float vx, float vy, int life, float size, int color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
        this.color = color;
    }

    public boolean update() {
        x += vx;
        y += vy;
        vy += 0.24f;
        life--;
        return life > 0;
    }

    public void draw(Canvas canvas, Paint paint, float camX) {
        paint.setColor(color);
        canvas.drawCircle(x - camX, y, size, paint);
    }
}
