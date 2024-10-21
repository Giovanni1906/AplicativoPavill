package com.example.Pavill.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {

    private List<AutocompletePrediction> suggestions;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(AutocompletePrediction prediction);
    }

    public SuggestionsAdapter(List<AutocompletePrediction> suggestions, OnItemClickListener onItemClickListener) {
        this.suggestions = suggestions;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        AutocompletePrediction prediction = suggestions.get(position);
        holder.textViewSuggestion.setText(prediction.getPrimaryText(null));
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(prediction));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSuggestion;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSuggestion = itemView.findViewById(android.R.id.text1);
        }
    }
}
