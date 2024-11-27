package com.example.Pavill.controller;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;
import com.example.Pavill.components.TemporaryData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CancelRequestController {

    public interface CancelRequestCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void cancelRequest(Context context, CancelRequestCallback callback) {
        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL correcta

        // Obtener el PedidoId desde TemporaryData
        String pedidoId = TemporaryData.getInstance().getPedidoId();

        if (pedidoId == null || pedidoId.isEmpty()) {
            callback.onFailure("No se encontró un ID de pedido válido para cancelar.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("P001".equals(respuesta)) {
                            callback.onSuccess("El pedido se canceló correctamente.");
                        } else {
                            callback.onFailure("No se pudo cancelar el pedido. Código: " + respuesta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure("Error al procesar la respuesta del servidor.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onFailure("Error de conexión con el servidor.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "CancelarPedido");
                params.put("PedidoId", pedidoId);
                return params;
            }
        };

        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services); // Define la URL en strings.xml
    }
}
