package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapController {

    private SelectionState currentSelection = SelectionState.NONE;
    private static final String TAG = "MapController";

    public MapController() {
    }

    public enum SelectionState {
        ORIGIN,
        DESTINATION,
        NONE
    }

    public void toggleSelection(boolean isForOrigin) {
        currentSelection = isForOrigin
                ? (currentSelection == SelectionState.ORIGIN ? SelectionState.NONE : SelectionState.ORIGIN)
                : (currentSelection == SelectionState.DESTINATION ? SelectionState.NONE : SelectionState.DESTINATION);
    }

    public boolean isActiveSelection(boolean isForOrigin) {
        return (isForOrigin && currentSelection == SelectionState.ORIGIN) ||
                (!isForOrigin && currentSelection == SelectionState.DESTINATION);
    }

    public void fetchFavoriteDestinations(Context context, String clienteId, FavoriteDestinationsCallback callback) {
        String url = context.getString(R.string.url_services_fav);

        Log.d(TAG, "Iniciando solicitud para obtener destinos favoritos.");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "ClienteId: " + clienteId);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Respuesta recibida: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "D001":
                                JSONArray datos = jsonResponse.optJSONArray("Datos");
                                List<FavoriteDestination> favorites = new ArrayList<>();
                                if (datos != null) {
                                    for (int i = 0; i < datos.length(); i++) {
                                        JSONObject obj = datos.getJSONObject(i);
                                        FavoriteDestination destination = new FavoriteDestination(
                                                obj.optString("ClienteDestinoFavoritoDireccion"),
                                                obj.optDouble("ClienteDestinoFavoritoCoordenadaX", 0.0),
                                                obj.optDouble("ClienteDestinoFavoritoCoordenadaY", 0.0)
                                        );
                                        favorites.add(destination);
                                    }
                                    Log.d(TAG, "Número de destinos favoritos obtenidos: " + favorites.size());
                                }
                                callback.onFavoritesReceived(favorites);
                                break;

                            case "D002":
                                Log.w(TAG, "No se encontraron destinos favoritos para el cliente.");
                                callback.onNoFavoritesFound("No se encontraron destinos favoritos.");
                                break;

                            case "D003":
                                Log.e(TAG, "No se proporcionó un ClienteId válido.");
                                callback.onError("No se proporcionó un ClienteId.");
                                break;

                            default:
                                Log.e(TAG, "Respuesta desconocida del servidor: " + respuesta);
                                callback.onError("Respuesta desconocida del servidor.");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta del servidor.", e);
                        callback.onError("Error procesando la respuesta del servidor.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión con el servidor.", error);
                    callback.onError("Error de conexión con el servidor.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ObtenerClienteDestinoFavoritos");
                params.put("ClienteId", clienteId);
                Log.d(TAG, "Parámetros enviados: " + params.toString());
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    public class FavoriteDestination {
        private final String address;
        private final double latitude;
        private final double longitude;

        public FavoriteDestination(String address, double latitude, double longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    public interface FavoriteDestinationsCallback {
        void onFavoritesReceived(List<FavoriteDestination> favorites);

        void onNoFavoritesFound(String message);

        void onError(String errorMessage);
    }
}
