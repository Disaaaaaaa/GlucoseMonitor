package com.example.glucosemonitor;

import java.util.List;

public class CalibrationModel {
    private static final int CALIBRATION_WINDOW = 10; // Используем последние 10 калибровок
    private static final float MIN_SIMILARITY = 0.8f; // Минимальное сходство для использования калибровки

    private final DataStorage dataStorage;

    public CalibrationModel(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public float adjustPrediction(float rawPrediction, float brightness, float contrast) {
        List<DataStorage.CalibrationData> calibrationData = 
            dataStorage.getLastCalibrationData(CALIBRATION_WINDOW);

        if (calibrationData.isEmpty()) {
            return rawPrediction; // Нет данных для калибровки
        }

        float totalWeight = 0;
        float weightedSum = 0;

        for (DataStorage.CalibrationData data : calibrationData) {
            // Вычисляем схожесть текущих параметров с калибровочными
            float similarity = calculateSimilarity(
                brightness, contrast,
                data.getBrightness(), data.getContrast()
            );

            if (similarity >= MIN_SIMILARITY) {
                // Вычисляем коэффициент коррекции для этой калибровки
                float correctionFactor = data.getRealValue() / data.getMeasuredValue();
                float weight = similarity;

                weightedSum += correctionFactor * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight > 0) {
            // Применяем взвешенную коррекцию
            float averageCorrectionFactor = weightedSum / totalWeight;
            return rawPrediction * averageCorrectionFactor;
        }

        return rawPrediction; // Нет подходящих калибровок
    }

    private float calculateSimilarity(float brightness1, float contrast1, 
                                    float brightness2, float contrast2) {
        // Нормализуем значения
        float maxBrightness = Math.max(brightness1, brightness2);
        float maxContrast = Math.max(contrast1, contrast2);

        float normBrightness1 = brightness1 / maxBrightness;
        float normBrightness2 = brightness2 / maxBrightness;
        float normContrast1 = contrast1 / maxContrast;
        float normContrast2 = contrast2 / maxContrast;

        // Вычисляем евклидово расстояние в нормализованном пространстве
        float brightnessDiff = normBrightness1 - normBrightness2;
        float contrastDiff = normContrast1 - normContrast2;
        float distance = (float) Math.sqrt(brightnessDiff * brightnessDiff + 
                                         contrastDiff * contrastDiff);

        // Преобразуем расстояние в меру сходства (1 - нормализованное расстояние)
        return Math.max(0, 1 - distance);
    }
} 