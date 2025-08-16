package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import org.json.JSONObject;

public class RatingController {

    public interface RatingCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void enviarCalificacion(
            Context context,
            String pedidoId,
            int calificacion,
            String comentario,
            RatingCallback callback
    ) {
        String SERVICE_URL = context.getString(R.string.url_services);

        calificacion = calificacion + 1;

        String url = SERVICE_URL + "?Accion=CalificarFinalizarPedido&PedidoId=" + pedidoId +
                "&PedidoCalificacion=" + calificacion + "&PedidoComentario=" + comentario;
        Log.d("RatingController", "enviado: " + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("L016".equals(respuesta)) {
                            callback.onSuccess("Calificación enviada con éxito.");
                            Log.d("RatingController", "enviarCalificacion: " + jsonResponse);
                        } else {
                            callback.onFailure("Error al calificar, codigo de error:" + respuesta);
                            Log.d("RatingController", "enviarCalificacion: " + jsonResponse);

                        }
                    } catch (Exception e) {
                        callback.onFailure("Error en la respuesta.");
                        Log.d("RatingController", "error interno ", e );

                    }
                },
                error -> callback.onFailure("Error de conexión.")
        );

        queue.add(request);
    }
}
