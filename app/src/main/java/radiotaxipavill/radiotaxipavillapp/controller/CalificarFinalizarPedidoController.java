package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONObject;

public class CalificarFinalizarPedidoController {

    private static final String TAG = "CalificarPedidoCtrl";

    public interface CalificarPedidoCallback {
        void onSuccess(String message, JSONObject response);
        void onFailure(String errorMessage);
    }

    /**
     * Método para calificar y finalizar un pedido.
     *
     * @param context      Contexto de la aplicación.
     * @param pedidoId     ID del pedido.
     * @param calificacion Calificación del pedido (entero).
     * @param comentario   Comentario del pedido.
     * @param coordenadaX  Coordenada X del cliente.
     * @param coordenadaY  Coordenada Y del cliente.
     * @param callback     Callback para manejar la respuesta.
     */
    public void calificarFinalizarPedido(Context context, String pedidoId, int calificacion, String comentario,
                                         double coordenadaX, double coordenadaY, CalificarPedidoCallback callback) {

        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String identificador = sharedPreferences.getString("Identificador", "");

        if (identificador.isEmpty()) {
            callback.onFailure("Problemas internos con el modelo de tu celular, favor de darle los permisos necesarios a la aplicación");
            return;
        }

        // Construir la URL con los parámetros
        String baseUrl = context.getString(R.string.url_services);
        String url = baseUrl + "?Accion=CalificarFinalizarPedido"
                + "&PedidoId=" + pedidoId
                + "&PedidoCalificacion=" + calificacion
                + "&PedidoComentario=" + comentario
                + "&ClienteCoordenadaX=" + coordenadaX
                + "&ClienteCoordenadaY=" + coordenadaY
                + "&Identificador=" + identificador;

        Log.d(TAG, "URL construida: " + url);

        // Crear una solicitud GET
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "Respuesta del servidor: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "L016":
                                callback.onSuccess("Pedido calificado y finalizado exitosamente.", jsonResponse);
                                break;
                            case "L017":
                                callback.onFailure("Error al calificar el pedido.");
                                break;
                            case "L018":
                                callback.onFailure("solicitud del pedido no encontrado.");
                                break;
                            default:
                                callback.onFailure("Problemas de conexión, intente nuevamente.");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar la respuesta: ", e);
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
