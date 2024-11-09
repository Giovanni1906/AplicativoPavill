package com.example.Pavill.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.Pavill.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetRatingDialog extends BottomSheetDialogFragment {

    private RatingListener ratingListener;
    private ImageView[] stars;

    public interface RatingListener {
        void onRatingSubmitted(int rating, String feedback);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RatingListener) {
            ratingListener = (RatingListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement RatingListener");
        }
    }

    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_rating, null);
        stars = new ImageView[] {
                view.findViewById(R.id.star1),
                view.findViewById(R.id.star2),
                view.findViewById(R.id.star3),
                view.findViewById(R.id.star4),
                view.findViewById(R.id.star5)
        };

        final EditText editTextFeedback = view.findViewById(R.id.editTextFeedback);
        final AppCompatButton btnSubmitRating = view.findViewById(R.id.btnSubmitRating);

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                setStarRating(index + 1);
            });
        }

        btnSubmitRating.setOnClickListener(v -> {
            int rating = getStarRating();
            String feedback = editTextFeedback.getText().toString().trim();
            if (rating == 0) {
                Toast.makeText(getContext(), "Por favor, selecciona una calificación", Toast.LENGTH_SHORT).show();
            } else {
                if (ratingListener != null) {
                    ratingListener.onRatingSubmitted(rating, feedback);
                    dismiss();
                }
            }
        });

        return new Dialog(requireContext(), getTheme());
    }

    private void setStarRating(int rating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < rating ? getResources().getColor(com.google.android.libraries.places.R.color.quantum_yellow) : getResources().getColor(com.google.android.libraries.places.R.color.quantum_grey));
        }
    }

    private int getStarRating() {
        int rating = 0;
        for (ImageView star : stars) {
            if (star.getColorFilter() != null) {
                rating++;
            }
        }
        return rating;
    }
}
