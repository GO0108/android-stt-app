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
    private final int maxAudioLengthSec;

    private HandlerThread timerThread;
    private Handler timerHandler;
    private int currentCount;
    private Runnable timerRunnable;

    public UiUtils(Button button, TextView textView, int maxAudioLengthSec) {
        this.button = button;
        this.textView = textView;
        this.maxAudioLengthSec = maxAudioLengthSec;
        initTimerRunnable();
    }

    private void initTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentCount >= maxAudioLengthSec) {
                    button.performClick(); // força clicar "Stop"
                    return;
                }
                timerHandler.postDelayed(this, 1000);
                button.post(() -> {
                    button.setText(String.format("Stop (%ds)", currentCount));
                    currentCount += 1;
                });
            }
        };
    }

    public void startRecordingUI() {
        button.post(() -> {
            button.setText("Stop (0s)");
            button.setEnabled(true);
        });
        currentCount = 1;
        startTimer();
    }

    public void startTimer() {
        stopTimer();
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
        stopTimer();
        button.post(() -> button.setText("Recognizing..."));
    }

    public void showResult(String result) {
        stopTimer();
        button.post(() -> {
            textView.setText(result);
            button.setEnabled(true);
            button.setText("Start");
        });
    }

    public void showError(String error) {
        stopTimer();
        button.post(() -> {
            textView.setText(error);
            button.setEnabled(true);
            button.setText("Start");
        });
    }
}
