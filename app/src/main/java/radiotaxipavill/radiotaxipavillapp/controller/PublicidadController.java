package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONObject;

public class PublicidadController {

    public interface PublicidadCallback {
        void onPublicidadReceived(String imageUrl);
        void onNoPublicidadFound(String message);
        void onError(String errorMessage);
    }

    public void fetchPublicidad(Context context, PublicidadCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");

        if (clienteId.isEmpty()) {
            callback.onError("ClienteId no encontrado en SharedPreferences.");
            return;
        }

        String baseUrl = context.getString(R.string.url_services_publicidad);
        String url = baseUrl + "?Accion=ObtenerPublicidadUltimo&ClienteId=" + clienteId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        if ("U003".equals(respuesta)) {
                            String imageUrl = jsonResponse.optString("PublicidadArchivo", "");
                            if (!imageUrl.isEmpty()) {
                                callback.onPublicidadReceived(imageUrl);
                            } else {
                                callback.onNoPublicidadFound("No se encontró la URL de la publicidad.");
                            }
                        } else {
                            callback.onNoPublicidadFound("No se encontraron promociones.");
                        }
                    } catch (Exception e) {
                        Log.e("PublicidadController", "Error procesando la respuesta: ", e);
                        callback.onError("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e("PublicidadController", "Error de conexión: ", error);
                    callback.onError("Problemas de conexión, intente de nuevo.");
                });

        Volley.newRequestQueue(context).add(request);
    }
}
