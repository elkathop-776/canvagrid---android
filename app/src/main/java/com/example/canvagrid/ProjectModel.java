package com.example.canvagrid;

public class ProjectModel {
    private final int id;
    private String name;
    private final String imageUri;
    private final int gridSize;
    private final int opacity;
    private final int color;

    // constructor
    public ProjectModel(int id, String imageUri, int gridSize, int opacity, int color) {
        this.id = id;
        this.name = "Project #" + id; // default name fallback
        this.imageUri = imageUri;
        this.gridSize = gridSize;
        this.opacity = opacity;
        this.color = color;
    }

    // getters & setters
    public int getId() { return id; }
    public String getImageUri() { return imageUri; }
    public int getGridSize() { return gridSize; }
    public int getOpacity() { return opacity; }

    public int getColor() { return color; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}