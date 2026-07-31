package com.example.canvagrid;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

public class EditorActivity extends AppCompatActivity {

    // ui views
    private ImageView ivSelectedImage;
    private GridView gridView;
    private SeekBar sbGridSize;
    private SeekBar sbOpacity;

    // variables
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri savedImageUri;
    private ProjectDbHelper dbHelper;
    private int currentProjectId = -1; // -1 is new project
    private String activeDialogType = null; // keeps track of open dialog for rotation


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // initialize ui
        ivSelectedImage = findViewById(R.id.iv_selected_image);
        gridView = findViewById(R.id.grid_view);
        Button btnSelectImage = findViewById(R.id.btn_select_image);
        sbGridSize = findViewById(R.id.sb_grid_size);
        sbOpacity = findViewById(R.id.sb_opacity);
        View controlPanel = findViewById(R.id.control_panel);
        Button btnMainSave = findViewById(R.id.btn_main_save);

        View colorRed = findViewById(R.id.color_red);
        View colorGreen = findViewById(R.id.color_green);
        View colorBlue = findViewById(R.id.color_blue);
        View colorWhite = findViewById(R.id.color_white);
        View colorBlack = findViewById(R.id.color_black);

        dbHelper = new ProjectDbHelper(this);

        // color selectors logic
        if (colorRed != null)
            colorRed.setOnClickListener(v -> { gridView.setGridColor(android.graphics.Color.RED); gridView.invalidate(); });
        if (colorGreen != null)
            colorGreen.setOnClickListener(v -> { gridView.setGridColor(android.graphics.Color.GREEN); gridView.invalidate(); });
        if (colorBlue != null)
            colorBlue.setOnClickListener(v -> { gridView.setGridColor(android.graphics.Color.BLUE); gridView.invalidate(); });
        if (colorWhite != null)
            colorWhite.setOnClickListener(v -> { gridView.setGridColor(android.graphics.Color.WHITE); gridView.invalidate(); });
        if (colorBlack != null)
            colorBlack.setOnClickListener(v -> { gridView.setGridColor(android.graphics.Color.BLACK); gridView.invalidate(); });

        // handle screen rotation restore
        if (savedInstanceState != null) {
            currentProjectId = savedInstanceState.getInt("saved_project_id", -1);
            activeDialogType = savedInstanceState.getString("active_dialog_type", null);

            if (savedInstanceState.containsKey("saved_image_uri")) {
                String uriString = savedInstanceState.getString("saved_image_uri");
                if (uriString != null && !uriString.isEmpty()) {
                    savedImageUri = Uri.parse(uriString);

                    try {
                        java.io.InputStream inputStream = getContentResolver().openInputStream(savedImageUri);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();

                        if (bitmap != null) {
                            ivSelectedImage.setImageBitmap(bitmap);
                            gridView.setVisibility(View.VISIBLE);
                            controlPanel.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Security reset: Please re-select image", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
            }

            int restoredSize = savedInstanceState.getInt("saved_grid_size", 5);
            int restoredOpacity = savedInstanceState.getInt("saved_opacity", 255);
            int restoredColor = savedInstanceState.getInt("saved_color", android.graphics.Color.RED);

            if (restoredSize < 2) restoredSize = 2;

            sbGridSize.setProgress(restoredSize - 2);
            sbOpacity.setProgress(restoredOpacity);

            gridView.setGridSize(restoredSize, restoredSize);
            gridView.setOpacity(restoredOpacity);
            gridView.setGridColor(restoredColor);

            // reopen the dialog that was open before rotation
            if (activeDialogType != null) {
                if ("CHANGE_IMAGE".equals(activeDialogType)) {
                    showChangeImageDialog();
                } else if ("EXIT".equals(activeDialogType)) {
                    showExitConfirmationDialog();
                } else if ("SAVE_OPTIONS".equals(activeDialogType)) {
                    showSaveOptionsDialog();
                } else if ("HELP".equals(activeDialogType)) {
                    showHelpDialog("Editor Guide", getEditorHelpData());
                }
            }
        }

        // gallery picker result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            savedImageUri = imageUri;
                            ivSelectedImage.setImageURI(imageUri);
                            Toast.makeText(this, "Image loaded successfully!", Toast.LENGTH_SHORT).show();

                            try {
                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }

                            gridView.setVisibility(View.VISIBLE);
                            controlPanel.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        // image select button click
        btnSelectImage.setOnClickListener(v -> {
            if (ivSelectedImage.getDrawable() != null) {
                activeDialogType = "CHANGE_IMAGE";
                showChangeImageDialog();
            } else {
                openGallery();
            }
        });

        // device back button intercept
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                activeDialogType = "EXIT";
                showExitConfirmationDialog();
            }
        });

