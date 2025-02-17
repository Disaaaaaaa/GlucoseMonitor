package com.example.glucosemonitor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class ImageProcessing {
    
    public static float[] processImage(ImageProxy image) {
        // Конвертируем изображение в градации серого
        Bitmap grayscaleImage = convertToGrayscale(image);
        
        // Применяем фильтрацию для уменьшения шума
        Bitmap filteredImage = applyNoiseReduction(grayscaleImage);
        
        // Извлекаем характеристики изображения
        return extractFeatures(filteredImage);
    }
    
    private static Bitmap convertToGrayscale(ImageProxy image) {
        Image.Plane[] planes = image.getImage().getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = data[y * width + x] & 0xff;
                bitmap.setPixel(x, y, Color.rgb(pixel, pixel, pixel));
            }
        }
        
        return bitmap;
    }
    
    private static Bitmap applyNoiseReduction(Bitmap input) {
        // Применяем медианный фильтр для уменьшения шума
        int width = input.getWidth();
        int height = input.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, input.getConfig());
        
        int[] pixels = new int[9];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Получаем 9 пикселей вокруг текущей точки
                int index = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        pixels[index++] = input.getPixel(x + i, y + j);
                    }
                }
                // Находим медианное значение
                output.setPixel(x, y, findMedian(pixels));
            }
        }
        
        return output;
    }
    
    private static int findMedian(int[] pixels) {
        // Сортируем значения яркости
        int[] values = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            values[i] = Color.red(pixels[i]); // Для grayscale R=G=B
        }
        java.util.Arrays.sort(values);
        
        // Возвращаем медианное значение
        int median = values[values.length / 2];
        return Color.rgb(median, median, median);
    }
    
    private static float[] extractFeatures(Bitmap image) {
        // Извлекаем характеристики изображения для анализа
        // Например: средняя яркость, контраст, и т.д.
        float[] features = new float[5];
        
        // Средняя яркость
        features[0] = calculateAverageBrightness(image);
        
        // Контраст
        features[1] = calculateContrast(image);
        
        // TODO: Добавить другие характеристики
        
        return features;
    }
    
    private static float calculateAverageBrightness(Bitmap image) {
        long sum = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                sum += Color.red(pixel); // Для grayscale R=G=B
            }
        }
        
        return (float) sum / (width * height);
    }
    
    private static float calculateContrast(Bitmap image) {
        float avgBrightness = calculateAverageBrightness(image);
        float sumSquareDiff = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                float diff = Color.red(pixel) - avgBrightness;
                sumSquareDiff += diff * diff;
            }
        }
        
        return (float) Math.sqrt(sumSquareDiff / (width * height));
    }
} 