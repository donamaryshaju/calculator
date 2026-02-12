package com.example.calculators;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class CalculationDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3; // <-- CORRECTED: Incremented from 2 to 3 to force an upgrade
    public static final String DATABASE_NAME = "CalculatorHistory.db";

    // --- SQL for History Table (Existing) ---
    private static final String SQL_CREATE_HISTORY_TABLE =
            "CREATE TABLE " + CalculationHistoryContract.HistoryEntry.TABLE_NAME + " (" +
                    CalculationHistoryContract.HistoryEntry._ID + " INTEGER PRIMARY KEY," +
                    CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION + " TEXT," +
                    CalculationHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_HISTORY_TABLE =
            "DROP TABLE IF EXISTS " + CalculationHistoryContract.HistoryEntry.TABLE_NAME;

    // --- SQL for Units Table ---
    private static final String SQL_CREATE_UNITS_TABLE =
            "CREATE TABLE " + UnitContract.UnitEntry.TABLE_NAME + " (" +
                    UnitContract.UnitEntry._ID + " INTEGER PRIMARY KEY," +
                    UnitContract.UnitEntry.COLUMN_NAME_UNIT_NAME + " TEXT NOT NULL," +
                    UnitContract.UnitEntry.COLUMN_NAME_UNIT_TYPE + " TEXT NOT NULL," +
                    UnitContract.UnitEntry.COLUMN_NAME_CONVERSION_FACTOR + " REAL NOT NULL)";

    private static final String SQL_DELETE_UNITS_TABLE =
            "DROP TABLE IF EXISTS " + UnitContract.UnitEntry.TABLE_NAME;


    public CalculationDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create both tables
        db.execSQL(SQL_CREATE_HISTORY_TABLE);
        db.execSQL(SQL_CREATE_UNITS_TABLE);

        // Populate the units table with default values
        populateInitialUnits(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache, so its upgrade policy is to simply discard the data and start over
        db.execSQL(SQL_DELETE_HISTORY_TABLE);
        db.execSQL(SQL_DELETE_UNITS_TABLE);
        onCreate(db);
    }

    // --- This method is already correct and includes the currency data ---
    private void populateInitialUnits(SQLiteDatabase db) {
        // Length Units (Base: Meter)
        addUnit(db, "Millimeter (mm)", "Length", 0.001);
        addUnit(db, "Centimeter (cm)", "Length", 0.01);
        addUnit(db, "Inch (in)", "Length", 0.0254);
        addUnit(db, "Foot (ft)", "Length", 0.3048);
        addUnit(db, "Meter (m)", "Length", 1.0);
        addUnit(db, "Kilometer (km)", "Length", 1000.0);

        // Weight Units (Base: Gram)
        addUnit(db, "Gram (g)", "Weight", 1.0);
        addUnit(db, "Ounce (oz)", "Weight", 28.3495);
        addUnit(db, "Pound (lb)", "Weight", 453.592);
        addUnit(db, "Kilogram (kg)", "Weight", 1000.0);
        addUnit(db, "Tonne", "Weight", 1000000.0);

        // Volume Units (Base: Milliliter)
        addUnit(db, "Milliliter (mL)", "Volume", 1.0);
        addUnit(db, "Liter (L)", "Volume", 1000.0);

        // --- Currency "Units" (Base: USD) ---
        // The "factor" is how many USD 1 unit of this currency is worth.
        addUnit(db, "USD", "Currency", 1.0);      // Base currency (1 USD = 1 USD)
        addUnit(db, "EUR", "Currency", 1.08);     // 1 EUR = 1.08 USD
        addUnit(db, "JPY", "Currency", 0.0067);   // 1 JPY = 0.0067 USD
        addUnit(db, "GBP", "Currency", 1.27);     // 1 GBP = 1.27 USD
        addUnit(db, "INR", "Currency", 0.012);    // 1 INR = 0.012 USD
    }

    // Helper method to insert a single unit
    private void addUnit(SQLiteDatabase db, String name, String type, double factor) {
        ContentValues values = new ContentValues();
        values.put(UnitContract.UnitEntry.COLUMN_NAME_UNIT_NAME, name);
        values.put(UnitContract.UnitEntry.COLUMN_NAME_UNIT_TYPE, type);
        values.put(UnitContract.UnitEntry.COLUMN_NAME_CONVERSION_FACTOR, factor);
        db.insert(UnitContract.UnitEntry.TABLE_NAME, null, values);
    }

    // Method to fetch all units of a specific type from the database
    public Map<String, Double> getUnitsByType(String type) {
        Map<String, Double> units = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                UnitContract.UnitEntry.TABLE_NAME,   // The table to query
                new String[]{UnitContract.UnitEntry.COLUMN_NAME_UNIT_NAME, UnitContract.UnitEntry.COLUMN_NAME_CONVERSION_FACTOR}, // The columns to return
                UnitContract.UnitEntry.COLUMN_NAME_UNIT_TYPE + "=?", // The columns for the WHERE clause
                new String[]{type},                          // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(UnitContract.UnitEntry.COLUMN_NAME_UNIT_NAME));
            double factor = cursor.getDouble(cursor.getColumnIndexOrThrow(UnitContract.UnitEntry.COLUMN_NAME_CONVERSION_FACTOR));
            units.put(name, factor);
        }
        cursor.close();
        return units;
    }
}
