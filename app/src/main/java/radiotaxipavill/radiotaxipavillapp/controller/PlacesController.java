package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.Places;

import java.util.Arrays;
import java.util.List;

public class PlacesController {

    private final PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;

    /**
     * Interfaz para recibir predicciones de direcciones
     */
    public interface PredictionsCallback {
        void onPredictionsReceived(List<AutocompletePrediction> predictions);
    }

    /**
     * Constructor de PlacesController
     * @param context
     */
    public PlacesController(Context context) {
        this.placesClient = Places.createClient(context);
    }

    /**
     * Obtiene predicciones de direcciones desde el servicio de Places API
     * @param query
     * @param callback
     */
    public void getPredictions(String query, PredictionsCallback callback) {
        if (sessionToken == null) {
            sessionToken = AutocompleteSessionToken.newInstance();
        }

        // Define los límites geográficos de Tacna, Perú
        RectangularBounds boundsTacna = RectangularBounds.newInstance(
                new LatLng(-18.0569, -70.3191), // Suroeste (latitud y longitud mínima)
                new LatLng(-17.8800, -69.9800)  // Noreste (latitud y longitud máxima)
        );

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .setCountries("PE") // Limitar a Perú
                .setLocationRestriction(boundsTacna) // Limitar a los límites de Tacna
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    callback.onPredictionsReceived(response.getAutocompletePredictions());
                })
                .addOnFailureListener(exception -> {
                    Log.e("PlacesController", "Error obteniendo predicciones: " + exception.getMessage());
                });
    }


    /**
     * Obtiene LatLng a partir de un PlaceId
     * @param placeId
     * @param callback
     */
    public void getLatLngFromPlaceId(String placeId, LatLngCallback callback) {
        // Configurar los campos necesarios
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            if (place.getLatLng() != null) {
                callback.onLatLngFetched(place.getLatLng());
            } else {
                callback.onError(new Exception("No se pudo obtener LatLng del PlaceId"));
            }
        }).addOnFailureListener(callback::onError);
    }

    public interface LatLngCallback {
        void onLatLngFetched(LatLng latLng);
        void onError(Exception exception);
    }

}
