package com.example.glucosemonitor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.data.CombinedData;

import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import android.widget.LinearLayout;
import android.view.Gravity;

import java.io.File;

import android.content.res.Configuration;
import android.content.res.Resources;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.PorterDuff;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            // Устанавливаем белый цвет для стрелки назад и гамбургера через тему
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.getNavigationIcon().setColorFilter(
                getResources().getColor(R.color.surface), 
                PorterDuff.Mode.SRC_ATOP
            );
        }

        // Инициализация Navigation Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Настройка кнопки-гамбургера
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, 
            drawerLayout, 
            toolbar,
            R.string.nav_drawer_open, 
            R.string.nav_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Обработка нажатий в меню
        navigationView.setNavigationItemSelectedListener(item -> {
            // Закрываем drawer перед выполнением действия
            drawerLayout.closeDrawer(GravityCompat.START);
            
            // Обработка выбора пункта меню
            switch (item.getItemId()) {
                case R.id.nav_home:
                    // Уже на главной
                    return true;
                case R.id.nav_measurement:
                    startCameraActivity();
                    return true;
                case R.id.nav_calibration:
                    startModelTraining();
                    return true;
                case R.id.nav_language:
                    switchLanguage();
                    return true;
                default:
                    return false;
            }
        });

        // Отмечаем текущий пункт меню
        navigationView.setCheckedItem(R.id.nav_home);

        checkPermissions();

        // Добавляем обработчик кнопки очистки истории
        Button btnClearHistory = findViewById(R.id.btnClearHistory);
        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton("OK", (dialog, which) -> {
                    DataStorage dataStorage = new DataStorage(this);
                    dataStorage.clearMeasurements();
                    Toast.makeText(this, R.string.clear_history_success, Toast.LENGTH_SHORT).show();
                    updateRecentMeasurements(); // Обновляем отображение после очистки
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        // Добавляем обработчик для кнопки нового измерения
        FloatingActionButton fabNewMeasurement = findViewById(R.id.fabNewMeasurement);
        fabNewMeasurement.setOnClickListener(v -> startCameraActivity());
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void startCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void startModelTraining() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_calibration, null);
        
        PreviewView previewView = dialogView.findViewById(R.id.calibrationPreview);
        TextView currentMeasurement = dialogView.findViewById(R.id.currentMeasurement);
        EditText glucoseInput = dialogView.findViewById(R.id.glucoseInput);
        TextView lastMeasurement = dialogView.findViewById(R.id.lastMeasurement);
        Button btnCapture = dialogView.findViewById(R.id.btnCapture);
        
        // Получаем последнее измерение из базы данных
        DataStorage dataStorage = new DataStorage(this);
        List<DataStorage.Measurement> measurements = dataStorage.getAllMeasurements();
        
        if (measurements.isEmpty()) {
            Toast.makeText(this, R.string.no_measurements, Toast.LENGTH_LONG).show();
            return;
        }
        
        DataStorage.Measurement lastMeasure = measurements.get(0);
        lastMeasurement.setText(getString(R.string.last_calibration_measurement, 
            lastMeasure.getGlucoseLevel()));

        AlertDialog dialog = builder.setTitle(R.string.calibration_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create();

        // Настраиваем камеру
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        MLModel mlModel = new MLModel(this);
        AtomicReference<Float> currentValue = new AtomicReference<>(0f);
        AtomicReference<Float> capturedValue = new AtomicReference<>(0f);
        AtomicBoolean isCapturing = new AtomicBoolean(true);

        btnCapture.setOnClickListener(v -> {
            capturedValue.set(currentValue.get());
            isCapturing.set(false);
            currentMeasurement.setText(getString(R.string.value_captured, capturedValue.get()));
            btnCapture.setText(R.string.start_capture);
            btnCapture.setOnClickListener(v2 -> {
                isCapturing.set(true);
                btnCapture.setText(R.string.capture_value);
                btnCapture.setOnClickListener(v3 -> {
                    capturedValue.set(currentValue.get());
                    isCapturing.set(false);
                    currentMeasurement.setText(getString(R.string.value_captured, capturedValue.get()));
                    btnCapture.setText(R.string.start_capture);
                });
            });
        });

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    try {
                        if (isCapturing.get()) {
                            float[] features = ImageProcessing.processImage(image);
                            float prediction = mlModel.mockPrediction(features);
                            currentValue.set(prediction);
                            
                            runOnUiThread(() -> {
                                currentMeasurement.setText(getString(R.string.current_value, prediction));
                            });
                        }
                    } finally {
                        image.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка камеры: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

        // При сохранении используем захваченное значение
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                try {
                    String input = glucoseInput.getText().toString();
                    if (input.isEmpty()) {
                        Toast.makeText(this, R.string.enter_value, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    float realValue = Float.parseFloat(input);
                    if (realValue < 0 || realValue > 30) {
                        Toast.makeText(this, R.string.invalid_value, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dataStorage.saveCalibrationData(
                        capturedValue.get(),
                        realValue,
                        0, // brightness
                        0  // contrast
                    );

                    Toast.makeText(this, R.string.calibration_saved, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.invalid_value, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showCalibrationStats() {
        DataStorage dataStorage = new DataStorage(this);
        List<DataStorage.CalibrationData> calibrationData = dataStorage.getAllCalibrationData();
        
        if (calibrationData.isEmpty()) {
            Toast.makeText(this, R.string.no_calibration_data, Toast.LENGTH_SHORT).show();
            return;
        }

        CalibrationStats stats = new CalibrationStats(calibrationData);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_calibration_stats, null);
        TextView statsText = dialogView.findViewById(R.id.statsText);
        ScatterChart chart = dialogView.findViewById(R.id.correlationChart);
        
        // Настраиваем текстовую статистику
        String statsMessage = String.format(getString(R.string.calibration_stats_format),
            stats.getCalibrationCount(),
            stats.getAccuracy(),
            stats.getMeanError(),
            stats.getMaxError(),
            stats.getStandardDeviation()
        );
        statsText.setText(statsMessage);

        // Настраиваем график
        ArrayList<Entry> entries = new ArrayList<>();
        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;
        
        for (DataStorage.CalibrationData data : calibrationData) {
            entries.add(new Entry(data.getMeasuredValue(), data.getRealValue()));
            minValue = Math.min(minValue, Math.min(data.getMeasuredValue(), data.getRealValue()));
            maxValue = Math.max(maxValue, Math.max(data.getMeasuredValue(), data.getRealValue()));
        }

        // Добавляем идеальную линию (x=y)
        ArrayList<Entry> idealLine = new ArrayList<>();
        idealLine.add(new Entry(minValue, minValue));
        idealLine.add(new Entry(maxValue, maxValue));

        // Настраиваем наборы данных
        ScatterDataSet scatterDataSet = new ScatterDataSet(entries, getString(R.string.measurements));
        scatterDataSet.setColor(Color.BLUE);
        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        scatterDataSet.setScatterShapeSize(12f);

        LineDataSet lineDataSet = new LineDataSet(idealLine, getString(R.string.ideal_correlation));
        lineDataSet.setColor(Color.RED);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);

        // Объединяем данные
        CombinedData combinedData = new CombinedData();
        combinedData.setData(new ScatterData(scatterDataSet));
        combinedData.setData(new LineData(lineDataSet));

        // Настраиваем внешний вид графика
        chart.setData(combinedData.getScatterData());
        chart.getDescription().setText(getString(R.string.measurement_correlation));

        // Настраиваем оси
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMinimum(minValue - 0.5f);
        xAxis.setAxisMaximum(maxValue + 0.5f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(minValue - 0.5f);
        leftAxis.setAxisMaximum(maxValue + 0.5f);

        // Отключаем правую ось
        chart.getAxisRight().setEnabled(false);

        // Добавляем описание осей в виде текста под/слева от графика
        TextView xAxisLabel = new TextView(this);
        xAxisLabel.setText("Измеренные значения (ммоль/л)");
        xAxisLabel.setGravity(Gravity.CENTER);
        xAxisLabel.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ((LinearLayout) chart.getParent()).addView(xAxisLabel);

        // Настраиваем легенду
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        // Обновляем график
        chart.invalidate();

        // Показываем диалог
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Статистика")
               .setView(dialogView)
               .setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Настраиваем размер диалога
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void showExportDialog() {
        String[] options = {"Экспорт измерений", "Экспорт калибровочных данных"};
        
        new AlertDialog.Builder(this)
            .setTitle("Выберите тип экспорта")
            .setItems(options, (dialog, which) -> {
                try {
                    DataStorage dataStorage = new DataStorage(this);
                    DataExporter exporter = new DataExporter(this);
                    File exportedFile;

                    if (which == 0) {
                        List<DataStorage.Measurement> measurements = dataStorage.getAllMeasurements();
                        if (measurements.isEmpty()) {
                            Toast.makeText(this, "Нет данных для экспорта", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        exportedFile = exporter.exportMeasurements(measurements);
                    } else {
                        List<DataStorage.CalibrationData> calibrationData = 
                            dataStorage.getAllCalibrationData();
                        if (calibrationData.isEmpty()) {
                            Toast.makeText(this, "Нет калибровочных данных для экспорта", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        exportedFile = exporter.exportCalibrationData(calibrationData);
                    }

                    Toast.makeText(this, 
                        "Данные экспортированы в " + exportedFile.getAbsolutePath(), 
                        Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(this, 
                        "Ошибка экспорта: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void switchLanguage() {
        String currentLang = getResources().getConfiguration().locale.getLanguage();
        Locale newLocale = currentLang.equals("ru") ? 
            new Locale("en") : new Locale("ru");
        
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(newLocale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Пересоздаем активность для применения изменений
        Intent refresh = new Intent(this, MainActivity.class);
        refresh.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(refresh);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "Все разрешения получены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Необходимы все разрешения для работы приложения", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Если drawer открыт, закрываем его
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Программное открытие/закрытие drawer
    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRecentMeasurements();
    }

    private void updateRecentMeasurements() {
        DataStorage dataStorage = new DataStorage(this);
        List<DataStorage.Measurement> measurements = dataStorage.getRecentMeasurements(10); // последние 10 измерений

        // Обновляем график
        LineChart chart = findViewById(R.id.recentMeasurementsChart);
        updateChart(chart, measurements);

        // Обновляем текст последнего измерения
        TextView lastMeasurementText = findViewById(R.id.lastMeasurementText);
        if (!measurements.isEmpty()) {
            DataStorage.Measurement last = measurements.get(measurements.size() - 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = dateFormat.format(last.getTimestamp());
            String measurementStr = String.format(Locale.getDefault(), "%.1f ммоль/л", last.getGlucoseLevel());
            lastMeasurementText.setText(getString(R.string.last_measurement, dateStr, measurementStr));
        } else {
            lastMeasurementText.setText(R.string.no_measurements);
        }
    }

    private void updateChart(LineChart chart, List<DataStorage.Measurement> measurements) {
        if (measurements.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_measurements));
            chart.invalidate();
            return;
        }

        // Подготовка данных для графика
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;

        // Добавляем данные в обратном порядке (старые слева, новые справа)
        for (int i = measurements.size() - 1; i >= 0; i--) {
            DataStorage.Measurement m = measurements.get(i);
            entries.add(new Entry(measurements.size() - 1 - i, m.getGlucoseLevel()));
            labels.add(dateFormat.format(m.getTimestamp()));
            minValue = Math.min(minValue, m.getGlucoseLevel());
            maxValue = Math.max(maxValue, m.getGlucoseLevel());
        }

        // Настраиваем набор данных
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(getResources().getColor(R.color.chart_line));
        dataSet.setCircleColor(getResources().getColor(R.color.chart_line));
        dataSet.setValueTextColor(getResources().getColor(R.color.text_primary));
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        // Добавляем линии нормы
        ArrayList<Entry> normalLowLine = new ArrayList<>();
        ArrayList<Entry> normalHighLine = new ArrayList<>();
        normalLowLine.add(new Entry(0, 3.9f));
        normalLowLine.add(new Entry(measurements.size() - 1, 3.9f));
        normalHighLine.add(new Entry(0, 6.1f));
        normalHighLine.add(new Entry(measurements.size() - 1, 6.1f));

        LineDataSet lowLineDataSet = new LineDataSet(normalLowLine, getString(R.string.lower_limit));
        lowLineDataSet.setColor(getResources().getColor(R.color.chart_limit_line));
        lowLineDataSet.setDrawCircles(false);
        lowLineDataSet.setLineWidth(1f);
        lowLineDataSet.enableDashedLine(10f, 5f, 0f);
        lowLineDataSet.setDrawValues(false);

        LineDataSet highLineDataSet = new LineDataSet(normalHighLine, getString(R.string.upper_limit));
        highLineDataSet.setColor(getResources().getColor(R.color.chart_limit_line));
        highLineDataSet.setDrawCircles(false);
        highLineDataSet.setLineWidth(1f);
        highLineDataSet.enableDashedLine(10f, 5f, 0f);
        highLineDataSet.setDrawValues(false);

        // Настраиваем график
        LineData lineData = new LineData(dataSet, lowLineDataSet, highLineDataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        
        // Настраиваем оси
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45f);
        xAxis.setGridColor(getResources().getColor(R.color.chart_grid));
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(Math.max(0, minValue - 1));
        leftAxis.setAxisMaximum(maxValue + 1);
        leftAxis.setGridColor(getResources().getColor(R.color.chart_grid));
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawGridLines(true);
        
        chart.getAxisRight().setEnabled(false);
        
        // Настраиваем легенду
        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setTextColor(getResources().getColor(R.color.text_secondary));
        legend.setTextSize(12f);

        // Настройки графика
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        
        // Отступы
        chart.setExtraOffsets(5f, 10f, 5f, 10f);

        // Анимация и обновление
        chart.animateX(1000);
        chart.invalidate();
    }
} 