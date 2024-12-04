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

    public MapController() {

    }

    public enum SelectionState {
        ORIGIN,
        DESTINATION,
        NONE
    }



    public void toggleSelection(boolean isForOrigin) {
        if (isForOrigin) {
            currentSelection = (currentSelection == SelectionState.ORIGIN) ? SelectionState.NONE : SelectionState.ORIGIN;
        } else {
            currentSelection = (currentSelection == SelectionState.DESTINATION) ? SelectionState.NONE : SelectionState.DESTINATION;
        }
    }

    public boolean isActiveSelection(boolean isForOrigin) {
        return (isForOrigin && currentSelection == SelectionState.ORIGIN) ||
                (!isForOrigin && currentSelection == SelectionState.DESTINATION);
    }

    public void fetchFavoriteDestinations(Context context, String clienteId, FavoriteDestinationsCallback callback) {
        String url = context.getString(R.string.url_services_fav);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("D001".equals(respuesta)) {
                            JSONArray datos = jsonResponse.optJSONArray("Datos");
                            List<FavoriteDestination> favorites = new ArrayList<>();
                            if (datos != null) {
                                for (int i = 0; i < datos.length(); i++) {
                                    JSONObject obj = datos.getJSONObject(i);
                                    FavoriteDestination destination = new FavoriteDestination(
                                            obj.optString("ClienteDestinoFavoritoDireccion"),
                                            obj.optDouble("ClienteDestinoFavoritoCoordenadaX"),
                                            obj.optDouble("ClienteDestinoFavoritoCoordenadaY")
                                    );
                                    favorites.add(destination);
                                }
                            }
                            callback.onFavoritesReceived(favorites);
                        } else if ("D002".equals(respuesta)) {
                            Log.e("MapController", "Fav: No se encontraron destinos favoritos. ");
                            callback.onNoFavoritesFound("No se encontraron destinos favoritos.");
                        } else if ("D003".equals(respuesta)) {
                            Log.e("MapController", "Fav: No se proporcionó un ClienteId.");
                            callback.onError("No se proporcionó un ClienteId.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("MapController", "Fav: No se encontraron destinos favoritos. ", e);
                        callback.onError("Error al procesar la respuesta del servidor.");
                    }
                },
                error -> {
                    error.printStackTrace();

                    callback.onError("Error de conexión con el servidor.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ObtenerClienteDestinoFavoritos");
                params.put("ClienteId", clienteId);
                params.put("RegionId", "REG-2");
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