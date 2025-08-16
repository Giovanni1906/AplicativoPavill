package radiotaxipavill.radiotaxipavillapp.components;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class PlacesAutoCompleteAdapter extends RecyclerView.Adapter<PlacesAutoCompleteAdapter.PredictionViewHolder> {

    private final List<AutocompletePrediction> predictionList;
    private final OnPlaceClickListener onPlaceClickListener;

    public PlacesAutoCompleteAdapter(List<AutocompletePrediction> predictionList, OnPlaceClickListener onPlaceClickListener) {
        this.predictionList = predictionList;
        this.onPlaceClickListener = onPlaceClickListener;
    }

    @NonNull
    @Override
    public PredictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PredictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PredictionViewHolder holder, int position) {
        AutocompletePrediction prediction = predictionList.get(position);
        holder.textViewPrediction.setText(prediction.getPrimaryText(null));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlaceClickListener.onPlaceClick(prediction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return predictionList.size();
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(AutocompletePrediction prediction);
    }

    public static class PredictionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPrediction;

        public PredictionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPrediction = itemView.findViewById(android.R.id.text1);
        }
    }
}
