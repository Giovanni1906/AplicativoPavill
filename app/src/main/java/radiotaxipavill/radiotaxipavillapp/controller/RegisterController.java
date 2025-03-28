package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.VolleySingleton;

public class RegisterController {

    public interface RegisterCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public void registerClient(
            Context context,
            String dni,
            String name,
            String email,
            String phoneNumber,
            String password,
            String identifier,
            String appVersion,
            RegisterCallback callback
    ) {
        String SERVICE_URL = context.getString(R.string.url_services); // Obtiene la URL correcta

        Map<String, String> params = new HashMap<>();
        params.put("Accion", "RegistrarCliente");
        params.put("ClienteNumeroDocumento", dni);
        params.put("ClienteNombre", name);
        params.put("ClienteEmail", email);
        params.put("ClienteCelular", phoneNumber);
        params.put("ClienteContrasena", password);
        params.put("ClienteNacionalidadId", "PER");
        params.put("Identificador", identifier);
        params.put("AppVersion", appVersion);

        Log.d("RegisterController", "🌍 Enviando solicitud a: " + SERVICE_URL);
        Log.d("RegisterController", "📨 Datos enviados: " + params.toString());

        VolleySingleton.getInstance(context).addToRequestQueue(new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    Log.d("RegisterController", "✅ Respuesta recibida: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.getString("Respuesta");

                        if ("L001".equals(respuesta) || "L003".equals(respuesta)) {
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(getErrorMessage(respuesta));
                        }
                    } catch (JSONException e) {
                        callback.onError("Problemas de conexión, intente nuevamente.");
                    }
                },
                error -> {
                    Log.e("RegisterController", "❌ Error en la solicitud: " + error.getMessage());
                    callback.onError("Error de conexión.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        });
    }

    private String getErrorMessage(String code) {
        switch (code) {
            case "L002": return "Error al registrar al cliente.";
            case "L040": return "Número de celular inválido.";
            case "L041": return "Dígito inicial del celular incorrecto.";
            default: return "Error desconocido.";
        }
    }
}
