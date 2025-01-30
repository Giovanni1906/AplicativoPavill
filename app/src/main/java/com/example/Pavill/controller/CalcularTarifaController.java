package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;

import org.json.JSONObject;

public class CalcularTarifaController {

    private static final String TAG = "CalcularTarifaCtrl";

    public interface CalcularTarifaCallback {
        void onSuccess(double tarifarioMonto, String respuesta);
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
                        double tarifarioMonto = jsonResponse.optDouble("TarifarioMonto", 0.0);

                        // Manejo de respuesta basada en el código
                        switch (respuesta) {
                            case "P111":
                            case "P112":
                            case "P113":
                            case "P114":
                            case "P115":
                                callback.onSuccess(tarifarioMonto, respuesta);
                                break;
                            default:
                                callback.onFailure("Respuesta desconocida del servidor.");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando la respuesta: ", e);
                        callback.onFailure("Error al procesar la respuesta del servidor.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión: ", error);
                    callback.onFailure("Error de conexión con el servidor.");
                });

        // Agregar la solicitud a la cola de Volley
        Volley.newRequestQueue(context).add(request);
    }
}