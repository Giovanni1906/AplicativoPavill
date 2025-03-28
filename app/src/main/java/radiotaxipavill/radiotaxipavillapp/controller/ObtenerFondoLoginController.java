package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONObject;

public class ObtenerFondoLoginController {

    private static final String TAG = "ObtenerFondoLoginCtrl";

    public interface FondoLoginCallback {
        void onSuccess(String urlImagen);
        void onFailure(String errorMessage);
    }

    public void obtenerFondoLogin(Context context, FondoLoginCallback callback) {
        String url = context.getString(R.string.url_services_publicidad) + "?Accion=ObtenerFondoLogin";

        Log.d(TAG, "URL de la solicitud: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");
                        String urlImagen = jsonResponse.optString("urlImagen", "");

                        if ("LG001".equals(respuesta)) {
                            callback.onSuccess(urlImagen);
                        } else {
                            callback.onFailure("Error: " + respuesta);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar la respuesta: ", e);
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión: ", error);
                    callback.onFailure("Problemas de conexión, intente de nuevo.");
                });

        Volley.newRequestQueue(context).add(request);
    }
}
