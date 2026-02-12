package com.example.calculators;


import android.provider.BaseColumns;

public final class CalculationHistoryContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private CalculationHistoryContract() {}

    /* Inner class that defines the table contents */
    public static class HistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "calculation_history";
        public static final String COLUMN_NAME_EXPRESSION = "expression";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }
}
