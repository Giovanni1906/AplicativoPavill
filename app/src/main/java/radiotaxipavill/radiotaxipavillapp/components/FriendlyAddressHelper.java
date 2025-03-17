package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class FriendlyAddressHelper {

    public interface AddressCallback {
        void onAddressRetrieved(String friendlyAddress);
        void onError(String errorMessage);
    }

    /**
     * Obtiene una dirección amigable a partir de coordenadas, usando Geocoder y Google Places.
     */
    public static void getFriendlyAddress(Context context, double latitude, double longitude, AddressCallback callback) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            // Intentar obtener dirección con Geocoder
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String street = address.getThoroughfare(); // Calle
                String neighborhood = address.getSubLocality(); // Vecindario
                String city = address.getLocality(); // Ciudad
                String plusCode = address.getExtras() != null ? address.getExtras().getString("plus_code") : null;

                String friendlyAddress;

                if (street != null && neighborhood != null) {
                    friendlyAddress = "Referencia: " + street + ", " + neighborhood;
                } else if (street != null) {
                    friendlyAddress = "Referencia: calle: " + street;
                } else if (neighborhood != null) {
                    friendlyAddress = "Referencia: vecindario: " + neighborhood;
                } else if (city != null) {
                    friendlyAddress = "Marcado en mapa, sin referencia cercana";
                    // Agregar subdivisiones si están disponibles
                    if (plusCode != null) {
                        getSubdivisionFromPlusCode(context, plusCode, new AddressCallback() {
                            @Override
                            public void onAddressRetrieved(String subdivision) {
                                callback.onAddressRetrieved(friendlyAddress + ", dentro de: " + subdivision);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                callback.onAddressRetrieved(friendlyAddress + " (sin subdivisión detallada)");
                            }
                        });
                        return; // Salir porque la respuesta se manejará en el callback
                    }
                } else {
                    friendlyAddress = "Marcado en mapa, dirección no disponible";
                }

                callback.onAddressRetrieved(friendlyAddress);
                return;
            }

            // Si no hay resultado con Geocoder, intentar con Google Places
            getNearbyPlaceUsingGooglePlaces(context, latitude, longitude, callback);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onError("Error al obtener la dirección");
        }
    }

    /**
     * Utiliza Google Places API para obtener un lugar cercano basado en coordenadas.
     */
    private static void getNearbyPlaceUsingGooglePlaces(Context context, double latitude, double longitude, AddressCallback callback) {
        String apiKey = context.getString(R.string.map_api_key); // API Key desde strings.xml
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude + "&radius=100&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray results = jsonResponse.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject firstPlace = results.getJSONObject(0);
                    String placeName = "Referencia por: " + firstPlace.getString("name");
                    callback.onAddressRetrieved(placeName);
                } else {
                    callback.onAddressRetrieved("Sin referencias cercanas");
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar los datos de Google Places");
            }
        }, error -> {
            error.printStackTrace();
            callback.onError("Error al obtener datos de Google Places");
        });

        queue.add(request);
    }

    /**
     * Obtiene una subdivisión a partir de un Plus Code utilizando la API de Google Maps Geocoding.
     */
    private static void getSubdivisionFromPlusCode(Context context, String plusCode, AddressCallback callback) {
        String apiKey = context.getString(R.string.map_api_key); // API Key desde strings.xml
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + plusCode + "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray results = jsonResponse.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject firstResult = results.getJSONObject(0);
                    JSONArray components = firstResult.getJSONArray("address_components");

                    StringBuilder subdivisionBuilder = new StringBuilder();
                    for (int i = 0; i < components.length(); i++) {
                        JSONObject component = components.getJSONObject(i);
                        JSONArray types = component.getJSONArray("types");

                        if (types.toString().contains("sublocality") || types.toString().contains("locality")) {
                            if (subdivisionBuilder.length() > 0) {
                                subdivisionBuilder.append(", ");
                            }
                            subdivisionBuilder.append(component.getString("long_name"));
                        }
                    }

                    String subdivision = subdivisionBuilder.toString();
                    if (!subdivision.isEmpty()) {
                        callback.onAddressRetrieved(subdivision);
                    } else {
                        callback.onAddressRetrieved("Sin subdivisión disponible");
                    }
                } else {
                    callback.onAddressRetrieved("No se encontró información para el Plus Code");
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar los datos de Google Maps");
            }
        }, error -> {
            error.printStackTrace();
            callback.onError("Error al realizar la solicitud");
        });

        queue.add(request);
    }
}
