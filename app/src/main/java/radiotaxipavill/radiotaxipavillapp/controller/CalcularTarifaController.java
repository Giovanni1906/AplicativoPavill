package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONObject;

public class CalcularTarifaController {

    private static final String TAG = "CalcularTarifaCtrl";

    public interface CalcularTarifaCallback {
        void onSuccess(String tarifarioMonto, String respuesta);
        void onFailure(String errorMessage);
    }

    /**
     * Calcula la tarifa aproximada entre dos ubicaciones.
     *
     * @param context      Contexto de la aplicación.
     * @param originLat    Latitud del origen.
     * @param originLng    Longitud del origen.
     * @param destLat      Latitud del destino.
     * @param destLng      Longitud del destino.
     * @param callback     Callback para manejar las respuestas.
     */
    public void calcularTarifa(Context context, double originLat, double originLng, double destLat, double destLng, CalcularTarifaCallback callback) {
        String baseUrl = context.getString(R.string.url_services_pedido); // URL base desde strings.xml
        String url = baseUrl + "?Accion=CalcularTarifa"
                + "&PedidoCoordenadaX=" + originLat
                + "&PedidoCoordenadaY=" + originLng
                + "&PedidoDestinoCoordenadaX=" + destLat
                + "&PedidoDestinoCoordenadaY=" + destLng;

        Log.d(TAG, "URL: " + url);

        // Crear una solicitud GET
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        String tarifarioMonto = "N/A";
                        if (jsonResponse.has("TarifarioMonto") && !jsonResponse.isNull("TarifarioMonto")) {
                            tarifarioMonto = jsonResponse.optString("TarifarioMonto", "N/A");
                        }

                        if (respuesta.equals("P111") || respuesta.equals("P112") || respuesta.equals("P113") || respuesta.equals("P114") || respuesta.equals("P115")) {
                            Log.d(TAG, "Respuesta: " + jsonResponse);
                            callback.onSuccess(tarifarioMonto, respuesta);
                        } else {
                            callback.onFailure("Problemas de conexión, intente nuevamente");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta: ", e);
                        callback.onFailure("Problemas de conexión, intente nuevamente.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión: ", error);
                    callback.onFailure("Problemas de conexión, intente nuevamente.");
                });

        // Agregar la solicitud a la cola de Volley
        Volley.newRequestQueue(context).add(request);
    }
}