package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.Places;

import java.util.List;

public class PlacesController {

    private final PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;

    public interface PredictionsCallback {
        void onPredictionsReceived(List<AutocompletePrediction> predictions);
    }

    public PlacesController(Context context) {
        this.placesClient = Places.createClient(context);
    }

    public void getPredictions(String query, PredictionsCallback callback) {
        if (sessionToken == null) {
            sessionToken = AutocompleteSessionToken.newInstance();
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .setCountries("PE") // Limitar a Perú
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    callback.onPredictionsReceived(response.getAutocompletePredictions());
                })
                .addOnFailureListener(exception -> {
                    Log.e("PlacesController", "Error obteniendo predicciones: " + exception.getMessage());
                });
    }
}
