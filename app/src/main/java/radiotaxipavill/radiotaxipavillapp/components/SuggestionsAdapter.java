package radiotaxipavill.radiotaxipavillapp.components;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import radiotaxipavill.radiotaxipavillapp.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {

    private List<AutocompletePrediction> suggestions;
    private final OnSuggestionClickListener listener;
    private final OnSuggestionSelectedListener selectedListener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(AutocompletePrediction suggestion);
    }

    public interface OnSuggestionSelectedListener {
        void onSuggestionSelected();
    }

   /* public SuggestionsAdapter(List<AutocompletePrediction> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
        this.selectedListener = null;
    }*/

    public SuggestionsAdapter(List<AutocompletePrediction> suggestions, OnSuggestionClickListener listener, OnSuggestionSelectedListener selectedListener) {
        this.suggestions = suggestions;
        this.listener = listener;
        this.selectedListener = selectedListener;
    }

    public void updateSuggestions(List<AutocompletePrediction> newSuggestions) {
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        AutocompletePrediction suggestion = suggestions.get(position);
        holder.bind(suggestion, listener, selectedListener);
    }

    @Override
    public int getItemCount() {
        return suggestions != null ? Math.min(suggestions.size(), 2) : 0; // Mostrar máximo 3 sugerencias
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView primaryText;
        private final TextView secondaryText;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            primaryText = itemView.findViewById(R.id.primary_text);
            secondaryText = itemView.findViewById(R.id.secondary_text);
        }

        public void bind(AutocompletePrediction suggestion, OnSuggestionClickListener listener, OnSuggestionSelectedListener selectedListener) {
            primaryText.setText(suggestion.getPrimaryText(null));
            secondaryText.setText(suggestion.getSecondaryText(null));

            itemView.setOnClickListener(v -> {
               // Log.e("itemView", "setOnClickListener");
               listener.onSuggestionClick(suggestion);
                if (selectedListener != null) {
                    selectedListener.onSuggestionSelected();
                }
            });
        }
    }
}
