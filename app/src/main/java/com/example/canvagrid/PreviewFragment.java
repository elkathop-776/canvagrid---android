package com.example.canvagrid;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PreviewFragment extends Fragment {

    private String uriStr;
    private int gridSize;
    private int opacity;
    private int color;
    private String projectName;

    // required empty constructor
    public PreviewFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore state after rotation
        if (savedInstanceState != null) {
            uriStr = savedInstanceState.getString("uri");
            gridSize = savedInstanceState.getInt("gridSize");
            opacity = savedInstanceState.getInt("opacity");
            color = savedInstanceState.getInt("color");
            projectName = savedInstanceState.getString("projectName");
        }
        // load initial bundle arguments from adapter
        else if (getArguments() != null) {
            uriStr = getArguments().getString("uri");
            gridSize = getArguments().getInt("gridSize");
            opacity = getArguments().getInt("opacity");
            color = getArguments().getInt("color");
            projectName = getArguments().getString("projectName");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview, container, false);

        ImageView ivFull = view.findViewById(R.id.iv_preview_full);
        GridView gvFull = view.findViewById(R.id.gv_preview_full);
        TextView tvProjectTitle = view.findViewById(R.id.tv_preview_project_name);

        // set header title text
        if (tvProjectTitle != null) {
            if (projectName != null && !projectName.isEmpty()) {
                tvProjectTitle.setText(projectName);
            } else {
                tvProjectTitle.setText("Untitled Project");
            }
        }

        // load image and set up grid overlay
        if (uriStr != null && !uriStr.isEmpty() && ivFull != null && gvFull != null) {
            ivFull.setImageURI(Uri.parse(uriStr));
            gvFull.bindToImageView(ivFull);

            // wait for image view to be drawn before calculating grid bounds
            ivFull.post(() -> {
                if (ivFull.getDrawable() == null) return;

                gvFull.setGridSize(gridSize, gridSize);
                gvFull.setOpacity(opacity);
                gvFull.setGridColor(color);

                gvFull.setVisibility(View.VISIBLE);
                gvFull.invalidate();
            });
        }
        return view;
    }

    // save state before rotation
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("uri", uriStr);
        outState.putInt("gridSize", gridSize);
        outState.putInt("opacity", opacity);
        outState.putInt("color", color);
        outState.putString("projectName", projectName);
    }
}