        // grid size seekbar
        sbGridSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualGridSize = progress + 2;
                gridView.setGridSize(actualGridSize, actualGridSize);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // grid opacity seekbar
        sbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gridView.setOpacity(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // save options popup menu
        btnMainSave.setOnClickListener(v -> {
            if (ivSelectedImage.getDrawable() == null) {
                Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
                return;
            }

            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(EditorActivity.this, btnMainSave);
            popup.getMenu().add(Menu.NONE, 1, 1, "Save to Gallery");
            popup.getMenu().add(Menu.NONE, 2, 2, "Save to Projects");

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        saveEditorImageWithGrid(ivSelectedImage);
                        return true;
                    case 2:
                        if (currentProjectId != -1) {
                            activeDialogType = "SAVE_OPTIONS";
                            showSaveOptionsDialog();
                        } else {
                            String uriStr = (savedImageUri != null) ? savedImageUri.toString() : "";
                            int currentGridSize = sbGridSize.getProgress() + 2;
                            int currentOpacity = sbOpacity.getProgress();
                            int currentColor = gridView.getGridColor();
                            boolean isSaved = dbHelper.insertProject(uriStr, currentGridSize, currentOpacity, currentColor);
                            if (isSaved) {
                                Toast.makeText(EditorActivity.this, "Project saved successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(EditorActivity.this, "Failed to save project.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            });
            popup.show();
        });

        // load intent extras if opening an existing project
        if (savedInstanceState == null) {
            if (getIntent().hasExtra("IMAGE_URI")) {
                String intentUriStr = getIntent().getStringExtra("IMAGE_URI");
                int intentGridSize = getIntent().getIntExtra("GRID_SIZE", 5);
                int intentOpacity = getIntent().getIntExtra("OPACITY", 255);
                int intentColor = getIntent().getIntExtra("COLOR", android.graphics.Color.RED);

                currentProjectId = getIntent().getIntExtra("PROJECT_ID", -1);

                if (intentUriStr != null && !intentUriStr.isEmpty()) {
                    savedImageUri = android.net.Uri.parse(intentUriStr);

                    try {
                        java.io.InputStream inputStream = getContentResolver().openInputStream(savedImageUri);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();
                        ivSelectedImage.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ivSelectedImage.setImageURI(savedImageUri);
                    }

                    if (intentGridSize < 2) intentGridSize = 2;

                    sbGridSize.setProgress(intentGridSize - 2);
                    sbOpacity.setProgress(intentOpacity);

                    gridView.setGridSize(intentGridSize, intentGridSize);
                    gridView.setOpacity(intentOpacity);
                    gridView.setGridColor(intentColor);

                    gridView.setVisibility(android.view.View.VISIBLE);
                    controlPanel.setVisibility(android.view.View.VISIBLE);

                    Toast.makeText(this, "Project loaded successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // open device gallery
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        galleryLauncher.launch(intent);
    }

    // dialogue for changing image
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
                .setMessage("Are you sure you want to select a new image? Your current grid configuration will be lost.")
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

    // dialogue for exit confirmation
    private void showExitConfirmationDialog() {
        activeDialogType = "EXIT_CONFIRMATION";

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
                .setTitle("Exit Editor")
                .setMessage("Are you sure you want to go back? You will lose all progress.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    activeDialogType = null;
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    activeDialogType = null;
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> activeDialogType = null)
                .setCancelable(false)
                .setIcon(icon)
                .show();
    }

    // dialogue for db save options
    private void showSaveOptionsDialog() {
        String uriStr = (savedImageUri != null) ? savedImageUri.toString() : "";
        int currentGridSize = sbGridSize.getProgress() + 2;
        int currentOpacity = sbOpacity.getProgress();
        int currentColor = gridView.getGridColor();

        new androidx.appcompat.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
                .setTitle("Save Options")
                .setMessage("Do you want to overwrite the existing project or save it as a new one?")
                .setPositiveButton("Overwrite", (dialog, which) -> {
                    activeDialogType = null;
                    boolean success = dbHelper.updateProject(currentProjectId, uriStr, currentGridSize, currentOpacity, currentColor);
                    if (success) {
                        Toast.makeText(EditorActivity.this, "Project updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditorActivity.this, "Failed to update project.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Save as New", (dialog, which) -> {
                    activeDialogType = null;
                    boolean success = dbHelper.insertProject(uriStr, currentGridSize, currentOpacity, currentColor);
                    if (success) {
                        Toast.makeText(EditorActivity.this, "Saved as a new project!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditorActivity.this, "Failed to save project.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
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
        if (savedImageUri != null) {
            outState.putString("saved_image_uri", savedImageUri.toString());
        }
        outState.putInt("saved_grid_size", sbGridSize.getProgress() + 2);
        outState.putInt("saved_opacity", sbOpacity.getProgress());
        outState.putInt("saved_color", gridView.getGridColor());
        outState.putInt("saved_project_id", currentProjectId);
        if (activeDialogType != null) {
            outState.putString("active_dialog_type", activeDialogType);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            activeDialogType = "EXIT";
            showExitConfirmationDialog();
            return true;
        }
        if (item.getItemId() == R.id.menu_help) {
            activeDialogType = "HELP";
            showHelpDialog("Editor Guide", getEditorHelpData());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // burn grid lines onto bitmap and save to storage
    private void saveEditorImageWithGrid(ImageView imageView) {
        if (imageView.getDrawable() == null) {
            Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap bitmapWithGrid = originalBitmap.copy(originalBitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmapWithGrid);

        int currentGridSize = sbGridSize.getProgress() + 2;
        int currentOpacity = sbOpacity.getProgress();
        int currentGridColor = gridView.getGridColor();

        Paint paint = new Paint();
        paint.setColor(currentGridColor);
        paint.setAlpha(currentOpacity);
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.STROKE);

        int width = bitmapWithGrid.getWidth();
        int height = bitmapWithGrid.getHeight();

        float cellWidth = (float) width / currentGridSize;
        float cellHeight = (float) height / currentGridSize;

        // draw lines
        for (int i = 1; i < currentGridSize; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, paint);
        }
        for (int i = 1; i < currentGridSize; i++) {
            canvas.drawLine(0, i * cellHeight, width, i * cellHeight, paint);
        }

        String fileName = "CanvaGrid_Edit_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CanvaGrid");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                fos = getContentResolver().openOutputStream(imageUri);
                bitmapWithGrid.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                if (fos != null) fos.close();

                Toast.makeText(this, "Image with grid saved to Gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // help data arraylist
    private java.util.List<HelpItem> getEditorHelpData() {
        java.util.List<HelpItem> list = new java.util.ArrayList<>();
        list.add(new HelpItem("Step 1: Upload your image", "Press the 'Select Image from Gallery' button to choose a picture from your device.", R.drawable.outline_upload_24));
        list.add(new HelpItem("Step 2: Adjust the grid lines", "Use the first slider (Grid Size) to increase or decrease the number of rows and columns on your image.", R.drawable.outline_grid_4x4_24));
        list.add(new HelpItem("Step 3: Change line visibility", "Use the second slider (Opacity) to make your grid lines thicker and darker, or faint and transparent.", R.drawable.baseline_opacity_24));
        list.add(new HelpItem("Step 4: Pick a grid color", "Tap on any of the colored circles at the bottom panel to pick a contrast color that stands out on your image.", R.drawable.outline_colors_24));
        list.add(new HelpItem("Step 5: Save your work", "Press the save button on the top menu and select either to export the image to your device's gallery or store the layout inside the app.", R.drawable.outline_save_24));
        return list;
    }

    // help guide dialogue view
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