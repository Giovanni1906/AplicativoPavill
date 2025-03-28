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

import org.json.JSONObject;

public class PhoneVerificationController {

    public interface Callback {
        void onSuccess(String verificationCode, String phoneNumber);
        void onFailure(String errorMessage);
    }

    public void verifyPhone(Context context, String phoneNumber, String deviceId, String appVersion, Callback callback) {
        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL correcta
        // Construir la URL con los parámetros requeridos
        String url = SERVICE_URL +
                "?ClienteCelular=" + phoneNumber +
                "&Identificador=" + deviceId +
                "&ClienteAppVersion=" + appVersion +
                "&Accion=VerificarCelular";

        // Configurar la solicitud HTTP GET
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // Parsear la respuesta
                            JSONObject jsonResponse = new JSONObject(response);
                            String respuesta = jsonResponse.getString("Respuesta");
                            Log.d("PhoneVerification", "onResponse: " + response);
                            if ("L050".equals(respuesta)) {
                                String verificationCode = jsonResponse.getString("ClienteCodigoVerificacion");
                                String fullPhoneNumber = jsonResponse.getString("ClienteCelularCompleto");
                                callback.onSuccess(verificationCode, fullPhoneNumber);
                            } else {
                                String mensaje = jsonResponse.optString("Mensaje", "Error desconocido.");
                                callback.onFailure(mensaje);
                            }
                        } catch (Exception e) {
                            callback.onFailure("Problemas de conexión, intente nuevamente.");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("PhoneVerification", "Error en la solicitud: " + error.getMessage());
                        callback.onFailure("No se pudo conectar al servicio.");
                    }
                }
        );

        // Agregar la solicitud a la cola
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services); // Define la URL en strings.xml
    }
}
