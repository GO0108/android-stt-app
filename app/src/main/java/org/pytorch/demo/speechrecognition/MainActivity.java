package org.pytorch.demo.speechrecognition;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final int SAMPLE_RATE = 16000;
    private static final int MAX_RECORDING_LENGTH = SAMPLE_RATE * 30; // Máximo 30 segundos

    private ModelHelper modelHelper;
    private UiUtils uiUtils;
    private Spinner spinnerModel;
    private String selectedModel;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.btnRecognize);
        TextView textView = findViewById(R.id.tvResult);
        spinnerModel = findViewById(R.id.spinnerModel);

        selectedModel = getResources().getStringArray(R.array.model_list)[0];
        modelHelper = new ModelHelper(getApplicationContext(), selectedModel);
        uiUtils = new UiUtils(button, textView, 30);

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = parent.getItemAtPosition(position).toString();
                modelHelper = new ModelHelper(getApplicationContext(), selectedModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        button.setOnClickListener(v -> {
            if (!isRecording) {
                isRecording = true;
                uiUtils.startRecordingUI();
                AudioUtils.startRecording(SAMPLE_RATE, MAX_RECORDING_LENGTH);
            } else {
                isRecording = false;
                uiUtils.showRecognizing();
                float[] floatInputBuffer = AudioUtils.stopRecording();

                if (floatInputBuffer == null) {
                    uiUtils.showError("Erro na captura de áudio");
                    return;
                }

                AudioUtils.logAudioData(floatInputBuffer);

                // ⏱️ Marca início da inferência
                long startTime = System.currentTimeMillis();

                String result = modelHelper.recognize(floatInputBuffer);

                // ⏱️ Marca fim da inferência
                long endTime = System.currentTimeMillis();
                double inferenceTimeSec = (endTime - startTime) / 1000.0;

                // 🎧 Duração do áudio original
                double audioDurationSec = floatInputBuffer.length / (double) SAMPLE_RATE;

                // 🧠 RTF = duração do áudio / tempo de inferência
                double rtf = inferenceTimeSec / audioDurationSec  ;

                // 🔥 Exibe resultado junto com tempo e RTF
                String finalResult = String.format(
                        "Resultado:\n%s\n\n⏱️ Duração do áudio: %.2f s\n🧠 Tempo de transcrição: %.2f s\n⚙️ RTF: %.2f",
                        result, audioDurationSec, inferenceTimeSec, rtf
                );


                uiUtils.showResult(finalResult);
            }
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
    protected void onDestroy() {
        uiUtils.stopTimer();
        super.onDestroy();
    }
}