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

public class EnviarMensajeController {

    public interface MensajeCallback {
        void onSuccess(String respuesta);
        void onFailure(String errorMessage);
    }

    public void enviarMensaje(Context context, String mensaje, MensajeCallback callback) {
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

                        if ("P087".equals(respuesta)) {
                            callback.onSuccess("Mensaje enviado correctamente.");
                        } else {
                            callback.onFailure("Error al enviar mensaje. Código: " + respuesta);
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
                params.put("Accion", "EnviarPedidoMensajeConductor");
                params.put("PedidoId", pedidoId);
                params.put("PedidoMensajeConductor", mensaje);
                return params;
            }
        };

        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services_pedido);
    }
}
