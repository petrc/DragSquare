package com.example.dragsquare;

import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    static final int NETWORK_REQUEST_INTERVAL = 10;

    Point screenSize;

    ViewGroup viewDrag;
    ViewGroup viewDrop;
    View viewRotate;
    TextView timeText;

    float[] initialPosition;
    float[] newPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseViews();
        initialiseRotationAnimation();
        initialiseTouchDrag();
        initialiseNetworkThreads();
    }

    void initialiseViews() {

        viewDrag = findViewById(R.id.viewDrag);
        viewDrop = findViewById(R.id.viewDrop);
        viewRotate = findViewById(R.id.viewRotate);
        timeText = findViewById(R.id.textTime);

        Display display = getWindowManager().getDefaultDisplay();
        screenSize = new Point();
        display.getSize(screenSize);

        viewDrop.setY(screenSize.y - viewDrop.getHeight());

        initialPosition = new float[2];
        newPosition = new float[2];

        viewDrag.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        initialPosition[0] = viewDrag.getX();
                        initialPosition[1] = viewDrag.getY();
                        viewDrag.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    void initialiseTouchDrag() {

        viewDrag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {

                        newPosition[0] = viewDrag.getX() - event.getRawX();
                        newPosition[1] = viewDrag.getY() - event.getRawY();

                        viewDrop.animate().y(screenSize.y - viewDrop.getHeight()).setDuration(600);

                        break;
                    }
                    case MotionEvent.ACTION_UP: {

                        float dragViewBottom = viewDrag.getHeight() * 0.75f + viewDrag.getY();

                        if (dragViewBottom > viewDrop.getY()) {

                            float centerX = viewDrop.getWidth() / 2.0f - viewDrag.getWidth() / 2.0f;
                            float centerY = screenSize.y - viewDrag.getHeight() * 1.8f;

                            viewDrag.animate().x(centerX).y(centerY).setDuration(1000);

                        } else {

                            viewDrag.animate().x(initialPosition[0]).y(initialPosition[1]).setDuration(1000);
                            viewDrop.animate().y(screenSize.y).setDuration(600);
                        }

                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {

                        viewDrag.animate()
                                .x(event.getRawX() + newPosition[0])
                                .y(event.getRawY() + newPosition[1])
                                .setDuration(0)
                                .start();
                        break;
                    }
                }

                return false;
            }
        });

    }

    void initialiseRotationAnimation() {

        ObjectAnimator imageViewObjectAnimator = ObjectAnimator.ofFloat(viewRotate, "rotation", 0f, 360f);
        imageViewObjectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        imageViewObjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
        imageViewObjectAnimator.setDuration(1000);
        imageViewObjectAnimator.setInterpolator(new LinearInterpolator());
        imageViewObjectAnimator.start();
    }

    void initialiseNetworkThreads() {

        final OkHttpClient okClient = new OkHttpClient();

        Thread timerThread = new Thread() {
            public void run() {
                try {
                    while (true) {

                        Request request = new Request.Builder()
                                .url("https://dateandtimeasjson.appspot.com/")
                                .build();

                        try (Response response = okClient.newCall(request).execute()) {
                            JSONObject obj = new JSONObject(response.body().string());
                            final String time = obj.getString("datetime");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeText.setText(time.split(" ")[1]);
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        sleep(NETWORK_REQUEST_INTERVAL);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        timerThread.start();
    }
}
