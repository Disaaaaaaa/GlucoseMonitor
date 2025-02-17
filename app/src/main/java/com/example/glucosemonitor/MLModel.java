package com.example.glucosemonitor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class MLModel {
    private static final String MODEL_PATH = "glucose_model.tflite";
    private Interpreter tflite;
    private final Context context;
    private boolean isModelLoaded = false;
    private final CalibrationModel calibrationModel;

    public MLModel(Context context) {
        this.context = context;
        this.calibrationModel = new CalibrationModel(new DataStorage(context));
        loadModel();
    }

    private void loadModel() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(loadModelFile(), options);
            isModelLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        String modelPath = MODEL_PATH;
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float predictGlucoseLevel(float[] features) {
        if (!isModelLoaded) {
            throw new IllegalStateException("Model not loaded");
        }

        // Подготовка входных данных
        float[][] inputArray = new float[1][features.length];
        inputArray[0] = features;

        // Подготовка выходных данных
        float[][] outputArray = new float[1][1];

        // Выполнение предсказания
        tflite.run(inputArray, outputArray);

        return outputArray[0][0];
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            isModelLoaded = false;
        }
    }

    // Временная реализация для тестирования без модели
    public float mockPrediction(float[] features) {
        // Базовое значение глюкозы (нормальный уровень)
        float baseGlucose = 5.5f;
        
        // Используем яркость и контраст для модификации базового значения
        float brightnessEffect = (features[0] / 128.0f - 1.0f) * 2.0f;
        float contrastEffect = (features[1] / 50.0f) * 1.5f;
        
        // Добавляем небольшую случайную вариацию
        float randomVariation = (float) ((Math.random() - 0.5) * 0.8);
        
        // Вычисляем сырое значение
        float rawPrediction = baseGlucose + brightnessEffect + contrastEffect + randomVariation;
        
        // Ограничиваем значения реалистичным диапазоном
        rawPrediction = Math.max(3.3f, Math.min(10.0f, rawPrediction));
        
        // Применяем калибровку
        float calibratedPrediction = calibrationModel.adjustPrediction(
            rawPrediction, features[0], features[1]
        );
        
        // Округляем до одного знака после запятой
        return Math.round(calibratedPrediction * 10.0f) / 10.0f;
    }
} 