//package org.pytorch.demo.speechrecognition;
//
//import android.os.Build;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class MainActivity extends AppCompatActivity implements Runnable {
//    private static final int REQUEST_RECORD_AUDIO = 13;
//    private static final int AUDIO_LEN_IN_SECOND = 3;
//    private static final int SAMPLE_RATE = 16000;
//    private static final int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_IN_SECOND;
//
//    private ModelHelper modelHelper;
//    private UiUtils uiUtils;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        Button button = findViewById(R.id.btnRecognize);
//        TextView textView = findViewById(R.id.tvResult);
//
//        modelHelper = new ModelHelper(getApplicationContext(), RECORDING_LENGTH);
//        uiUtils = new UiUtils(button, textView, AUDIO_LEN_IN_SECOND);
//
//        button.setOnClickListener(v -> {
//            uiUtils.startRecordingUI();
//            new Thread(MainActivity.this).start();
//        });
//
//        requestMicrophonePermission();
//    }
//
//    private void requestMicrophonePermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(
//                    new String[]{android.Manifest.permission.RECORD_AUDIO},
//                    REQUEST_RECORD_AUDIO
//            );
//        }
//    }
//
//    @Override
//    public void run() {
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
//
//        float[] floatInputBuffer = AudioUtils.recordAudio(SAMPLE_RATE, RECORDING_LENGTH);
//
//        if (floatInputBuffer == null) {
//            uiUtils.showError("Erro na captura de áudio");
//            return;
//        }
//
//        AudioUtils.logAudioData(floatInputBuffer);
//        uiUtils.showRecognizing();
//
//        final String result = modelHelper.recognize(floatInputBuffer);
//        uiUtils.showResult(result);
//    }
//
//    @Override
//    protected void onDestroy() {
//        uiUtils.stopTimer();
//        super.onDestroy();
//    }
//}

package org.pytorch.demo.speechrecognition;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final int AUDIO_LEN_IN_SECOND = 3;
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_IN_SECOND;

    private ModelHelper modelHelper;
    private UiUtils uiUtils;
    private Spinner spinnerModel;
    private String selectedModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.btnRecognize);
        TextView textView = findViewById(R.id.tvResult);
        spinnerModel = findViewById(R.id.spinnerModel);

        // Inicialização com modelo padrão
        selectedModel = getResources().getStringArray(R.array.model_list)[0];
        modelHelper = new ModelHelper(getApplicationContext(), RECORDING_LENGTH, selectedModel);
        uiUtils = new UiUtils(button, textView, AUDIO_LEN_IN_SECOND);

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = parent.getItemAtPosition(position).toString();
                modelHelper = new ModelHelper(getApplicationContext(), RECORDING_LENGTH, selectedModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        button.setOnClickListener(v -> {
            uiUtils.startRecordingUI();
            new Thread(MainActivity.this).start();
        });

        requestMicrophonePermission();
    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO
            );
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        float[] floatInputBuffer = AudioUtils.recordAudio(SAMPLE_RATE, RECORDING_LENGTH);

        if (floatInputBuffer == null) {
            uiUtils.showError("Erro na captura de áudio");
            return;
        }

        AudioUtils.logAudioData(floatInputBuffer);
        uiUtils.showRecognizing();

        final String result = modelHelper.recognize(floatInputBuffer);
        uiUtils.showResult(result);
    }

    @Override
    protected void onDestroy() {
        uiUtils.stopTimer();
        super.onDestroy();
    }
}