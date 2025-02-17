package com.example.glucosemonitor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MeasurementResultsActivity extends AppCompatActivity {
    public static final String EXTRA_GLUCOSE_VALUE = "glucose_value";
    public static final String EXTRA_QUALITY = "quality";
    
    private static final float GLUCOSE_LOW = 3.9f;
    private static final float GLUCOSE_HIGH = 6.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_results);

        float glucoseValue = getIntent().getFloatExtra(EXTRA_GLUCOSE_VALUE, 0f);
        int quality = getIntent().getIntExtra(EXTRA_QUALITY, 0);

        TextView glucoseValueText = findViewById(R.id.glucoseValue);
        TextView glucoseStateText = findViewById(R.id.glucoseState);
        TextView glucoseTrendText = findViewById(R.id.glucoseTrend);
        TextView qualityText = findViewById(R.id.measurementQuality);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnRetry = findViewById(R.id.btnRetry);
        Button btnHome = findViewById(R.id.btnHome);

        // Устанавливаем значение глюкозы
        glucoseValueText.setText(getString(R.string.glucose_value, glucoseValue));

        // Определяем и отображаем состояние
        setGlucoseState(glucoseStateText, glucoseValue);

        // Определяем и отображаем тренд
        DataStorage dataStorage = new DataStorage(this);
        List<DataStorage.Measurement> recentMeasurements = dataStorage.getRecentMeasurements(3);
        setGlucoseTrend(glucoseTrendText, glucoseValue, recentMeasurements);

        // Отображаем качество измерения
        qualityText.setText(getString(R.string.quality_indicator, quality));

        btnHome.setOnClickListener(v -> {
            // Очищаем стек активностей и возвращаемся на главный экран
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnSave.setOnClickListener(v -> {
            dataStorage.saveMeasurement(glucoseValue, "Автоматическое измерение");
            // После сохранения возвращаемся на главный экран
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnRetry.setOnClickListener(v -> {
            // Возвращаемся к измерению
            finish();
        });
    }

    private void setGlucoseState(TextView stateText, float glucoseValue) {
        String stateMessage;
        int textColor;

        if (glucoseValue < GLUCOSE_LOW) {
            stateMessage = getString(R.string.glucose_state_low);
            textColor = getResources().getColor(R.color.error);
        } else if (glucoseValue > GLUCOSE_HIGH) {
            stateMessage = getString(R.string.glucose_state_high);
            textColor = getResources().getColor(R.color.error);
        } else {
            stateMessage = getString(R.string.glucose_state_normal);
            textColor = getResources().getColor(R.color.success);
        }

        stateText.setText(stateMessage);
        stateText.setTextColor(textColor);
    }

    private void setGlucoseTrend(TextView trendText, float currentValue, 
                                List<DataStorage.Measurement> recentMeasurements) {
        String trend;
        
        if (recentMeasurements.size() < 2) {
            trendText.setVisibility(TextView.GONE);
            return;
        }

        float previousValue = recentMeasurements.get(0).getGlucoseLevel();
        float difference = currentValue - previousValue;
        
        if (Math.abs(difference) < 0.5f) {
            trend = getString(R.string.trend_stable);
        } else if (difference > 0) {
            trend = getString(R.string.trend_rising);
        } else {
            trend = getString(R.string.trend_falling);
        }

        trendText.setText(getString(R.string.glucose_trend, trend));
    }
} 