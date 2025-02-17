package com.example.glucosemonitor;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import android.widget.Toast;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.CameraSelector;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import android.widget.ProgressBar;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private MLModel mlModel;
    private DataStorage dataStorage;
    private Button btnMeasure;
    private float currentGlucoseValue = 0f;
    private int currentQuality = 0;
    private TextView qualityIndicator;
    private ProgressBar qualityProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.viewFinder);
        qualityIndicator = findViewById(R.id.qualityIndicator);
        qualityProgress = findViewById(R.id.qualityProgress);
        
        mlModel = new MLModel(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        dataStorage = new DataStorage(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка запуска камеры: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

        btnMeasure = findViewById(R.id.btnMeasure);
        btnMeasure.setOnClickListener(v -> showMeasurementResults());
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            try {
                float[] features = ImageProcessing.processImage(image);
                currentGlucoseValue = mlModel.mockPrediction(features);
                currentQuality = calculateQuality(features);
                
                runOnUiThread(() -> {
                    qualityIndicator.setText(getString(R.string.quality_indicator, currentQuality));
                    qualityProgress.setProgress(currentQuality);
                    btnMeasure.setEnabled(currentQuality >= 70);
                });
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка обработки: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            } finally {
                image.close();
            }
        });

        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
            preview, imageAnalysis);
    }

    private void showMeasurementResults() {
        Intent intent = new Intent(this, MeasurementResultsActivity.class);
        intent.putExtra(MeasurementResultsActivity.EXTRA_GLUCOSE_VALUE, currentGlucoseValue);
        intent.putExtra(MeasurementResultsActivity.EXTRA_QUALITY, currentQuality);
        startActivity(intent);
    }

    private int calculateQuality(float[] features) {
        return 75;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mlModel != null) {
            mlModel.close();
        }
    }
} 