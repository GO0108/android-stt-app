package org.pytorch.demo.speechrecognition;

import android.content.Context;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;

public class ModelHelper {
    private static final String TAG = "ModelHelper";
    private final Module module;
    private final int recordingLength;

    public ModelHelper(Context context, int recordingLength, String modelFileName) {
        this.recordingLength = recordingLength;
        this.module = LiteModuleLoader.load(assetFilePath(context, modelFileName));
    }

    private String assetFilePath(Context context, String modelFileName) {
        File file = new File(context.getFilesDir(), modelFileName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(modelFileName);
             OutputStream os = new FileOutputStream(file)) {

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error processing asset " + modelFileName, e);
            throw new RuntimeException("Error loading model", e);
        }
    }

    public String recognize(float[] floatInputBuffer) {
        if (floatInputBuffer.length != recordingLength) {
            throw new IllegalArgumentException("Input buffer size mismatch. Expected: " + recordingLength);
        }

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(recordingLength);
        inTensorBuffer.put(floatInputBuffer);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, recordingLength});

        return module.forward(IValue.from(inTensor)).toStr();
    }
}


//package org.pytorch.demo.speechrecognition;
//
//import android.content.Context;
//import android.util.Log;
//
//import org.pytorch.IValue;
//import org.pytorch.LiteModuleLoader;
//import org.pytorch.Module;
//import org.pytorch.Tensor;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.FloatBuffer;
//
//public class ModelHelper {
//    private static final String TAG = "ModelHelper";
//    private static final String MODEL_FILE = "wav2vec2.ptl";
//
//    private final Module module;
//    private final int recordingLength;
//
//    public ModelHelper(Context context, int recordingLength) {
//        this.recordingLength = recordingLength;
//        this.module = LiteModuleLoader.load(assetFilePath(context));
//    }
//
//    private String assetFilePath(Context context) {
//        File file = new File(context.getFilesDir(), MODEL_FILE);
//        if (file.exists() && file.length() > 0) {
//            return file.getAbsolutePath();
//        }
//
//        try (InputStream is = context.getAssets().open(MODEL_FILE);
//             OutputStream os = new FileOutputStream(file)) {
//
//            byte[] buffer = new byte[4 * 1024];
//            int read;
//            while ((read = is.read(buffer)) != -1) {
//                os.write(buffer, 0, read);
//            }
//            os.flush();
//            return file.getAbsolutePath();
//        } catch (IOException e) {
//            Log.e(TAG, "Error processing asset " + MODEL_FILE, e);
//            throw new RuntimeException("Error loading model", e);
//        }
//    }
//
//    public String recognize(float[] floatInputBuffer) {
//        if (floatInputBuffer.length != recordingLength) {
//            throw new IllegalArgumentException("Input buffer size mismatch. Expected: " + recordingLength);
//        }
//
//        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(recordingLength);
//        inTensorBuffer.put(floatInputBuffer);
//
//        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, recordingLength});
//
//        return module.forward(IValue.from(inTensor)).toStr();
//    }
//}


//----ONNX MODEL ----
//package org.pytorch.demo.speechrecognition;
//
//import android.content.Context;
//import android.util.Log;
//
//import ai.onnxruntime.OnnxTensor;
//import ai.onnxruntime.OrtEnvironment;
//import ai.onnxruntime.OrtSession;
//import ai.onnxruntime.OrtException;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.FloatBuffer;
//import java.util.Collections;
//import java.util.Map;
//
//public class ModelHelper {
//    private static final String TAG = "ModelHelper";
//    private static final String MODEL_FILE = "wav2vec2.onnx";
//
//    private final OrtEnvironment env;
//    private final OrtSession session;
//    private final int recordingLength;
//
//    public ModelHelper(Context context, int recordingLength) {
//        this.recordingLength = recordingLength;
//        try {
//            env = OrtEnvironment.getEnvironment();
//            session = env.createSession(assetFilePath(context));
//        } catch (OrtException e) {
//            throw new RuntimeException("Erro ao carregar modelo ONNX", e);
//        }
//    }
//
//    private String assetFilePath(Context context) {
//        File file = new File(context.getFilesDir(), MODEL_FILE);
//        Log.d(TAG, "Model path: " + file.getAbsolutePath());
//        Log.d(TAG, "Model exists: " + file.exists() + ", size: " + file.length());
//        if (file.exists() && file.length() > 0) {
//            return file.getAbsolutePath();
//        }
//
//        try (InputStream is = context.getAssets().open(MODEL_FILE);
//             OutputStream os = new FileOutputStream(file)) {
//
//            byte[] buffer = new byte[4 * 1024];
//            int read;
//            while ((read = is.read(buffer)) != -1) {
//                os.write(buffer, 0, read);
//            }
//            os.flush();
//            return file.getAbsolutePath();
//        } catch (IOException e) {
//            Log.e(TAG, "Erro ao processar o asset " + MODEL_FILE, e);
//            throw new RuntimeException("Erro ao carregar o modelo", e);
//        }
//    }
//
//    public String recognize(float[] floatInputBuffer) {
//        if (floatInputBuffer.length != recordingLength) {
//            throw new IllegalArgumentException("Tamanho do buffer incorreto. Esperado: " + recordingLength);
//        }
//
//        try {
//            // Prepara tensor de entrada [1, recordingLength]
//            long[] shape = new long[]{1, recordingLength};
//            OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatInputBuffer), shape);
//
//            // Executa inferência
//            OrtSession.Result results = session.run(Collections.singletonMap("waveforms", inputTensor));
//
//            // Recupera saída (assumindo que a saída é uma sequência de índices)
//            float[][] outputArray = (float[][]) results.get(0).getValue();
//            int[] predictedIds = new int[outputArray[0].length];
//            for (int i = 0; i < outputArray[0].length; i++) {
//                predictedIds[i] = (int) outputArray[0][i];
//            }
//
//            // Converte para texto
//            String transcription = decode(predictedIds);
//
//            inputTensor.close();
//            results.close();
//
//            return transcription;
//
//        } catch (Exception e) {
//            Log.e(TAG, "Erro na inferência ONNX", e);
//            return "Erro na transcrição";
//        }
//    }
//
//    private static final String[] LABELS = new String[]{
//            "<s>", "<pad>", "</s>", "<unk>", "|", "e", "t", "a", "o", "n", "i", "h", "s",
//            "r", "d", "l", "u", "m", "w", "c", "f", "g", "y", "p", "b", "v", "k", "'", "x",
//            "j", "q", "z"
//    };
//
//    private String decode(int[] tokens) {
//        StringBuilder sb = new StringBuilder();
//        String prev = "";
//
//        for (int index : tokens) {
//            if (index < 0 || index >= LABELS.length) continue;
//            String c = LABELS[index];
//            if (c.equals("<s>") || c.equals("<pad>") || c.equals("</s>") || c.equals("<unk>")) {
//                prev = "";
//                continue;
//            }
//            if (c.equals(prev)) continue;
//            sb.append(c);
//            prev = c;
//        }
//
//        return sb.toString().replace("|", " ").trim();
//    }
//}



