package com.example.canvagrid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.HelpViewHolder> {

    private final List<HelpItem> helpItems;

    public HelpAdapter(List<HelpItem> helpItems) {
        this.helpItems = helpItems;
    }

    // inflate the row layout
    @NonNull
    @Override
    public HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_help, parent, false);
        return new HelpViewHolder(view);
    }

    // bind data to row views
    @Override
    public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
        HelpItem item = helpItems.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDesc.setText(item.getDescription());

        // use fallback icon if resource id is 0
        if (item.getIconResId() != 0) {
            holder.ivIcon.setImageResource(item.getIconResId());
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_help);
        }
    }

    @Override
    public int getItemCount() {
        return helpItems.size();
    }

    // view holder definition
    static class HelpViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        ImageView ivIcon;

        public HelpViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_help_title);
            tvDesc = itemView.findViewById(R.id.tv_help_desc);
            ivIcon = itemView.findViewById(R.id.iv_help_icon);
        }
    }
}