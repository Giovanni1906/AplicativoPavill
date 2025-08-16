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

public class RecibirMensajeController {

    public interface MensajeCallback {
        void onSuccess(String mensajeCliente);
        void onFailure(String errorMessage);
    }

    public void recibirMensaje(Context context, MensajeCallback callback) {
        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL del servicio

        // Obtén el PedidoId de TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados
        String pedidoId = temporaryData.getPedidoId();
        if (pedidoId == null || pedidoId.isEmpty()) {
            callback.onFailure("No hay un PedidoId disponible.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("P090".equals(respuesta)) {
                            String mensajeCliente = jsonResponse.optString("PedidoMensajeCliente");
                            callback.onSuccess(mensajeCliente);
                        } else {
                            callback.onFailure("Error al recibir mensaje. Código: " + respuesta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> callback.onFailure("Problemas de conexión, intente de nuevo.")
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "VerificarPedidoMensajeCliente");
                params.put("PedidoId", pedidoId);
                return params;
            }
        };

        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services_pedido);
    }
}
