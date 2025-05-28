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

    public ModelHelper(Context context, String modelFileName) {
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
        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(floatInputBuffer.length);
        inTensorBuffer.put(floatInputBuffer);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, floatInputBuffer.length});

        return module.forward(IValue.from(inTensor)).toStr();
    }
}
