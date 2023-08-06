package com.example.elderfit;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FitnessDataAdapter extends RecyclerView.Adapter<FitnessDataAdapter.ViewHolder> {
    private String[] fitnessNames;
    private int[] fitnessIcons;
    private String[][] fitnessData;

    public FitnessDataAdapter(String[] fitnessNames, int[] fitnessIcons, String[][] fitnessData) {
        this.fitnessNames = fitnessNames;
        this.fitnessIcons = fitnessIcons;
        this.fitnessData = fitnessData;
    }


    public void updateData(String[][] newData) {
        if (newData != null && newData.length == fitnessData.length) {
            System.arraycopy(newData, 0, fitnessData, 0, newData.length);
            notifyDataSetChanged();
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fitness_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = fitnessNames[position];
        int iconResId = fitnessIcons[position];
        String[] data = fitnessData[position];

        holder.nameTextView.setText(name);
        holder.iconImageView.setImageResource(iconResId);

        // Format the fitness data as needed, e.g., concatenate all data points into a single string
        StringBuilder dataBuilder = new StringBuilder();
        for (String dataPoint : data) {
            dataBuilder.append(dataPoint).append("\n"); // Add a new line between data points
        }
        holder.valueTextView.setText(dataBuilder.toString());
    }

    @Override
    public int getItemCount() {
        return fitnessNames.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private TextView valueTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.icon);
            nameTextView = itemView.findViewById(R.id.name);
            valueTextView = itemView.findViewById(R.id.value);
        }
    }
}
