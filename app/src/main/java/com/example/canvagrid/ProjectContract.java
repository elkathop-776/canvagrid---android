package com.example.canvagrid;

import android.provider.BaseColumns;

public final class ProjectContract {

    // empty constructor to prevent instantiation
    private ProjectContract() {}

    // database table configuration
    public static class ProjectEntry implements BaseColumns {
        public static final String TABLE_NAME = "projects";
        public static final String COLUMN_NAME = "project_name";
        public static final String COLUMN_IMAGE_URI = "image_uri";
        public static final String COLUMN_GRID_SIZE = "grid_size";
        public static final String COLUMN_OPACITY = "opacity";
        public static final String COLUMN_COLOR = "color";
    }
}