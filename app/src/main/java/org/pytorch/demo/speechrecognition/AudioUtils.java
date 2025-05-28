package org.pytorch.demo.speechrecognition;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioUtils {

    private static final String TAG = "AudioUtils";

    private static AudioRecord recorder = null;
    private static short[] recordingBuffer;
    private static int bufferOffset = 0;
    private static int maxRecordingLength = 0;
    private static boolean isRecording = false;

    public static void startRecording(int sampleRate, int maxLength) {
        maxRecordingLength = maxLength;
        int bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord não inicializado!");
            return;
        }

        recordingBuffer = new short[maxRecordingLength];
        bufferOffset = 0;
        isRecording = true;

        recorder.startRecording();

        new Thread(() -> {
            short[] audioBuffer = new short[bufferSize / 2];

            while (isRecording && bufferOffset < maxRecordingLength) {
                int read = recorder.read(audioBuffer, 0, audioBuffer.length);
                if (read > 0) {
                    int toCopy = Math.min(read, maxRecordingLength - bufferOffset);
                    System.arraycopy(audioBuffer, 0, recordingBuffer, bufferOffset, toCopy);
                    bufferOffset += toCopy;
                }
            }
        }).start();
    }

    public static float[] stopRecording() {
        if (recorder == null) return null;

        isRecording = false;

        try {
            recorder.stop();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parar gravação", e);
        }
        recorder.release();
        recorder = null;

        short[] finalBuffer = new short[bufferOffset];
        System.arraycopy(recordingBuffer, 0, finalBuffer, 0, bufferOffset);

        return convertToFloat(finalBuffer);
    }

    public static float[] convertToFloat(short[] input) {
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i] / (float) Short.MAX_VALUE;
        }
        return output;
    }

    public static void logAudioData(float[] audioData) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎧 floatInputBuffer = [");

        for (int i = 0; i < audioData.length; i++) {
            sb.append(audioData[i]);
            if (i < audioData.length - 1) sb.append(", ");
        }

        sb.append("]");
        Log.d(TAG, sb.toString());
    }

    public static float[] loadWavFile(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        byte[] wavBytes = byteBuffer.toByteArray();
        ByteBuffer byteBuf = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN);

        // Pula cabeçalho WAV de 44 bytes
        byteBuf.position(44);

        int samples = (wavBytes.length - 44) / 2;
        float[] audioData = new float[samples];
        for (int i = 0; i < samples; i++) {
            audioData[i] = byteBuf.getShort() / (float) Short.MAX_VALUE;
        }

        return audioData;
    }
}
