package com.example.canvagrid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.canvagrid.ProjectContract.ProjectEntry;
import java.util.ArrayList;

public class ProjectDbHelper extends SQLiteOpenHelper {

    // db configuration
    public static final String DATABASE_NAME = "CanvaGrid.db";
    public static final int DATABASE_VERSION = 2;

    // queries to create and drop table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ProjectEntry.TABLE_NAME + " ("
                    + ProjectEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ProjectContract.ProjectEntry.COLUMN_NAME + " TEXT, "
                    + ProjectEntry.COLUMN_IMAGE_URI + " TEXT,"
                    + ProjectEntry.COLUMN_GRID_SIZE + " INTEGER,"
                    + ProjectEntry.COLUMN_OPACITY + " INTEGER,"
                    + ProjectEntry.COLUMN_COLOR + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ProjectEntry.TABLE_NAME;

    public ProjectDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    // Drops existing layout schemas and forces complete regeneration during version upgrades
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ProjectContract.ProjectEntry.TABLE_NAME);
        onCreate(db);
    }

    // insert new project to db
    public boolean insertProject(String imageUri, int gridSize, int opacity, int color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ProjectEntry.COLUMN_IMAGE_URI, imageUri);
        values.put(ProjectEntry.COLUMN_GRID_SIZE, gridSize);
        values.put(ProjectEntry.COLUMN_OPACITY, opacity);
        values.put(ProjectEntry.COLUMN_COLOR, color);

        long newRowId = db.insert(ProjectEntry.TABLE_NAME, null, values);
        return newRowId != -1;
    }

    // check if any data exists
    public boolean hasSavedProjects() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                ProjectContract.ProjectEntry.TABLE_NAME,
                new String[]{ProjectContract.ProjectEntry._ID},
                null, null, null, null, null
        );

        boolean hasRows = false;
        if (cursor != null) {
            hasRows = cursor.getCount() > 0;
            cursor.close(); // close cursor to avoid memory leaks
        }

        return hasRows;
    }

    // fetch all projects from db (newest first)
    public ArrayList<ProjectModel> getAllProjects() {
        ArrayList<ProjectModel> projectList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                ProjectContract.ProjectEntry.TABLE_NAME,
                null, null, null, null, null,
                ProjectContract.ProjectEntry._ID + " DESC"
        );

        if (cursor != null) {
            // get column indexes
            int idIndex = cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry._ID);
            int uriIndex = cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_IMAGE_URI);
            int sizeIndex = cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_GRID_SIZE);
            int opacityIndex = cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_OPACITY);
            int colorIndex = cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_COLOR);
            int nameIndex = cursor.getColumnIndex(ProjectContract.ProjectEntry.COLUMN_NAME);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(idIndex);
                String uri = cursor.getString(uriIndex);
                int size = cursor.getInt(sizeIndex);
                int opacity = cursor.getInt(opacityIndex);
                int color = cursor.getInt(colorIndex);
                String name = (nameIndex != -1) ? cursor.getString(nameIndex) : null;

                // fallback name if empty
                if (name == null || name.isEmpty()) {
                    name = "Project #" + id;
                }

                ProjectModel project = new ProjectModel(id, uri, size, opacity, color);
                project.setName(name);
                projectList.add(project);
            }
            cursor.close();
        }
        return projectList;
    }

    // delete project by id
    public boolean deleteProject(int projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(
                ProjectContract.ProjectEntry.TABLE_NAME,
                ProjectContract.ProjectEntry._ID + "=?",
                new String[]{String.valueOf(projectId)}
        );
        return rowsDeleted > 0;
    }

    // update existing project data
    public boolean updateProject(int id, String uri, int gridSize, int opacity, int color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProjectContract.ProjectEntry.COLUMN_IMAGE_URI, uri);
        values.put(ProjectContract.ProjectEntry.COLUMN_GRID_SIZE, gridSize);
        values.put(ProjectContract.ProjectEntry.COLUMN_OPACITY, opacity);
        values.put(ProjectContract.ProjectEntry.COLUMN_COLOR, color);

        int rowsAffected = db.update(
                ProjectContract.ProjectEntry.TABLE_NAME,
                values,
                ProjectContract.ProjectEntry._ID + "=?",
                new String[]{String.valueOf(id)}
        );
        return rowsAffected > 0;
    }

    // rename project title
    public boolean updateProjectName(long projectId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProjectContract.ProjectEntry.COLUMN_NAME, newName);

        int rowsAffected = db.update(
                ProjectContract.ProjectEntry.TABLE_NAME,
                values,
                ProjectContract.ProjectEntry._ID + " = ?",
                new String[]{String.valueOf(projectId)}
        );
        return rowsAffected > 0;
    }
}