package com.example.canvagrid;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SavedProjectsActivity extends AppCompatActivity implements ProjectAdapter.OnProjectActionListener {

    // ui views
    private RecyclerView recyclerView;
    private ProjectAdapter adapter;
    private ProjectDbHelper dbHelper;
    private ArrayList<ProjectModel> projectList;

    // variables
    private long backPressedTime;
    private String activeDialogType = null; // "RENAME", "DELETE", "HELP" or null
    private int activeProjectId = -1;
    private String activeProjectName = "";
    private int activePosition = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_projects);

        dbHelper = new ProjectDbHelper(this);
        recyclerView = findViewById(R.id.rv_saved_projects);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // enable back arrow in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // handle back press (fragment pop or double click to exit)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finish();
                    } else {
                        Toast.makeText(SavedProjectsActivity.this, "Press BACK again to return to main menu.", Toast.LENGTH_SHORT).show();
                    }
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });

        // restore open dialogues after rotation
        if (savedInstanceState != null) {
            backPressedTime = savedInstanceState.getLong("saved_back_pressed_time", 0);
            activeDialogType = savedInstanceState.getString("active_dialog_type", null);
            activeProjectId = savedInstanceState.getInt("active_project_id", -1);
            activeProjectName = savedInstanceState.getString("active_project_name", "");
            activePosition = savedInstanceState.getInt("active_position", -1);

            if (activeDialogType != null) {
                if ("RENAME".equals(activeDialogType) && activeProjectId != -1 && activePosition != -1) {
                    showRenameDialog(activeProjectId, activeProjectName, activePosition);
                } else if ("DELETE".equals(activeDialogType) && activeProjectId != -1 && activePosition != -1) {
                    showDeleteDialog(activeProjectId, activePosition);
                } else if ("HELP".equals(activeDialogType)) {
                    showHelpDialog("Saved Projects Guide", getSavedHelpData());
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            activeDialogType = "HELP";
            showHelpDialog("Saved Projects Guide", getSavedHelpData());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // load or reload data from db
        projectList = dbHelper.getAllProjects();

        // exit if no projects left
        if (projectList.isEmpty()) {
            Toast.makeText(this, "No projects found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ProjectAdapter(this, projectList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // adapter callbacks
    @Override
    public void onRenameSelected(int projectId, String currentName, int position) {
        activeDialogType = "RENAME";
        activeProjectId = projectId;
        activeProjectName = currentName;
        activePosition = position;
        showRenameDialog(projectId, currentName, position);
    }

    @Override
    public void onDeleteSelected(int projectId, int position) {
        activeDialogType = "DELETE";
        activeProjectId = projectId;
        activePosition = position;
        showDeleteDialog(projectId, position);
    }

    // dialogue for renaming a project record
    private void showRenameDialog(int projectId, String currentName, int position) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(currentName);
        input.setSelectAllOnFocus(true);

        // keep active name updated on typing
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeProjectName = s.toString();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle("Rename Project")
                .setMessage("Enter new name for this project:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        boolean success = dbHelper.updateProjectName(projectId, newName);
                        if (success) {
                            if (projectList != null && position < projectList.size()) {
                                projectList.get(position).setName(newName);
                                if (adapter != null) adapter.notifyItemChanged(position);
                            }
                            Toast.makeText(this, "Project renamed!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to rename project.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                    }
                    activeDialogType = null;
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    activeDialogType = null;
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .show();
    }

    // dialogue for deleting a project record
    private void showDeleteDialog(int projectId, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteProject(projectId);
                    if (success) {
                        if (projectList != null && position < projectList.size()) {
                            projectList.remove(position);
                            if (adapter != null) {
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, projectList.size());
                            }
                        }
                        Toast.makeText(this, "Project deleted!", Toast.LENGTH_SHORT).show();

                        if (projectList == null || projectList.isEmpty()) {
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Failed to delete project.", Toast.LENGTH_SHORT).show();
                    }
                    activeDialogType = null;
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    activeDialogType = null;
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .show();
    }

    // save state before rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("saved_back_pressed_time", backPressedTime);

        if (activeDialogType != null) {
            outState.putString("active_dialog_type", activeDialogType);
            outState.putInt("active_project_id", activeProjectId);
            outState.putString("active_project_name", activeProjectName);
            outState.putInt("active_position", activePosition);
        }
    }

    // help data arraylist
    private java.util.List<HelpItem> getSavedHelpData() {
        java.util.List<HelpItem> list = new java.util.ArrayList<>();
        list.add(new HelpItem("Step 1: Browse your projects", "Look through the list to find the saved project you want to manage.", R.drawable.outline_lists_24));
        list.add(new HelpItem("Step 2: Press on the project", "Tap directly on the project item from the list to open its options menu.", R.drawable.outline_menu_open_24));
        list.add(new HelpItem("Step 3: Choose a menu option", "Select one of the 4 available actions depending on what you want to do:", R.drawable.outline_gesture_select_24));
        list.add(new HelpItem("• Rename Project", "Change the title of your project to give it a new name.", R.drawable.outline_edit_square_24));
        list.add(new HelpItem("• Simple View", "Open and look at the image with the grid overlay without any editing tools on your screen.", R.drawable.baseline_eye_24));
        list.add(new HelpItem("• Edit in Editor", "Load the project back into the editor workspace to change the grid size, opacity, or colors.", R.drawable.outline_edit_24));
        list.add(new HelpItem("• Delete Project", "Permanently remove the project and its grid settings from the application.", R.drawable.outline_delete_24));
        return list;
    }

    // help guide dialogue view
    private void showHelpDialog(String title, java.util.List<HelpItem> data) {
        androidx.recyclerview.widget.RecyclerView dialogRecyclerView = new androidx.recyclerview.widget.RecyclerView(this);
        dialogRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        dialogRecyclerView.setPadding(0, 24, 0, 24);

        HelpAdapter helpAdapter = new HelpAdapter(data);
        dialogRecyclerView.setAdapter(helpAdapter);

        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle(title)
                .setView(dialogRecyclerView)
                .setPositiveButton("Got it", (dialog, which) -> {
                    activeDialogType = null; // Clear active trackers post modal confirmation
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> activeDialogType = null) // Clear operational values if canceled via physical back tap
                .show();
    }
}