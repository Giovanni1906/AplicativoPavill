package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthController {


    public interface Callback {
        void onSuccess(JSONObject clientData);
        void onFailure(String errorMessage);
    }

    public void login(Context context, String email, String password, String deviceId, Callback callback) {
        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL correcta
        String url = SERVICE_URL +
                "?Accion=AccederCliente" +
                "&ClienteEmail=" + email +
                "&ClienteContrasena=" + password +
                "&Identificador=" + deviceId;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String respuesta = jsonResponse.getString("Respuesta");

                            switch (respuesta) {
                                case "L011": // Credenciales válidas
                                    Log.d("AuthController", "Credenciales válidas. Accediendo..." + jsonResponse);
                                    callback.onSuccess(jsonResponse);
                                    Log.d("AuthController", "Credenciales válidas. Acceso correcto" + respuesta);
                                    break;
                                case "L013": // Credenciales incorrectas
                                    callback.onFailure("Correo electrónico o contraseña incorrecta.");
                                    Log.d("AuthController", "Credenciales incorrectas. Acceso denegado: " + respuesta);

                                    break;
                                case "L036": // Cliente bloqueado o desactivado
                                    callback.onFailure("Tu cuenta está bloqueada o desactivada.");
                                    Log.d("AuthController", "Cuenta bloqueada o desactivada. Acceso denegado: " + respuesta);
                                    break;
                                default: // Otros errores
                                    callback.onFailure("Error desconocido: " + respuesta);
                                    Log.d("AuthController", "Error desconocido. Acceso denegado");

                                    break;
                            }
                        } catch (JSONException e) {
                            Log.e("AuthController", "Error al procesar la respuesta del servidor "+ response +": ", e);
                            callback.onFailure("Problemas de conexión, intente nuevamente");

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("AuthController", "Verifica tu conexión e intenta nuevamente." + error.getMessage());
                        callback.onFailure("Verifica tu conexión e intenta nuevamente.");
                    }
                }
        );

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services); // Define la URL en strings.xml
    }
}
