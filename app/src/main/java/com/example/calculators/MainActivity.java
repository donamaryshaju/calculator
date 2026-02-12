

package com.example.calculators;

import android.content.ContentValues; // ADDED
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase; // ADDED
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private TextView display;
    private TextView operationDisplay;
    private String currentInput = "";
    private String currentOperator = "";
    private double firstValue = Double.NaN;
    private boolean isNewInput = true;
    private String operationHistory = "";
    private DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    private CalculationDBHelper dbHelper; // ADDED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        dbHelper = new CalculationDBHelper(this); // ADDED

        display = findViewById(R.id.display);
        operationDisplay = findViewById(R.id.operation_display);
        setupButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_measurement) {
            startActivity(new Intent(this, MeasurementActivity.class));
            return true;
        } else if (itemId == R.id.menu_currency) {
            startActivity(new Intent(this, CurrencyActivity.class));
            return true;
        } else if (itemId == R.id.menu_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtons() {
        // Number buttons (0-9, 00, decimal)
        int[] numberButtonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_double_zero, R.id.btn_decimal
        };

        // Operator buttons
        int[] operatorButtonIds = {
                R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide,
                R.id.btn_equals
        };

        // Setup number buttons
        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(v -> {
                Button button = (Button) v;
                appendInput(button.getText().toString());
            });
        }

        // Setup operator buttons
        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(v -> {
                Button button = (Button) v;
                handleOperator(button.getText().toString());
            });
        }

        findViewById(R.id.btn_clear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btn_percent).setOnClickListener(v -> calculatePercentage());
        findViewById(R.id.btn_backspace).setOnClickListener(v -> deleteLastCharacter());
    }

    private void appendInput(String value) {
        if (isNewInput) {
            currentInput = "";
            isNewInput = false;
        }

        if (value.equals("00") && (currentInput.isEmpty() || currentInput.equals("0"))) {
            currentInput = "0";
        } else if (value.equals(".") && currentInput.contains(".")) {
            return;
        } else if (currentInput.equals("0") && !value.equals(".")) {
            currentInput = value;
        } else {
            currentInput += value;
        }

        updateDisplay();
    }

    private void handleOperator(String operator) {
        if (currentInput.isEmpty()) return;

        if (!Double.isNaN(firstValue) && !isNewInput) {
            calculateResult();
        } else {
            firstValue = Double.parseDouble(currentInput);
        }

        if (operator.equals("=")) {
            currentOperator = "";
        } else {
            currentOperator = operator;
            operationHistory = formatNumberForDisplay(firstValue) + " " + currentOperator;
            updateOperationDisplay();
        }

        isNewInput = true;
    }

    private void calculateResult() {
        if (Double.isNaN(firstValue) || currentInput.isEmpty() || currentOperator.isEmpty()) {
            return;
        }

        double secondValue = Double.parseDouble(currentInput);
        double result = 0;

        switch (currentOperator) {
            case "+":
                result = firstValue + secondValue;
                break;
            case "-":
                result = firstValue - secondValue;
                break;
            case "×":
                result = firstValue * secondValue;
                break;
            case "÷":
                if (secondValue == 0) {
                    display.setText("Error");
                    // Don't save errors to the database
                    // Keep the state so the user can clear it, but prevent further calculations
                    // or automatically reset. Here, we'll just return.
                    return;
                }
                result = firstValue / secondValue;
                break;
        }

        String resultStr = formatNumberForDisplay(result);
        operationHistory = formatNumberForDisplay(firstValue) + " " + currentOperator + " " + formatNumberForDisplay(secondValue) + " = " + resultStr;

        saveCalculationToDb(operationHistory); // ADDED: Save the result

        updateOperationDisplay();

        display.setText(resultStr);
        firstValue = result;
        currentInput = resultStr; // Allow chaining calculations
        isNewInput = true;
    }

    private void clearAll() {
        currentInput = "";
        currentOperator = "";
        firstValue = Double.NaN;
        isNewInput = true;
        operationHistory = "";
        display.setText("0");
        updateOperationDisplay();
    }

    private void calculatePercentage() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput) / 100;
            currentInput = formatNumberForDisplay(value);
            updateDisplay();
            isNewInput = true;
        }
    }

    private void deleteLastCharacter() {
        if (!currentInput.isEmpty() && !isNewInput) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                currentInput = "0";
                isNewInput = true;
            }
            updateDisplay();
        }
    }

    private void updateDisplay() {
        if (currentInput.isEmpty()) {
            display.setText("0");
        } else {
            display.setText(currentInput);
        }
    }

    private void updateOperationDisplay() {
        operationDisplay.setText(operationHistory);
    }

    private String formatNumberForDisplay(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        } else {
            return decimalFormat.format(value);
        }
    }

    // ADDED NEW METHOD: saveCalculationToDb
    private void saveCalculationToDb(String expression) {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION, expression);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(CalculationHistoryContract.HistoryEntry.TABLE_NAME, null, values);

        // --- Add logging to check if the insert was successful ---
        if (newRowId == -1) {
            Log.e("DB_INSERT_CHECK", "Failed to insert row for expression: " + expression);
        } else {
            Log.d("DB_INSERT_CHECK", "Successfully inserted row with ID: " + newRowId);
        }
    }
}

