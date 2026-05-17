package com.araziarabasi.yildizavi;

import android.graphics.*;

public class Bonus {
    public static final int STAR = 1;
    public static final int REPAIR = 2;
    public static final int BIG = 3;
    public static final int SMALL = 4;
    public static final int SLOW = 5;
    public static final int MINUS = 6;
    public static final int WHEEL = 7;

    public float x;
    public float y;
    public int type;
    public boolean taken = false;

    public Bonus(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw(Canvas canvas, Paint paint, float camX) {
        if (taken) return;

        float sx = x - camX;
        if (sx < -80 || sx > canvas.getWidth() + 80) return;

        if (type == STAR) {
            drawStar(canvas, paint, sx, y, 22);
            return;
        }

        int color = Color.GREEN;
        String text = "+";

        if (type == REPAIR) { color = Color.GREEN; text = "+"; }
        else if (type == BIG) { color = Color.MAGENTA; text = "B"; }
        else if (type == SMALL) { color = Color.CYAN; text = "S"; }
        else if (type == SLOW) { color = Color.BLUE; text = "T"; }
        else if (type == MINUS) { color = Color.RED; text = "-"; }
        else if (type == WHEEL) { color = Color.rgb(255, 145, 0); text = "W"; }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(sx + 3, y + 3, 25, paint);

        paint.setColor(color);
        canvas.drawCircle(sx, y, 25, paint);

        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(28);
        canvas.drawText(text, sx, y + 10, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStar(Canvas canvas, Paint paint, float x, float y, float r) {
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
}
