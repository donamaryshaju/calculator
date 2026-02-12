package com.example.calculators;

import android.provider.BaseColumns;

public final class UnitContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private UnitContract() {}

    /* Inner class that defines the table contents */
    public static class UnitEntry implements BaseColumns {
        public static final String TABLE_NAME = "units";
        public static final String COLUMN_NAME_UNIT_NAME = "unit_name";
        public static final String COLUMN_NAME_UNIT_TYPE = "unit_type";
        public static final String COLUMN_NAME_CONVERSION_FACTOR = "conversion_factor";
    }
}
