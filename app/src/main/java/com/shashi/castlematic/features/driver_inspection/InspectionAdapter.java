package com.shashi.castlematic.features.driver_inspection;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.shashi.castlematic.R;
import com.shashi.castlematic.features.driver_inspection.models.InspectionModels.InspectionItem;

import java.util.List;

public class InspectionAdapter extends RecyclerView.Adapter<InspectionAdapter.ViewHolder> {

    private List<InspectionItem> items;
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onCameraClick(InspectionItem item, int position);
        void onCheckChanged(InspectionItem item, boolean isChecked);
        void onRemarksChanged(InspectionItem item, String remarks);
    }

    public InspectionAdapter(List<InspectionItem> items, OnItemInteractionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inspection_checklist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InspectionItem item = items.get(position);

        // Show category header only for first item of each category
        if (position == 0 || !items.get(position - 1).category.equals(item.category)) {
            holder.categoryHeader.setVisibility(View.VISIBLE);
            holder.categoryHeader.setText(item.category);
        } else {
            holder.categoryHeader.setVisibility(View.GONE);
        }

        holder.title.setText(item.title);
        holder.description.setText(item.description);

        // FIXED: Remove listener before setting checked state to avoid triggering during bind
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(item.isChecked);

        // Show/hide remarks based on checkbox
        if (item.isChecked) {
            holder.remarksLayout.setVisibility(View.VISIBLE);
            holder.remarksInput.setText(item.remarks);
        } else {
            holder.remarksLayout.setVisibility(View.GONE);
        }

        // FIXED: Set checkbox listener AFTER setting the checked state
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                InspectionItem currentItem = items.get(currentPosition);
                currentItem.isChecked = isChecked;

                if (listener != null) {
                    listener.onCheckChanged(currentItem, isChecked);
                }

                // Use post to avoid modifying during layout computation
                holder.itemView.post(() -> {
                    notifyItemChanged(currentPosition);
                });
            }
        });

        // Camera button
        holder.cameraButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onCameraClick(items.get(currentPosition), currentPosition);
            }
        });

        // Remarks change listener
        holder.remarksInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    String remarks = holder.remarksInput.getText().toString();
                    InspectionItem currentItem = items.get(currentPosition);
                    currentItem.remarks = remarks;

                    if (listener != null) {
                        listener.onRemarksChanged(currentItem, remarks);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItemPhoto(int position, String photoPath) {
        if (position >= 0 && position < items.size()) {
            items.get(position).photoPath = photoPath;
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryHeader, title, description;
        CheckBox checkbox;
        ImageButton cameraButton;
        ImageView photoPreview;
        View remarksLayout;
        TextInputEditText remarksInput;

        ViewHolder(View view) {
            super(view);
            categoryHeader = view.findViewById(R.id.category_header);
            title = view.findViewById(R.id.item_title);
            description = view.findViewById(R.id.item_description);
            checkbox = view.findViewById(R.id.item_checkbox);
            cameraButton = view.findViewById(R.id.camera_button);
            photoPreview = view.findViewById(R.id.photo_preview);
            remarksLayout = view.findViewById(R.id.remarks_layout);
            remarksInput = view.findViewById(R.id.item_remarks);
        }
    }
}
