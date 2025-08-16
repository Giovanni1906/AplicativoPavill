package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DriverLocationController {

    public interface DriverLocationCallback {
        void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes);
        void onError(String errorMessage);
    }

    public void fetchDriverLocation(Context context, DriverLocationCallback callback) {
        String url = context.getString(R.string.url_services_ubicacion);
        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados

        String pedidoId = temporaryData.getPedidoId();
        String conductorId = temporaryData.getConductorId(); // Obtén el conductor ID del TemporaryData

        if (pedidoId == null || pedidoId.isEmpty() || conductorId == null || conductorId.isEmpty()) {
            callback.onError("Datos insuficientes para obtener la ubicación del conductor.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "U007": // Éxito
                                double lat = jsonResponse.optDouble("VehiculoCoordenadaX");
                                double lng = jsonResponse.optDouble("VehiculoCoordenadaY");
                                String orientation = jsonResponse.optString("VehiculoGPSOrientacion");
                                int estimatedTimeMinutes = jsonResponse.optInt("VehiculoVelocidad", 0);

                                callback.onLocationReceived(lat, lng, orientation, estimatedTimeMinutes);
                                break;

                            case "U008": // Sin ubicaciones
                                callback.onError("No se encontraron datos del conductor.");
                                break;

                            case "U009": // Falta de conductor ID
                                callback.onError("Identificador del conductor no válido.");
                                break;

                            default:
                                callback.onError("Respuesta desconocida: " + respuesta);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onError("Problemas de conexión, intente de nuevo.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ObtenerVehiculoCoordenadas");
                params.put("PedidoId", pedidoId);
                params.put("ConductorId", conductorId);
                return params;
            }
        };

        queue.add(request);
    }
}
