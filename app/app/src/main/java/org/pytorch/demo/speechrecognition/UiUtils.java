package org.pytorch.demo.speechrecognition;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class UiUtils {
    private static final String TAG = "UiUtils";

    private final Button button;
    private final TextView textView;
    private final int audioLengthSec;

    private HandlerThread timerThread;
    private Handler timerHandler;
    private int currentCount;
    private Runnable timerRunnable;

    public UiUtils(Button button, TextView textView, int audioLengthSec) {
        this.button = button;
        this.textView = textView;
        this.audioLengthSec = audioLengthSec;
        initTimerRunnable();
    }

    private void initTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentCount >= audioLengthSec) {
                    return;
                }
                timerHandler.postDelayed(this, 1000);
                button.post(() -> {
                    button.setText(String.format("Listening - %ds left", audioLengthSec - currentCount));
                    currentCount += 1;
                });
            }
        };
    }

    public void startRecordingUI() {
        button.post(() -> {
            button.setText(String.format("Listening - %ds left", audioLengthSec));
            button.setEnabled(false);
        });
        currentCount = 1;
        startTimer();
    }

    public void startTimer() {
        stopTimer(); // Garante que não há timers anteriores rodando
        timerThread = new HandlerThread("AudioTimer");
        timerThread.start();
        timerHandler = new Handler(timerThread.getLooper());
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    public void stopTimer() {
        if (timerThread != null) {
            timerThread.quitSafely();
            try {
                timerThread.join();
                timerThread = null;
                timerHandler = null;
                currentCount = 1;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping timer thread", e);
            }
        }
    }

    public void showRecognizing() {
        button.post(() -> button.setText("Recognizing..."));
    }

    public void showResult(String result) {
        button.post(() -> {
            textView.setText(result);
            button.setEnabled(true);
            button.setText("Start");
        });
    }

    public void showError(String error) {
        button.post(() -> {
            textView.setText(error);
            button.setEnabled(true);
            button.setText("Start");
        });
    }
}