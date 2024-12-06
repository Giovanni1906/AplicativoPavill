package com.example.Pavill.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Pavill.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {
    private List<AutocompletePrediction> suggestions;
    private final OnSuggestionClickListener listener;

    public SuggestionsAdapter(List<AutocompletePrediction> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        AutocompletePrediction suggestion = suggestions.get(position);
        holder.bind(suggestion, listener);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public void updateSuggestions(List<AutocompletePrediction> newSuggestions) {
        suggestions.clear();
        suggestions.addAll(newSuggestions);
        notifyDataSetChanged();
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClick(AutocompletePrediction suggestion);
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewSuggestion;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSuggestion = itemView.findViewById(R.id.textViewSuggestion);
        }

        public void bind(AutocompletePrediction suggestion, OnSuggestionClickListener listener) {
            textViewSuggestion.setText(suggestion.getPrimaryText(null));
            itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
        }
    }
}
