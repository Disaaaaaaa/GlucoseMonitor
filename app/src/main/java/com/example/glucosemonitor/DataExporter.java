package com.example.glucosemonitor;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DataExporter {
    private final Context context;
    private final SimpleDateFormat dateFormat;

    public DataExporter(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }

    public File exportMeasurements(List<DataStorage.Measurement> measurements) throws Exception {
        File exportDir = new File(context.getExternalFilesDir(null), "GlucoseMonitor");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new Exception("Не удалось создать директорию для экспорта");
        }

        String fileName = "measurements_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new java.util.Date()) + ".csv";
        File file = new File(exportDir, fileName);

        FileWriter writer = new FileWriter(file);
        writer.append("Дата,Значение (ммоль/л),Заметки\n");

        for (DataStorage.Measurement measurement : measurements) {
            writer.append(String.format("%s,%.1f,%s\n",
                dateFormat.format(measurement.getTimestamp()),
                measurement.getGlucoseLevel(),
                measurement.getNotes() != null ? measurement.getNotes() : ""
            ));
        }

        writer.close();
        return file;
    }

    public File exportCalibrationData(List<DataStorage.CalibrationData> calibrationData) throws Exception {
        File exportDir = new File(context.getExternalFilesDir(null), "GlucoseMonitor");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new Exception("Не удалось создать директорию для экспорта");
        }

        String fileName = "calibration_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new java.util.Date()) + ".csv";
        File file = new File(exportDir, fileName);

        FileWriter writer = new FileWriter(file);
        writer.append("Дата,Измеренное значение,Реальное значение,Яркость,Контраст\n");

        for (DataStorage.CalibrationData data : calibrationData) {
            writer.append(String.format("%s,%.1f,%.1f,%.1f,%.1f\n",
                dateFormat.format(data.getCalibrationDate()),
                data.getMeasuredValue(),
                data.getRealValue(),
                data.getBrightness(),
                data.getContrast()
            ));
        }

        writer.close();
        return file;
    }
} 