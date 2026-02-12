package com.example.calculators;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private CalculationDBHelper dbHelper;
    private ArrayList<String> historyList;
    private ArrayAdapter<String> historyAdapter;
    private TextView emptyHistoryText;

    // --- ANDROID LIFECYCLE METHODS ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Set up the toolbar and enable the back arrow
        Toolbar toolbar = findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar); // This is the key line that enables clicks
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 2. Initialize database helper and UI elements
        dbHelper = new CalculationDBHelper(this);
        historyListView = findViewById(R.id.history_list_view);
        emptyHistoryText = findViewById(R.id.empty_history_text);

        // 3. Load initial data from the database
        loadHistoryData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This method inflates the menu layout (history_menu.xml) and adds its items to the toolbar.
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // This method is called whenever a menu item in the toolbar is clicked.
        int itemId = item.getItemId(); // Get the ID of the clicked item

        if (itemId == android.R.id.home) {
            // This is the system ID for the "home" or "up" button (the back arrow).
            finish(); // This command closes the current activity and returns to the previous one.
            return true; // We have handled the click, so return true.
        } else if (itemId == R.id.menu_clear_history) {
            // This is the ID we defined in history_menu.xml for our "Clear History" button.
            clearHistory(); // Call the method to delete the data.
            return true; // We have handled the click, so return true.
        }

        // If the clicked item is not one we handle, let the system deal with it.
        return super.onOptionsItemSelected(item);
    }


    // --- HELPER METHODS ---

    /**
     * Reads all calculation records from the database and displays them in the ListView.
     */
    private void loadHistoryData() {
        historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION};
        Cursor cursor = db.query(
                CalculationHistoryContract.HistoryEntry.TABLE_NAME,
                projection, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String expression = cursor.getString(cursor.getColumnIndexOrThrow(CalculationHistoryContract.HistoryEntry.COLUMN_NAME_EXPRESSION));
            historyList.add(expression);
        }
        cursor.close();
        db.close();

        // Show the newest calculations at the top of the list.
        Collections.reverse(historyList);

        // Set up the adapter to display the data.
        historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        historyListView.setAdapter(historyAdapter);

        // Make sure the correct view is visible (the list or the "empty" text).
        updateVisibility();
    }

    /**
     * Deletes all records from the database and updates the screen to show it's empty.
     */
    private void clearHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // This command deletes all rows from the table.
        db.delete(CalculationHistoryContract.HistoryEntry.TABLE_NAME, null, null);
        db.close();

        // Clear the list that the adapter is using.
        historyList.clear();
        // Notify the adapter that the data has changed so it can refresh the ListView.
        historyAdapter.notifyDataSetChanged();

        // Update the visibility to show the "No History Yet" message.
        updateVisibility();

        // Optional: Show a confirmation message to the user.
        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks if the history list is empty and shows or hides the "No History Yet" text.
     */
    private void updateVisibility() {
        if (historyList.isEmpty()) {
            historyListView.setVisibility(View.GONE);
            emptyHistoryText.setVisibility(View.VISIBLE);
        } else {
            historyListView.setVisibility(View.VISIBLE);
            emptyHistoryText.setVisibility(View.GONE);
        }
    }
}
