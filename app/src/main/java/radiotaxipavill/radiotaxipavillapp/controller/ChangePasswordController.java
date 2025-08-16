package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordController {

    public interface ChangePasswordCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void changePassword(
            Context context,
            String newPassword,
            ChangePasswordCallback callback
    ) {
        // URL del servicio obtenida desde los recursos
        String SERVICE_URL = context.getString(R.string.url_services);

        // Obtener ClienteId del SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");
        String clienteNombre = sharedPreferences.getString("ClienteNombre", "");
        String clienteEmail = sharedPreferences.getString("ClienteEmail", "");
        String clienteCelular = sharedPreferences.getString("ClienteCelular", "");
        String identificador = sharedPreferences.getString("Identificador", "");

        // Validar si ClienteId está disponible
        if (clienteId.isEmpty()) {
            callback.onFailure("No se encontró información del usuario.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        // Crear solicitud HTTP POST
        StringRequest request = new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "L025": // Cambio de contraseña exitoso
                                callback.onSuccess("Contraseña cambiada con éxito.");
                                Log.d("ChangePasswordController", "Contraseña actualizada correctamente.");
                                break;

                            case "L026": // Error al cambiar la contraseña
                                callback.onFailure("No se pudo actualizar la contraseña. Inténtalo de nuevo.");
                                break;

                            case "L027": // ClienteId vacío o inválido
                                callback.onFailure("Error: ClienteId inválido.");
                                break;

                            default:
                                callback.onFailure("Error desconocido: " + respuesta);
                                break;
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
                params.put("Accion", "EditarClienteContrasena");
                params.put("ClienteId", clienteId);
                params.put("ClienteContrasena", newPassword);
                params.put("ClienteNombre", clienteNombre);
                params.put("ClienteEmail", clienteEmail);
                params.put("ClienteCelular", clienteCelular);
                params.put("Identificador", identificador);
                return params;
            }
        };

        queue.add(request);
    }
}
