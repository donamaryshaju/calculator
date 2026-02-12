package com.example.calculators;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.LinkedHashMap;
import java.util.Map;

public class MeasurementActivity extends AppCompatActivity {

    // UI Groups
    private LinearLayout cardSelectionLayout;
    private LinearLayout conversionUILayout;

    // Conversion UI Elements
    private TextView inputDisplay;
    private TextView resultText;
    private Spinner fromUnitSpinner, toUnitSpinner;
    private Toolbar toolbar;
    private String currentInput = "";

    // Card Selection Elements
    private MaterialCardView cardLength, cardWeight, cardVolume;

    private CalculationDBHelper dbHelper;
    private String selectedType;

    // This single map will now be populated dynamically from the database
    private Map<String, Double> currentUnitMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        dbHelper = new CalculationDBHelper(this);
        initializeViews();
        // The hardcoded initializeUnits() method is now removed.
        setupToolbar();
        setupCardListeners();
        setupKeypad();
        setupUnitSelectionListeners();

        // Modern way to handle back button and back gestures
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        // Initial state: show cards, hide converter
        showCardSelectionView();
    }

    // Groups all findViewById calls
    private void initializeViews() {
        cardSelectionLayout = findViewById(R.id.layout_card_selection);
        conversionUILayout = findViewById(R.id.layout_conversion_ui);
        inputDisplay = findViewById(R.id.input_display);
        resultText = findViewById(R.id.text_view_result);
        fromUnitSpinner = findViewById(R.id.spinner_from_unit);
        toUnitSpinner = findViewById(R.id.spinner_to_unit);
        toolbar = findViewById(R.id.measurement_toolbar);
        cardLength = findViewById(R.id.card_length);
        cardWeight = findViewById(R.id.card_weight);
        cardVolume = findViewById(R.id.card_volume);
    }

    // This method now only sets up the initial toolbar state
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // We use the standard onOptionsItemSelected to handle toolbar back arrow clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackNavigation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupCardListeners() {
        cardLength.setOnClickListener(v -> {
            selectedType = "Length";
            loadUnitsFromDB(); // Load units for "Length" from the database
            showConversionView();
        });

        cardWeight.setOnClickListener(v -> {
            selectedType = "Weight";
            loadUnitsFromDB(); // Load units for "Weight" from the database
            showConversionView();
        });

        cardVolume.setOnClickListener(v -> {
            selectedType = "Volume";
            loadUnitsFromDB(); // Load units for "Volume" from the database
            showConversionView();
        });
    }

    // New method to fetch units for the selected type from the database
    private void loadUnitsFromDB() {
        currentUnitMap = dbHelper.getUnitsByType(selectedType);
        updateUnitSpinners();
    }

    private void setupKeypad() {
        int[] numberButtonIds = {
                R.id.keypad_btn_0, R.id.keypad_btn_1, R.id.keypad_btn_2, R.id.keypad_btn_3,
                R.id.keypad_btn_4, R.id.keypad_btn_5, R.id.keypad_btn_6, R.id.keypad_btn_7,
                R.id.keypad_btn_8, R.id.keypad_btn_9, R.id.keypad_btn_decimal
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(v -> {
                Button button = (Button) v;
                appendInput(button.getText().toString());
            });
        }

        findViewById(R.id.keypad_btn_backspace).setOnClickListener(v -> deleteLastCharacter());
    }

    private void setupUnitSelectionListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                convert();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        fromUnitSpinner.setOnItemSelectedListener(listener);
        toUnitSpinner.setOnItemSelectedListener(listener);
    }

    private void showCardSelectionView() {
        cardSelectionLayout.setVisibility(View.VISIBLE);
        conversionUILayout.setVisibility(View.GONE);

        // Update toolbar for the card selection screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Measurement");
        }
    }

    private void showConversionView() {
        cardSelectionLayout.setVisibility(View.GONE);
        conversionUILayout.setVisibility(View.VISIBLE);

        // Update toolbar for the conversion screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(selectedType + " Conversion");
        }

        currentInput = "0";
        updateDisplayAndConvert();
    }

    // Centralized back-navigation logic for toolbar and system back press
    private void handleBackNavigation() {
        if (conversionUILayout.getVisibility() == View.VISIBLE) {
            showCardSelectionView();
        } else {
            finish(); // Close the activity and go back to MainActivity
        }
    }

    private void appendInput(String value) {
        if (currentInput.equals("0") && !value.equals(".")) {
            currentInput = value;
        } else if (value.equals(".") && currentInput.contains(".")) {
            return; // Avoid multiple decimals
        } else {
            if (currentInput.length() < 12) {
                currentInput += value;
            }
        }
        updateDisplayAndConvert();
    }

    private void deleteLastCharacter() {
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                currentInput = "0";
            }
        }
        updateDisplayAndConvert();
    }

    private void updateDisplayAndConvert() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            inputDisplay.setText("0");
        } else {
            inputDisplay.setText(currentInput);
        }
        convert();
    }

    // This method now uses the dynamically populated currentUnitMap
    private void updateUnitSpinners() {
        if (currentUnitMap == null || currentUnitMap.isEmpty()) return;

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentUnitMap.keySet().toArray(new String[0]));
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromUnitSpinner.setAdapter(unitAdapter);
        toUnitSpinner.setAdapter(unitAdapter);
    }

    // This method now uses the dynamically populated currentUnitMap
    private void convert() {
        if (currentInput.isEmpty() || currentInput.equals("0") || currentInput.equals(".")) {
            resultText.setText("");
            return;
        }

        if (fromUnitSpinner.getSelectedItem() == null || toUnitSpinner.getSelectedItem() == null) {
            return; // Spinners are not ready yet
        }

        String fromUnit = fromUnitSpinner.getSelectedItem().toString();
        String toUnit = toUnitSpinner.getSelectedItem().toString();
        double inputValue = Double.parseDouble(currentInput);

        // Get factors from the map that was loaded from the database
        Double fromFactor = currentUnitMap.get(fromUnit);
        Double toFactor = currentUnitMap.get(toUnit);

        if (fromFactor == null || toFactor == null) return;

        double result = inputValue * fromFactor / toFactor;
        String resultString = String.format("= %.4f", result);
        resultText.setText(resultString);

        String expression = String.format("%s %s %s %s", currentInput, fromUnit, resultString, toUnit);
        saveCalculationToHistory(expression);
    }

    private void saveCalculationToHistory(String expression) {
        // Use a background thread for database operations to avoid blocking the UI
        new Thread(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION, expression);
            db.insert(CalculationHistoryContract.HistoryEntry.TABLE_NAME, null, values);
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close(); // Close the database helper when the activity is destroyed
    }
}
