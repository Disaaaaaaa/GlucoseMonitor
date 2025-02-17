package com.example.glucosemonitor;

import java.util.List;

public class CalibrationStats {
    private final float meanError;
    private final float maxError;
    private final float standardDeviation;
    private final int calibrationCount;
    private final float accuracy;

    public CalibrationStats(List<DataStorage.CalibrationData> calibrationData) {
        this.calibrationCount = calibrationData.size();
        
        if (calibrationCount == 0) {
            this.meanError = 0;
            this.maxError = 0;
            this.standardDeviation = 0;
            this.accuracy = 0;
            return;
        }

        float sumError = 0;
        float maxErr = 0;
        float sumSquaredError = 0;
        int accurateCount = 0;

        for (DataStorage.CalibrationData data : calibrationData) {
            float error = Math.abs(data.getRealValue() - data.getMeasuredValue());
            sumError += error;
            maxErr = Math.max(maxErr, error);
            sumSquaredError += error * error;
            
            // Считаем измерение точным, если ошибка меньше 15%
            if (error <= data.getRealValue() * 0.15) {
                accurateCount++;
            }
        }

        this.meanError = sumError / calibrationCount;
        this.maxError = maxErr;
        this.standardDeviation = (float) Math.sqrt(
            (sumSquaredError / calibrationCount) - (meanError * meanError)
        );
        this.accuracy = (float) accurateCount / calibrationCount * 100;
    }

    public float getMeanError() { return meanError; }
    public float getMaxError() { return maxError; }
    public float getStandardDeviation() { return standardDeviation; }
    public int getCalibrationCount() { return calibrationCount; }
    public float getAccuracy() { return accuracy; }
} 