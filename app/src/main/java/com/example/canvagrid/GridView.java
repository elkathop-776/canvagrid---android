package com.example.canvagrid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;

public class GridView extends View {
    private Paint paint;
    private int rows = 5;
    private int columns = 5;
    private int opacity = 255;

    // reference to tracking image view
    private ImageView boundImageView;

    // programmatic constructor
    public GridView(Context context) {
        super(context);
        init();
    }

    // xml layout constructor
    public GridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // default paint setup
    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
    }

    // manually bind an image view if needed
    public void bindToImageView(ImageView imageView) {
        this.boundImageView = imageView;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // try to find the target image view automatically if not manually bound
        ImageView imageView = this.boundImageView;
        if (imageView == null) {
            View parent = (View) getParent();
            if (parent != null) {
                imageView = parent.findViewById(R.id.iv_selected_image);
                if (imageView == null) {
                    imageView = parent.findViewById(R.id.iv_preview_full);
                }
            }
        }

        // skip drawing if no image is loaded
        if (imageView == null || imageView.getDrawable() == null) {
            return;
        }

        // get matrix scaling values to find exact image size on screen
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        int intrinsicWidth = imageView.getDrawable().getIntrinsicWidth();
        int intrinsicHeight = imageView.getDrawable().getIntrinsicHeight();

        float imageWidthInScreen = intrinsicWidth * f[android.graphics.Matrix.MSCALE_X];
        float imageHeightInScreen = intrinsicHeight * f[android.graphics.Matrix.MSCALE_Y];

        // calculate the exact bounding box for the image
        float left = (getWidth() - imageWidthInScreen) / 2;
        float top = (getHeight() - imageHeightInScreen) / 2;
        float right = left + imageWidthInScreen;
        float bottom = top + imageHeightInScreen;

        // update opacity alpha before rendering
        paint.setAlpha(opacity);

        // draw horizontal grid lines
        for (int i = 1; i < rows; i++) {
            float y = top + (imageHeightInScreen / rows) * i;
            canvas.drawLine(left, y, right, y, paint);
        }

        // draw vertical grid lines
        for (int i = 1; i < columns; i++) {
            float x = left + (imageWidthInScreen / columns) * i;
            canvas.drawLine(x, top, x, bottom, paint);
        }

        // draw the outer border box around the image
        canvas.drawRect(left, top, right, bottom, paint);
    }

    // public updates that trigger view redraw
    public void setGridSize(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        invalidate();
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
        invalidate();
    }

    public void setGridColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public int getGridColor() {
        if (paint != null) {
            return paint.getColor();
        }
        return android.graphics.Color.RED;
    }
}