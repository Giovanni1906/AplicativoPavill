package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

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

    public interface FavoriteDestinationsCallback {
        void onFavoritesReceived(List<FavoriteDestination> origins, List<FavoriteDestination> destinations);
        void onNoFavoritesFound();
        void onError(String errorMessage);
    }

    public interface FavoriteDeleteCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public interface UpdateFavoritesCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void fetchFavoriteDestinations(Context context, String clienteId, boolean useMenuLayout, FavoriteDestinationsCallback callback) {
        String url = context.getString(R.string.url_services_fav);
        String action = useMenuLayout ? "ObtenerClienteDestinoFavoritosTotales" : "ObtenerClienteDestinoFavoritos";

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
                                List<FavoriteDestination> favoriteOrigins = new ArrayList<>();
                                List<FavoriteDestination> favoriteDestinations = new ArrayList<>();

                                if (datos != null) {
                                    for (int i = 0; i < datos.length(); i++) {
                                        JSONObject obj = datos.getJSONObject(i);

                                        String direccionOrigen = obj.optString("ClienteDestinoFavoritoDireccion", "");
                                        String reference = obj.optString("ClienteDestinoFavoritoReferencia", "");
                                        double latOrigen = obj.optDouble("ClienteDestinoFavoritoCoordenadaX", 0.0);
                                        double lngOrigen = obj.optDouble("ClienteDestinoFavoritoCoordenadaY", 0.0);

                                        int estado = obj.optInt("ClienteDestinoFavoritoEstado", -1); // Estado del favorito
                                        String id = obj.optString("ClienteDestinoFavoritoId", "");

                                        String direccionDestino = obj.optString("ClienteDestinoFavoritoDestino", "");
                                        double latDestino = obj.optDouble("ClienteDestinoFavoritoDestinoCoordenadaX", 0.0);
                                        double lngDestino = obj.optDouble("ClienteDestinoFavoritoDestinoCoordenadaY", 0.0);

                                        // Si tiene dirección y coordenadas válidas, se clasifica como ORIGEN
                                        if (!direccionOrigen.isEmpty() && latOrigen != 0.0 && lngOrigen != 0.0) {
                                            favoriteOrigins.add(new FavoriteDestination(id, direccionOrigen, latOrigen, lngOrigen, estado, reference));
                                        }

                                        // Si tiene dirección y coordenadas válidas, se clasifica como DESTINO
                                        if (!direccionDestino.isEmpty() && latDestino != 0.0 && lngDestino != 0.0) {
                                            favoriteDestinations.add(new FavoriteDestination(id, direccionDestino, latDestino, lngDestino, estado, reference));
                                        }
                                    }

                                    Log.d(TAG, "Número de favoritos ORIGEN obtenidos: " + favoriteOrigins.size());
                                    Log.d(TAG, "Número de favoritos DESTINO obtenidos: " + favoriteDestinations.size());
                                }

                                // Enviar las listas separadas al callback
                                callback.onFavoritesReceived(favoriteOrigins, favoriteDestinations);
                                break;

                            case "D002":
                                Log.w(TAG, "No se encontraron destinos favoritos para el cliente.");
                                callback.onNoFavoritesFound();
                                break;

                            case "D003":
                                Log.e(TAG, "No se proporcionó un ClienteId válido.");
                                callback.onError("No se proporcionó un ClienteId.");
                                break;

                            default:
                                Log.e(TAG, "Respuesta desconocida del servidor: " + respuesta);
                                callback.onError("Problemas de conexión, intente de nuevo.");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta del servidor.", e);
                        callback.onError("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión con el servidor.", error);
                    callback.onError("Problemas de conexión, intente de nuevo.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", action);
                params.put("ClienteId", clienteId);
                Log.d(TAG, "Parámetros enviados: " + params.toString());
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }


    public class FavoriteDestination {
        private final String id; // Agregar este campo para almacenar el ID
        private final String address;
        private final double latitude;
        private final double longitude;
        private final int estado;
        private final String reference;

        public FavoriteDestination(String id, String address, double latitude, double longitude, int estado, String reference) {
            this.id = id;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.estado = estado;
            this.reference = reference;
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

        public int getEstado() {
            return estado;
        }

        public String getId() {
            return id;
        }

        public String getReference() {
            return reference;
        }
    }

    public void deleteFavorite(Context context, String favoriteIds, FavoriteDeleteCallback callback) {
        String url = context.getString(R.string.url_services_fav);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Respuesta recibida: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "D004":
                                callback.onSuccess("Favorito eliminado exitosamente.");
                                break;

                            case "D005":
                                callback.onFailure("No se pudo eliminar el favorito.");
                                break;

                            case "D006":
                                callback.onFailure("No se proporcionó un ID de favorito válido.");
                                break;

                            default:
                                callback.onFailure("Problemas de conexión, intente de nuevo.");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta del servidor.", e);
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión con el servidor.", error);
                    callback.onFailure("Problemas de conexión, intente de nuevo.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "EliminarClienteDestinoFavorito");
                params.put("ClienteDestinoFavoritoId", favoriteIds);  // ID del favorito(s)
                params.put("Identificador", "XYZ"); // Si se requiere identificador
                Log.d(TAG, "Parámetros enviados: " + params.toString());
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    /**
     * Envía los favoritos seleccionados (estado 3) y no seleccionados (estado 4) al servidor
     */
    public void updateFavoriteStates(Context context, List<String> selectedIds, List<String> unselectedIds, UpdateFavoritesCallback callback) {
        String url = context.getString(R.string.url_services_fav);

        // Convertir listas en cadenas separadas por comas
        String selectedIdsString = TextUtils.join(",", selectedIds);
        String unselectedIdsString = TextUtils.join(",", unselectedIds);

        Log.d(TAG, "Enviando seleccionados (Estado 3): " + selectedIdsString);
        Log.d(TAG, "Enviando no seleccionados (Estado 4): " + unselectedIdsString);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Respuesta: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("E001".equals(respuesta)) {
                            callback.onSuccess("Actualización exitosa.");
                        } else {
                            callback.onFailure("Error en la actualización.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta del servidor.", e);
                        callback.onFailure("Error en el procesamiento de la respuesta.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión con el servidor.", error);
                    callback.onFailure("Problemas de conexión, intente de nuevo.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ActualizarEstadosMultiplesFavoritos");
                params.put("ClienteDestinoFavoritoIdsEstado3", selectedIdsString);
                params.put("ClienteDestinoFavoritoIdsEstado4", unselectedIdsString);

                Log.d(TAG, "Parámetros enviados: " + params.toString());
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

}
