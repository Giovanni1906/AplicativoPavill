package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;

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

        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados


        // Obtener el PedidoId desde TemporaryData
        String pedidoId = temporaryData.getPedidoId();

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

                        if ("L019".equals(respuesta)) {
                            Log.d("CancelRequestController", "Cancelación de pedido: El pedido se canceló correctamente.");
                            callback.onSuccess("El pedido se canceló correctamente." + response);
                        } else {
                            Log.e("CancelRequestController", "Cancelación de pedido: No se pudo cancelar el pedido. Código: " + respuesta);
                            callback.onFailure("No se pudo cancelar el pedido. Código: " + respuesta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("CancelRequestController", "Cancelación de pedido: Error al procesar la respuesta del servidor.: ", e);
                        callback.onFailure("Problemas de conexión, intente de nuevo");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onFailure("Problemas de conexión, intente de nuevo");
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
