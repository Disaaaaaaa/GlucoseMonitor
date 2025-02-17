package com.example.glucosemonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataStorage extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "GlucoseDB";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_MEASUREMENTS = "measurements";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_GLUCOSE_LEVEL = "glucose_level";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_NOTES = "notes";

    private static final String TABLE_CALIBRATION = "calibration";
    private static final String COLUMN_MEASURED_VALUE = "measured_value";
    private static final String COLUMN_REAL_VALUE = "real_value";
    private static final String COLUMN_BRIGHTNESS = "brightness";
    private static final String COLUMN_CONTRAST = "contrast";
    private static final String COLUMN_CALIBRATION_DATE = "calibration_date";

    public DataStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMeasurementsTable = "CREATE TABLE " + TABLE_MEASUREMENTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_GLUCOSE_LEVEL + " REAL NOT NULL, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                COLUMN_NOTES + " TEXT" +
                ")";
        
        String createCalibrationTable = "CREATE TABLE " + TABLE_CALIBRATION + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MEASURED_VALUE + " REAL NOT NULL, " +
                COLUMN_REAL_VALUE + " REAL NOT NULL, " +
                COLUMN_BRIGHTNESS + " REAL NOT NULL, " +
                COLUMN_CONTRAST + " REAL NOT NULL, " +
                COLUMN_CALIBRATION_DATE + " INTEGER NOT NULL" +
                ")";

        db.execSQL(createMeasurementsTable);
        db.execSQL(createCalibrationTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createCalibrationTable = "CREATE TABLE " + TABLE_CALIBRATION + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MEASURED_VALUE + " REAL NOT NULL, " +
                    COLUMN_REAL_VALUE + " REAL NOT NULL, " +
                    COLUMN_BRIGHTNESS + " REAL NOT NULL, " +
                    COLUMN_CONTRAST + " REAL NOT NULL, " +
                    COLUMN_CALIBRATION_DATE + " INTEGER NOT NULL" +
                    ")";
            db.execSQL(createCalibrationTable);
        }
    }

    public long saveMeasurement(float glucoseLevel, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_GLUCOSE_LEVEL, glucoseLevel);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_NOTES, notes);

        return db.insert(TABLE_MEASUREMENTS, null, values);
    }

    public List<Measurement> getAllMeasurements() {
        List<Measurement> measurements = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_MEASUREMENTS + 
                      " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Measurement measurement = new Measurement(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_GLUCOSE_LEVEL)),
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NOTES))
                );
                measurements.add(measurement);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return measurements;
    }

    public long saveCalibrationData(float measuredValue, float realValue, float brightness, float contrast) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_MEASURED_VALUE, measuredValue);
        values.put(COLUMN_REAL_VALUE, realValue);
        values.put(COLUMN_BRIGHTNESS, brightness);
        values.put(COLUMN_CONTRAST, contrast);
        values.put(COLUMN_CALIBRATION_DATE, System.currentTimeMillis());

        return db.insert(TABLE_CALIBRATION, null, values);
    }

    public List<CalibrationData> getAllCalibrationData() {
        List<CalibrationData> calibrationDataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CALIBRATION + 
                           " ORDER BY " + COLUMN_CALIBRATION_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                CalibrationData data = new CalibrationData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_MEASURED_VALUE)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_REAL_VALUE)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_BRIGHTNESS)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_CONTRAST)),
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CALIBRATION_DATE)))
                );
                calibrationDataList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return calibrationDataList;
    }

    public List<CalibrationData> getLastCalibrationData(int limit) {
        List<CalibrationData> calibrationDataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CALIBRATION + 
                           " ORDER BY " + COLUMN_CALIBRATION_DATE + " DESC LIMIT " + limit;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                CalibrationData data = new CalibrationData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_MEASURED_VALUE)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_REAL_VALUE)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_BRIGHTNESS)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_CONTRAST)),
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CALIBRATION_DATE)))
                );
                calibrationDataList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return calibrationDataList;
    }

    public List<Measurement> getRecentMeasurements(int count) {
        List<Measurement> measurements = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_MEASUREMENTS + 
                      " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(count)});

        if (cursor.moveToFirst()) {
            do {
                Measurement measurement = new Measurement(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_GLUCOSE_LEVEL)),
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NOTES))
                );
                measurements.add(measurement);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return measurements;
    }

    public void clearMeasurements() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEASUREMENTS, null, null);
        db.close();
    }

    public static class Measurement {
        private final long id;
        private final float glucoseLevel;
        private final Date timestamp;
        private final String notes;

        public Measurement(long id, float glucoseLevel, Date timestamp, String notes) {
            this.id = id;
            this.glucoseLevel = glucoseLevel;
            this.timestamp = timestamp;
            this.notes = notes;
        }

        public long getId() { return id; }
        public float getGlucoseLevel() { return glucoseLevel; }
        public Date getTimestamp() { return timestamp; }
        public String getNotes() { return notes; }
    }

    public static class CalibrationData {
        private final long id;
        private final float measuredValue;
        private final float realValue;
        private final float brightness;
        private final float contrast;
        private final Date calibrationDate;

        public CalibrationData(long id, float measuredValue, float realValue, 
                             float brightness, float contrast, Date calibrationDate) {
            this.id = id;
            this.measuredValue = measuredValue;
            this.realValue = realValue;
            this.brightness = brightness;
            this.contrast = contrast;
            this.calibrationDate = calibrationDate;
        }

        public long getId() { return id; }
        public float getMeasuredValue() { return measuredValue; }
        public float getRealValue() { return realValue; }
        public float getBrightness() { return brightness; }
        public float getContrast() { return contrast; }
        public Date getCalibrationDate() { return calibrationDate; }
    }
} 