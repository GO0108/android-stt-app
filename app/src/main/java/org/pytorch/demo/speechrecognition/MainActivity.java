package org.pytorch.demo.speechrecognition;

import android.Manifest;
import android.content.Intent;
import android.content.ClipData;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final int REQUEST_AUDIO_FILE = 42;
    private static final int SAMPLE_RATE = 16000;
    private static final int MAX_RECORDING_LENGTH = SAMPLE_RATE * 30;

    private ModelHelper modelHelper;
    private UiUtils uiUtils;
    private Spinner spinnerModel;
    private String selectedModel;
    private boolean isRecording = false;
    private String currentFileName = "gravado.wav";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.btnRecognize);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);
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

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                currentFileName = "gravado_" + timestamp + ".wav";

                processAudio(floatInputBuffer);
            }
        });

        btnSelectFile.setOnClickListener(v -> openAudioFilePicker());

        requestMicrophonePermission();
        requestWritePermission();
    }

    private void openAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/wav", "audio/x-wav"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Selecione arquivos .wav"), REQUEST_AUDIO_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    processUri(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                processUri(uri);
            }
        }
    }

    private void processUri(Uri audioUri) {
        if (audioUri != null) {
            try {
                // Permissão persistente de leitura (para acesso ao Drive)
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(audioUri, takeFlags);
            } catch (Exception ignored) {}

            currentFileName = getFileNameFromUri(audioUri);
            try {
                float[] audioData = AudioUtils.loadWavFile(getApplicationContext(), audioUri);
                processAudio(audioData);
            } catch (IOException e) {
                uiUtils.showError("Erro ao ler o arquivo: " + currentFileName);
            }
        }
    }

    private void processAudio(float[] floatInputBuffer) {
        if (floatInputBuffer == null) {
            uiUtils.showError("Áudio inválido");
            return;
        }

        AudioUtils.logAudioData(floatInputBuffer);

        long startTime = System.currentTimeMillis();
        String result = modelHelper.recognize(floatInputBuffer);
        long endTime = System.currentTimeMillis();

        double inferenceTimeSec = (endTime - startTime) / 1000.0;
        double audioDurationSec = floatInputBuffer.length / (double) SAMPLE_RATE;
        double rtf = inferenceTimeSec / audioDurationSec;

        String finalResult = String.format(
                "Resultado:\n%s\n\n⏱️ Duração do áudio: %.2f s\n🧠 Tempo de transcrição: %.2f s\n⚙️ RTF: %.2f",
                result, audioDurationSec, inferenceTimeSec, rtf
        );

        uiUtils.showResult(finalResult);

        String sanitizedResult = result.replaceAll("[\\r\\n]+", " ").trim();
        String logLine = String.format(Locale.US, "%s, %s, %.2f, %.2f, %.2f\n",
                currentFileName, sanitizedResult, audioDurationSec, inferenceTimeSec, rtf);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "transcription_log.txt");
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(logLine.getBytes());
            fos.close();

            Toast.makeText(this, "✅ Resultado salvo em:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            uiUtils.showError("Erro ao salvar em /Download/");
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "arquivo.wav";
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            result = new File(uri.getPath()).getName();
        }
        return result;
    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO
            );
        }
    }

    private void requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            }
        }
    }

    @Override
    protected void onDestroy() {
        uiUtils.stopTimer();
        super.onDestroy();
    }
}
