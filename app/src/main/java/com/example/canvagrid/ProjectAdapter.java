package com.example.canvagrid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final Context context;
    private final ArrayList<ProjectModel> projectList;
    private final OnProjectActionListener actionListener; // listener callback to communicate with activity

    public ProjectAdapter(Context context, ArrayList<ProjectModel> projectList, OnProjectActionListener actionListener) {
        this.context = context;
        this.projectList = projectList;
        this.actionListener = actionListener;
    }

    // interface to handle actions in the host activity
    public interface OnProjectActionListener {
        void onRenameSelected(int projectId, String currentName, int position);
        void onDeleteSelected(int projectId, int position);
    }

    // inflate row layout
    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.project_item, parent, false);
        return new ProjectViewHolder(view);
    }

    // bind data to views & handle item popup menu clicks
    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectModel project = projectList.get(position);

        holder.tvId.setText(project.getName());
        holder.tvDetails.setText("Grid: " + project.getGridSize() + "x" + project.getGridSize() + " | Opacity: " + project.getOpacity());

        // load image uri or fallback to gallery icon
        if (!project.getImageUri().isEmpty()) {
            try {
                holder.ivPreview.setImageURI(Uri.parse(project.getImageUri()));
            } catch (Exception e) {
                holder.ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // show popup options menu on row click
        holder.itemView.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.itemView);

            popup.getMenu().add(Menu.NONE, 1, 2, "Simple View");
            popup.getMenu().add(Menu.NONE, 2, 3, "Edit in Editor");
            popup.getMenu().add(Menu.NONE, 3, 4, "Delete Project");
            popup.getMenu().add(Menu.NONE, 4, 1, "Rename Project");

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: // option 1: pass bundle and open PreviewFragment
                        String uri = project.getImageUri();
                        int gridSize = project.getGridSize();
                        int opacity = project.getOpacity();
                        int color = project.getColor();
                        String projectName = project.getName();

                        PreviewFragment previewFragment = new PreviewFragment();
                        Bundle args = new Bundle();
                        args.putString("uri", uri);
                        args.putInt("gridSize", gridSize);
                        args.putInt("opacity", opacity);
                        args.putInt("color", color);
                        args.putString("projectName", projectName);
                        previewFragment.setArguments(args);

                        ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .replace(R.id.fragment_container, previewFragment)
                                .addToBackStack(null)
                                .commit();
                        return true;

                    case 2: // option 2: pass extras and launch EditorActivity
                        Intent intent = new Intent(context, EditorActivity.class);
                        intent.putExtra("IMAGE_URI", project.getImageUri());
                        intent.putExtra("GRID_SIZE", project.getGridSize());
                        intent.putExtra("OPACITY", project.getOpacity());
                        intent.putExtra("COLOR", project.getColor());
                        intent.putExtra("PROJECT_ID", project.getId());
                        context.startActivity(intent);
                        return true;

                    case 3: // option 3: notify activity to show delete prompt
                        if (actionListener != null) {
                            actionListener.onDeleteSelected(project.getId(), position);
                        }
                        return true;

                    case 4: // option 4: notify activity to show rename prompt
                        if (actionListener != null) {
                            actionListener.onRenameSelected(project.getId(), project.getName(), position);
                        }
                        return true;

                    default:
                        return false;
                }
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    // view holder definition
    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPreview;
        TextView tvId, tvDetails;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.iv_item_preview);
            tvId = itemView.findViewById(R.id.tv_item_name);
            tvDetails = itemView.findViewById(R.id.tv_item_details);
        }
    }
}