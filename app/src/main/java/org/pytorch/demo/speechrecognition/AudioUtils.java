package org.pytorch.demo.speechrecognition;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioUtils {

    private static final String TAG = "AudioUtils";

    /**
     * Grava o áudio por um tempo definido e retorna um array de float normalizado (-1.0 a 1.0)
     */
    public static float[] recordAudio(int sampleRate, int recordingLength) {

        int bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        AudioRecord record = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord não inicializado!");
            return null;
        }

        Log.d(TAG, "AudioRecord inicializado com sucesso");

        record.startRecording();

        long shortsRead = 0;
        int recordingOffset = 0;
        short[] audioBuffer = new short[bufferSize / 2];
        short[] recordingBuffer = new short[recordingLength];

        while (shortsRead < recordingLength) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;
            System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);
            recordingOffset += numberOfShort;
        }

        record.stop();
        record.release();

        return convertToFloat(recordingBuffer);
    }

    /**
     * Converte um array de short (PCM 16 bits) para float (-1.0 a 1.0)
     */
    public static float[] convertToFloat(short[] input) {
        float[] output = new float[input.length];

        for (int i = 0; i < input.length; i++) {
            output[i] = input[i] / (float) Short.MAX_VALUE;
        }

        return output;
    }

    /**
     * (Opcional) Faz log dos dados do áudio
     */
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
}
