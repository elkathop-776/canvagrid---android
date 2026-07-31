package com.example.canvagrid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ProjectDbHelper dbHelper;
    private String activeDialogType = null; // "EXIT", "HELP" or null

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ProjectDbHelper(this);

        // new project / editor button
        Button btnEditor = findViewById(R.id.btn_new_project);
        if (btnEditor != null) {
            btnEditor.setOnClickListener(v -> {
                Intent ed = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(ed);
            });
        }

        // saved projects button (check if db has data first)
        Button btnSaved = findViewById(R.id.btn_saved_projects);
        if (btnSaved != null) {
            btnSaved.setOnClickListener(v -> {
                boolean hasProjects = dbHelper.hasSavedProjects();
                if (hasProjects) {
                    Intent sp = new Intent(MainActivity.this, SavedProjectsActivity.class);
                    startActivity(sp);
                } else {
                    Toast.makeText(MainActivity.this, "No Saved Projects yet!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // canvas size suggestion button
        Button btnSuggest = findViewById(R.id.btn_canva_suggest);
        if (btnSuggest != null) {
            btnSuggest.setOnClickListener(v -> {
                Intent suggestIntent = new Intent(MainActivity.this, SuggestSizeActivity.class);
                startActivity(suggestIntent);
            });
        }

        // handle device back button
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                activeDialogType = "EXIT";
                showAppExitDialog();
            }
        });

        // restore open dialogues after rotation
        if (savedInstanceState != null) {
            activeDialogType = savedInstanceState.getString("active_dialog_type", null);
            if (activeDialogType != null) {
                if ("EXIT".equals(activeDialogType)) {
                    showAppExitDialog();
                } else if ("HELP".equals(activeDialogType)) {
                    showHelpDialog("Application Guide", getMainHelpData());
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            activeDialogType = "HELP"; // Record that the general guide modal is showing
            showHelpDialog("Application Guide", getMainHelpData());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // dialogue to exit and close the app
    private void showAppExitDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle("Exit CanvaGrid")
                .setMessage("Are you sure you want to close the application?")
                .setPositiveButton("Exit", (dialog, which) -> {
                    activeDialogType = null;
                    finishAffinity(); // close all activities and exit
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    activeDialogType = null;
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .setCancelable(false)
                .show();
    }

    // help data arraylist
    private java.util.List<HelpItem> getMainHelpData() {
        java.util.List<HelpItem> list = new java.util.ArrayList<>();
        list.add(new HelpItem("1. Grid Editor", "Open the editor to look for a photo from your device and draw a grid on top of it.", R.drawable.outline_edit_24));
        list.add(new HelpItem("2. Canvas Suggestion", "Upload your photo and get a smart recommendation for the best real-world canvas size.", R.drawable.outline_palette_24));
        list.add(new HelpItem("3. Saved Projects", "Open this section to find and continuing working on your previously saved grid setups.", R.drawable.outline_save_24));
        return list;
    }

    // show help recycler view dialogue
    private void showHelpDialog(String title, java.util.List<HelpItem> data) {
        androidx.recyclerview.widget.RecyclerView recyclerView = new androidx.recyclerview.widget.RecyclerView(this);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        recyclerView.setPadding(0, 24, 0, 24);

        HelpAdapter adapter = new HelpAdapter(data);
        recyclerView.setAdapter(adapter);

        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle(title)
                .setView(recyclerView)
                .setPositiveButton("Got it", (dialog, which) -> {
                    activeDialogType = null;
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .show();
    }

    // save state on rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeDialogType != null) {
            outState.putString("active_dialog_type", activeDialogType);
        }
    }
}