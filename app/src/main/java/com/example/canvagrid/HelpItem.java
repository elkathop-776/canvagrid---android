package com.example.canvagrid;

public class HelpItem {
    private final String title;
    private final String description;
    private final int iconResId;

    // constructor
    public HelpItem(String title, String description, int iconResId) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
    }

    // getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
}