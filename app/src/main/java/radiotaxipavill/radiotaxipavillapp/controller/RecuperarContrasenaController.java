package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.VolleySingleton;

public class RecuperarContrasenaController {

    private static final String TAG = "RecuperarContrasenaCtrl";

    public interface RecuperarContrasenaCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    /**
     * Método para recuperar contraseña de un cliente.
     *
     * @param context  Contexto de la aplicación.
     * @param email    Email del cliente.
     * @param callback Callback para manejar las respuestas.
     */
    public void recuperarContrasena(Context context, String email, RecuperarContrasenaCallback callback) {
        // Base URL desde strings.xml
        String baseUrl = context.getString(R.string.url_services);

        // Construcción de la URL con los parámetros
        String url = baseUrl + "?Accion=RecuperarContrasena&ClienteEmail=" + email;

        Log.d(TAG, "URL: " + url);

        // Creación de la solicitud GET
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    Log.d(TAG, "Response: " + response);

                    // Procesar la respuesta
                    if (response.contains("L110")) {
                        callback.onSuccess("Correo enviado exitosamente.");
                    } else if (response.contains("L111")) {
                        callback.onFailure("Error al enviar el correo.");
                    } else if (response.contains("L113")) {
                        callback.onFailure("No se encontró un cliente con el email proporcionado.");
                    } else if (response.contains("L114")) {
                        callback.onFailure("El cliente está inactivo.");
                    } else {
                        callback.onFailure("Error inesperado: " + response);
                    }
                },
                error -> {
                    Log.e(TAG, "Error de conexión: ", error);
                    callback.onFailure("Problemas de conexión, intente de nuevo.");
                }
        );

        // Agregar la solicitud a la cola de Volley
        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }
}
