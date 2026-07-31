package com.example.canvagrid;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;

public class SuggestSizeActivity extends AppCompatActivity {

    // ui views
    private ImageView ivPreview;
    private TextView tvDimensions, tvSuggestion, tvGridSuggestion;
    private LinearLayout llResultPanel;

    // variables & state data
    private Uri selectedImageUri;
    private long backPressedTime;
    private int suggestedGridValue = 5;
    private String activeDialogType = null; // "CHANGE_IMAGE", "HELP" or null
    private int finalSuggestedWidth = 40;
    private int finalSuggestedHeight = 40;

    // predefined canvas sizes (width x height in dm)
    private final int[][] PREDEFINED_SIZES = {
            // square config
            {4, 4}, {6, 6}, {8, 8}, {10, 10}, {12, 12},
            // portrait config
            {6, 8}, {5, 7}, {8, 10}, {9, 12}, {4, 6}, {4, 8},
            // landscape config
            {8, 6}, {7, 5}, {10, 8}, {12, 9}, {6, 4}, {8, 4}
    };

    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggest_size);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivPreview = findViewById(R.id.iv_suggest_preview);
        tvDimensions = findViewById(R.id.tv_dimensions);
        tvSuggestion = findViewById(R.id.tv_canva);
        tvGridSuggestion = findViewById(R.id.tv_suggestion);
        llResultPanel = findViewById(R.id.ll_result_panel);
        Button btnSelect = findViewById(R.id.btn_select_suggest);
        Button btnProceed = findViewById(R.id.btn_go_to_editor);

        // restore state after rotation
        if (savedInstanceState != null) {
            // Recover temporal stamps to preserve exit threshold synchronization inside landscape frames
            backPressedTime = savedInstanceState.getLong("saved_back_pressed_time", 0);
            activeDialogType = savedInstanceState.getString("active_dialog_type", null);
            if (savedInstanceState.containsKey("suggested_image_uri")) {
                String uriString = savedInstanceState.getString("suggested_image_uri");
                if (uriString != null) {
                    selectedImageUri = Uri.parse(uriString);
                    analyzeImage(selectedImageUri);
                }
            }
            if (activeDialogType != null) {
                if ("CHANGE_IMAGE".equals(activeDialogType)) {
                    showChangeImageDialog();
                } else if ("HELP".equals(activeDialogType)) {
                    showHelpDialog("Canvas Suggestion Guide", getSuggestHelpData());
                }
            }
        }

        // gallery selection callback result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        analyzeImage(selectedImageUri);
                    }
                }
        );

        btnSelect.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                activeDialogType = "CHANGE_IMAGE";
                showChangeImageDialog();
            } else {
                openGallery();
            }
        });

        // pass permissions & forward data to the editor workspace
        btnProceed.setOnClickListener(v -> {
            try {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("IMAGE_URI", selectedImageUri.toString());
            intent.putExtra("GRID_SIZE", suggestedGridValue);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            startActivity(intent);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleDoubleBackToExit();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // decode bitmap options and calculate the closest aspect ratio match
    private void analyzeImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();

            // read bounds only without loading the full image into memory
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            int imgWidth = options.outWidth;
            int imgHeight = options.outHeight;

            ivPreview.setImageURI(uri);
            tvDimensions.setText(getString(R.string.dimensions, imgWidth, imgHeight));

            double imgRatio = (double) imgWidth / (double) imgHeight;
            double minDifference = Double.MAX_VALUE;
            int bestMatchIndex = 0;

            // search for the best aspect ratio fit from preset sizes
            for (int i = 0; i < PREDEFINED_SIZES.length; i++) {
                double canvasRatio = (double) PREDEFINED_SIZES[i][0] / PREDEFINED_SIZES[i][1];
                double difference = Math.abs(imgRatio - canvasRatio);

                if (difference < minDifference) {
                    minDifference = difference;
                    bestMatchIndex = i;
                }
            }

            finalSuggestedWidth = PREDEFINED_SIZES[bestMatchIndex][0];
            finalSuggestedHeight = PREDEFINED_SIZES[bestMatchIndex][1];

            // set grid line increments between 2 and 12 rules safely
            suggestedGridValue = Math.max(finalSuggestedWidth, finalSuggestedHeight);
            if (suggestedGridValue < 2) suggestedGridValue = 2;
            if (suggestedGridValue > 12) suggestedGridValue = 12;

            int displayWidth = finalSuggestedWidth * 10;
            int displayHeight = finalSuggestedHeight * 10;

            tvSuggestion.setText(getString(R.string.suggest_canva, displayWidth, displayHeight));
            tvGridSuggestion.setText(getString(R.string.grid_suggest, suggestedGridValue));

            llResultPanel.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Error analyzing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDoubleBackToExit() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) { finish(); }
        else { Toast.makeText(this, "Press BACK again to return to main menu.", Toast.LENGTH_SHORT).show(); }
        backPressedTime = System.currentTimeMillis();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleDoubleBackToExit();
            return true;
        }
        if (item.getItemId() == R.id.menu_help) {
            activeDialogType = "HELP"; // Flag that the onboarding instructions guide has been triggered
            showHelpDialog("Canvas Suggestion Guide", getSuggestHelpData());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // confirmation prompt before changing current image setup
    private void showChangeImageDialog() {
        activeDialogType = "CHANGE_IMAGE";

        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.baseline_warning_24);

        if (icon != null) {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_NO) {
                icon.setColorFilter(android.graphics.Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                icon.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle("Change Image")
                .setMessage("Are you sure you want to select a new image? Your current canvas suggestion will be reset.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    activeDialogType = null;
                    openGallery();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    activeDialogType = null;
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .setIcon(icon)
                .show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("saved_back_pressed_time", backPressedTime);

        if (selectedImageUri != null) {
            outState.putString("suggested_image_uri", selectedImageUri.toString());
        }
        if (activeDialogType != null) {
            outState.putString("active_dialog_type", activeDialogType);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // help data arraylist
    private java.util.List<HelpItem> getSuggestHelpData() {
        java.util.List<HelpItem> list = new java.util.ArrayList<>();
        list.add(new HelpItem("Step 1: Upload your image", "Press the 'Select Image to Analyze' button to choose a picture from your device.", R.drawable.outline_upload_24));
        list.add(new HelpItem("Step 2: View dimensions", "Look at the screen to see the ideal canvas size (like 60x80) and the suggested grid lines calculated automatically.", R.drawable.baseline_eye_24));
        list.add(new HelpItem("Step 3: Open the editor", "Press the 'Open in Editor with this Grid' button to automatically apply these suggested lines and transfer your picture straight into the workspace.", R.drawable.outline_edit_24));
        return list;
    }

    // help guide recycler view dialogue
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
}