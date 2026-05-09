package com.gursoy.oyunklavyesi;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;

public class FloatingControlService extends Service {

    private WindowManager windowManager;
    private Button bubble;
    private LinearLayout panel;

    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    private boolean panelVisible = false;
    private float touchX, touchY;
    private int startX, startY;
    private boolean moved = false;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createBubble();
        createPanel();
    }

    private void createBubble() {
        bubble = new Button(this);
        bubble.setText("🎮");
        bubble.setTextSize(22);
        bubble.setAlpha(0.75f);

        bubbleParams = new WindowManager.LayoutParams(
                70,
                70,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 20;
        bubbleParams.y = 300;

        bubble.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    moved = false;
                    startX = bubbleParams.x;
                    startY = bubbleParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - touchX);
                    int dy = (int) (event.getRawY() - touchY);

                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        moved = true;
                    }

                    bubbleParams.x = startX + dx;
                    bubbleParams.y = startY + dy;
                    windowManager.updateViewLayout(bubble, bubbleParams);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        togglePanel();
                    }
                    return true;
            }
            return false;
        });

        windowManager.addView(bubble, bubbleParams);
    }

    private void createPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setAlpha(0.75f);
        panel.setPadding(10, 10, 10, 10);

        Button up = makeButton("↑");
        Button left = makeButton("←");
        Button down = makeButton("↓");
        Button right = makeButton("→");
        Button space = makeButton("SPACE");

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setGravity(Gravity.CENTER);

        middle.addView(left);
        middle.addView(down);
        middle.addView(right);

        panel.addView(up);
        panel.addView(middle);
        panel.addView(space);

        panelParams = new WindowManager.LayoutParams(
                320,
                320,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 120;
        panelParams.y = 500;

        panel.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = panelParams.x;
                    startY = panelParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    panelParams.x = startX + (int) (event.getRawX() - touchX);
                    panelParams.y = startY + (int) (event.getRawY() - touchY);
                    windowManager.updateViewLayout(panel, panelParams);
                    return true;
            }
            return true;
        });
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(text.equals("SPACE") ? 16 : 28);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(90, 75);
        lp.setMargins(5, 5, 5, 5);
        b.setLayoutParams(lp);

        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                b.setAlpha(0.45f);
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                b.setAlpha(1f);
            }
            return true;
        });

        return b;
    }

    private void togglePanel() {
        if (panelVisible) {
            try {
                windowManager.removeView(panel);
            } catch (Exception ignored) {}
            panelVisible = false;
        } else {
            try {
                windowManager.addView(panel, panelParams);
            } catch (Exception ignored) {}
            panelVisible = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (bubble != null) windowManager.removeView(bubble);
        } catch (Exception ignored) {}

        try {
            if (panel != null && panelVisible) windowManager.removeView(panel);
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
