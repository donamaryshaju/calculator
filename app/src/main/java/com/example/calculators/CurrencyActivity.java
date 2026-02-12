package com.example.calculators;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CurrencyActivity extends AppCompatActivity {

    private TextView inputDisplay;
    private TextView resultText;
    private Spinner fromCurrencySpinner, toCurrencySpinner;
    private Toolbar toolbar;
    private String currentInput = "";

    private CalculationDBHelper dbHelper;
    private Map<String, Double> currencyMap = new LinkedHashMap<>();

    // --- NEW: A variable to hold the final expression to be saved ---
    private String finalExpressionToSave = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);

        // Initialize dbHelper once
        dbHelper = new CalculationDBHelper(this);

        initializeViews();
        setupToolbar();
        setupSpinners();
        setupKeypad();
        setupUnitSelectionListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        updateDisplayAndConvert();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.currency_toolbar);
        inputDisplay = findViewById(R.id.currency_input_display);
        resultText = findViewById(R.id.text_view_currency_result);
        fromCurrencySpinner = findViewById(R.id.spinner_from_currency);
        toCurrencySpinner = findViewById(R.id.spinner_to_currency);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinners() {
        currencyMap = dbHelper.getUnitsByType("Currency");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyMap.keySet().toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromCurrencySpinner.setAdapter(adapter);
        toCurrencySpinner.setAdapter(adapter);

        String[] currencies = currencyMap.keySet().toArray(new String[0]);
        int usdPosition = -1;
        int eurPosition = -1;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals("USD")) usdPosition = i;
            if (currencies[i].equals("EUR")) eurPosition = i;
        }

        if (usdPosition != -1) fromCurrencySpinner.setSelection(usdPosition);
        if (eurPosition != -1) toCurrencySpinner.setSelection(eurPosition);
    }

    private void setupKeypad() {
        int[] numberButtonIds = {R.id.keypad_btn_0, R.id.keypad_btn_1, R.id.keypad_btn_2, R.id.keypad_btn_3, R.id.keypad_btn_4, R.id.keypad_btn_5, R.id.keypad_btn_6, R.id.keypad_btn_7, R.id.keypad_btn_8, R.id.keypad_btn_9, R.id.keypad_btn_decimal};
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
        fromCurrencySpinner.setOnItemSelectedListener(listener);
        toCurrencySpinner.setOnItemSelectedListener(listener);
    }

    private void appendInput(String value) {
        if (currentInput.equals("0") && !value.equals(".")) {
            currentInput = value;
        } else if (value.equals(".") && currentInput.contains(".")) {
            return;
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

    private void convert() {
        if (currentInput.isEmpty() || currentInput.equals("0") || currentInput.equals(".")) {
            resultText.setText("");
            // Clear the expression if the input is invalid
            finalExpressionToSave = null;
            return;
        }
        if (fromCurrencySpinner.getSelectedItem() == null || toCurrencySpinner.getSelectedItem() == null) {
            return;
        }
        try {
            String fromCurrency = fromCurrencySpinner.getSelectedItem().toString();
            String toCurrency = toCurrencySpinner.getSelectedItem().toString();
            double amount = Double.parseDouble(currentInput);
            Double fromFactor = currencyMap.get(fromCurrency);
            Double toFactor = currencyMap.get(toCurrency);

            if (fromFactor == null || toFactor == null || toFactor == 0) {
                resultText.setText("");
                finalExpressionToSave = null;
                return;
            }

            double amountInBase = amount * fromFactor;
            double result = amountInBase / toFactor;
            String resultString = String.format(Locale.US, "= %.2f", result);
            resultText.setText(resultString);

            // --- MODIFIED: Instead of saving immediately, update the final expression variable ---
            finalExpressionToSave = String.format(Locale.US, "%s %s %s %s", currentInput, fromCurrency, resultString, toCurrency);

        } catch (NumberFormatException e) {
            resultText.setText("Error");
            finalExpressionToSave = null; // Don't save on error
            e.printStackTrace();
        }
    }




    @Override
    protected void onDestroy() {
        // --- NEW: Save the final valid expression before closing ---
        if (finalExpressionToSave != null && !finalExpressionToSave.isEmpty()) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION, finalExpressionToSave);
            db.insert(CalculationHistoryContract.HistoryEntry.TABLE_NAME, null, values);
        }

        // Now, safely close the database helper
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