//----TFLITE MODEL ----
//
//package org.pytorch.demo.speechrecognition;
//
//import android.content.Context;
//import android.util.Log;
//
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.Tensor;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.util.Arrays;
//
//public class ModelHelper {
//    private static final String TAG = "ModelHelper";
//    private static final String MODEL_FILE = "wav2vec2.tflite";
//
//    private final Interpreter interpreter;
//    private final int recordingLength;
//    private final int outputSize;
//
//    public ModelHelper(Context context, int recordingLength) {
//        this.recordingLength = recordingLength;
//        try {
//            interpreter = new Interpreter(loadModelFile(context));
//            Tensor inputTensor = interpreter.getInputTensor(0);
//            Tensor outputTensor = interpreter.getOutputTensor(0);
//            outputSize = outputTensor.shape()[1];
//        } catch (Exception e) {
//            throw new RuntimeException("Erro ao carregar modelo TFLite", e);
//        }
//    }
//
//    private ByteBuffer loadModelFile(Context context) throws IOException {
//        File file = new File(context.getFilesDir(), MODEL_FILE);
//        if (!file.exists() || file.length() == 0) {
//            try (InputStream is = context.getAssets().open(MODEL_FILE);
//                 OutputStream os = new FileOutputStream(file)) {
//                byte[] buffer = new byte[4 * 1024];
//                int read;
//                while ((read = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, read);
//                }
//                os.flush();
//            }
//        }
//        return ByteBuffer.allocateDirect((int) file.length()).order(ByteOrder.nativeOrder());
//    }
//
//    public String recognize(float[] floatInputBuffer) {
//        if (floatInputBuffer.length != recordingLength) {
//            throw new IllegalArgumentException("Tamanho do buffer incorreto. Esperado: " + recordingLength);
//        }
//
//        try {
//            // Prepara entrada
//            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(recordingLength * 4)
//                    .order(ByteOrder.nativeOrder());
//            inputBuffer.asFloatBuffer().put(floatInputBuffer);
//
//            // Prepara saída
//            float[][] output = new float[1][outputSize];
//
//            // Executa inferência
//            interpreter.run(inputBuffer, output);
//
//            // Processa saída
//            int[] predictedIds = new int[outputSize];
//            for (int i = 0; i < outputSize; i++) {
//                predictedIds[i] = (int) output[0][i];
//            }
//
//            return decode(predictedIds);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Erro na inferência TFLite", e);
//            return "Erro na transcrição";
//        }
//    }
//
//    // Método decode() mantido igual à versão original
//    private static final String[] LABELS = new String[]{
//            "<s>", "<pad>", "</s>", "<unk>", "|", "e", "t", "a", "o", "n", "i", "h", "s",
//            "r", "d", "l", "u", "m", "w", "c", "f", "g", "y", "p", "b", "v", "k", "'", "x",
//            "j", "q", "z"
//    };
//
//    private String decode(int[] tokens) {
//        // Implementação idêntica à versão original
//        StringBuilder sb = new StringBuilder();
//        String prev = "";
//
//        for (int index : tokens) {
//            if (index < 0 || index >= LABELS.length) continue;
//            String c = LABELS[index];
//            if (c.equals("<s>") || c.equals("<pad>") || c.equals("</s>") || c.equals("<unk>")) {
//                prev = "";
//                continue;
//            }
//            if (c.equals(prev)) continue;
//            sb.append(c);
//            prev = c;
//        }
//
//        return sb.toString().replace("|", " ").trim();
//    }
//}
//
