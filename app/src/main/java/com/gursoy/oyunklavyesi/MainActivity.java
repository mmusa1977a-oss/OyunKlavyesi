package com.gursoy.oyunklavyesi;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    Button btnBubble, btnUp, btnDown, btnLeft, btnRight, btnSpace;
    LinearLayout controlPanel;

    float dX, dY;
    boolean moved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        btnBubble = findViewById(R.id.btnBubble);
        controlPanel = findViewById(R.id.controlPanel);

        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnSpace = findViewById(R.id.btnSpace);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        webView.loadUrl("https://www.gameszap.com/game/2141/dune-buggy.html");

        makeDraggableBubble(btnBubble);
        makeDraggable(controlPanel);

        btnBubble.setOnClickListener(v -> {
            if (controlPanel.getVisibility() == View.VISIBLE) {
                controlPanel.setVisibility(View.GONE);
            } else {
                controlPanel.setVisibility(View.VISIBLE);
            }
        });

        setupKey(btnUp, "ArrowUp", "ArrowUp");
        setupKey(btnDown, "ArrowDown", "ArrowDown");
        setupKey(btnLeft, "ArrowLeft", "ArrowLeft");
        setupKey(btnRight, "ArrowRight", "ArrowRight");
        setupKey(btnSpace, " ", "Space");
    }

    private void makeDraggableBubble(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    moved = false;
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    return false;

                case MotionEvent.ACTION_MOVE:
                    moved = true;
                    v.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                    return true;

                case MotionEvent.ACTION_UP:
                    return moved;
            }
            return false;
        });
    }

    private void makeDraggable(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    v.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                    return true;
            }
            return true;
        });
    }

    private void setupKey(Button button, String key, String code) {
        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey("keydown", key, code);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                sendKey("keyup", key, code);
                return true;
            }

            return true;
        });
    }

    private void sendKey(String type, String key, String code) {
        int keyCode = getKeyCode(code);

        String js =
                "var e = new KeyboardEvent('" + type + "', {" +
                        "key: '" + key + "'," +
                        "code: '" + code + "'," +
                        "keyCode: " + keyCode + "," +
                        "which: " + keyCode + "," +
                        "bubbles: true," +
                        "cancelable: true" +
                        "});" +
                        "window.dispatchEvent(e);" +
                        "document.dispatchEvent(e);" +
                        "if(document.body){document.body.dispatchEvent(e);}";

        webView.evaluateJavascript(js, null);
    }

    private int getKeyCode(String code) {
        switch (code) {
            case "ArrowUp":
                return 38;
            case "ArrowDown":
                return 40;
            case "ArrowLeft":
                return 37;
            case "ArrowRight":
                return 39;
            case "Space":
                return 32;
            default:
                return 0;
        }
    }
}
