package com.example.Pavill.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.TemporaryData;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RequestTaxiController {

    public interface RequestTaxiCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void requestTaxi(
            Context context,
            String originAddress,
            String destinationAddress,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            String reference,
            RequestTaxiCallback callback
    ) {
        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL del servicio

        // Obtén los datos compartidos desde SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        String clienteId = sharedPreferences.getString("ClienteId", "");
        String clienteNumeroDocumento = sharedPreferences.getString("ClienteNumeroDocumento", "");
        String clienteNombre = sharedPreferences.getString("ClienteNombre", "");
        String clienteCelular = sharedPreferences.getString("ClienteCelular", "");
        String clienteEmail = sharedPreferences.getString("ClienteEmail", "");
        String identificador = sharedPreferences.getString("Identificador", "");
        String appVersion = context.getString(R.string.app_version_number);

        if (clienteId.isEmpty() || identificador.isEmpty()) {
            callback.onFailure("Información del cliente no encontrada.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "P001": // Pedido registrado correctamente
                                // Guarda datos en TemporaryData
                                TemporaryData temporaryData = TemporaryData.getInstance();
                                temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados
                                temporaryData.setPedidoId(jsonResponse.optString("PedidoId"), context);
                                temporaryData.setOriginCoordinates(new LatLng(originLat, originLng), context);
                                temporaryData.setDestinationCoordinates(new LatLng(destinationLat, destinationLng), context);

                                callback.onSuccess("Pedido registrado con éxito.");
                                Log.d("RequestTaxiController", "Pedido registrado con éxito: " + jsonResponse);
                                break;

                            case "P002": // Error al registrar el pedido
                                callback.onFailure("Error al registrar el pedido. Por favor, inténtalo de nuevo.");
                                break;

                            case "P047": // Cliente con problema (estado inactivo)
                                callback.onFailure("No se puede registrar el pedido. El cliente tiene restricciones.");
                                break;

                            default:
                                callback.onFailure("Error desconocido: " + respuesta);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure("Error al procesar la respuesta del servidor.");
                    }
                },
                error -> callback.onFailure("Error al conectar con el servidor.")
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "RegistrarPedido");
                params.put("ClienteId", clienteId);
                params.put("PedidoDireccion", originAddress);
                params.put("PedidoReferencia", reference);
                params.put("PedidoDestino", destinationAddress);
                params.put("PedidoDestinoCoordenadaX", String.valueOf(destinationLat));
                params.put("PedidoDestinoCoordenadaY", String.valueOf(destinationLng));
                params.put("PedidoCoordenadaX", String.valueOf(originLat));
                params.put("PedidoCoordenadaY", String.valueOf(originLng));
                params.put("ClienteNumeroDocumento", clienteNumeroDocumento);
                params.put("ClienteNombre", clienteNombre);
                params.put("ClienteCelular", clienteCelular);
                params.put("ClienteEmail", clienteEmail);
                params.put("Favorito", "1");
                params.put("Identificador", identificador);
                params.put("PedidoCalculoTarifa", "1");
                params.put("ClienteAppVersion", appVersion);
                return params;
            }
        };

        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services_pedido); // Define la URL en strings.xml
    }
}
