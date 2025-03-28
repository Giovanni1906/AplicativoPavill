package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.MyClusterItem;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyTaxisController {

    public interface NearbyTaxisCallback {
        void onTaxisReceived(List<MyClusterItem> taxis);
        void onNoTaxisFound(String message);
        void onError(String errorMessage);
    }

    public void fetchNearbyTaxis(Context context, LatLng currentLocation, NearbyTaxisCallback callback) {
        String url = context.getString(R.string.url_services_mapa);

        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");
        String regionId = "REG-2"; // RegionId fijo como "REG-2"

        if (clienteId.isEmpty() || currentLocation == null) {
            callback.onError("Faltan parámetros para realizar la solicitud.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("M004".equals(respuesta)) {
                            JSONArray datos = jsonResponse.optJSONArray("Datos");
                            if (datos != null && datos.length() > 0) {
                                List<MyClusterItem> taxis = new ArrayList<>();
                                for (int i = 0; i < datos.length(); i++) {
                                    try {
                                        JSONObject taxi = datos.getJSONObject(i);

                                        Log.d("NearbyTaxisController", "Taxi recibido: " + taxi);
                                        double lat = taxi.optDouble("DestinoCoordenadaX", Double.NaN);
                                        double lng = taxi.optDouble("DestinoCoordenadaY", Double.NaN);
                                        String nombre = (3 + (int) (Math.random() * 3)) + "✮";

                                        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                                            MyClusterItem taxiMarker = new MyClusterItem(lat, lng, nombre, "Activo");
                                            taxis.add(taxiMarker);
                                        } else {
                                            Log.w("NearbyTaxisController", "Taxi con coordenadas inválidas: " + taxi);
                                        }
                                    } catch (Exception e) {
                                        Log.e("NearbyTaxisController", "Error al procesar un taxi: ", e);
                                    }
                                }

                                if (!taxis.isEmpty()) {
                                    callback.onTaxisReceived(taxis);
                                } else {
                                    callback.onNoTaxisFound("No se encontraron taxis válidos en los datos recibidos.");
                                }
                            } else {
                                callback.onNoTaxisFound("No se encontraron taxis cercanos.");
                            }
                        } else if ("M005".equals(respuesta)) {
                            callback.onNoTaxisFound("No se encontraron taxis cercanos.");
                        } else if ("M006".equals(respuesta)) {
                            callback.onError("Faltan parámetros en la solicitud.");
                        } else {
                            callback.onError("Respuesta no reconocida: " + respuesta);
                        }
                    } catch (Exception e) {
                        Log.e("NearbyTaxisController", "Error al procesar la respuesta del servidor: ", e);
                        callback.onError("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e("NearbyTaxisController", "Error de conexión con el servidor: ", error);
                    callback.onError("Problemas de conexión, intente de nuevo.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ObtenerMapaCliente");
                params.put("ClienteId", clienteId);
                params.put("CoordenadaX", String.valueOf(currentLocation.latitude));
                params.put("CoordenadaY", String.valueOf(currentLocation.longitude));
                params.put("RegionId", regionId);

                Log.d("NearbyTaxisController", "Enviando parámetros: " + params.toString());
                return params;
            }
        };
        queue.add(request);
    }
}