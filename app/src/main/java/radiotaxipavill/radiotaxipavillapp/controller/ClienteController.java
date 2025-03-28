package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.VolleySingleton;

public class ClienteController {

    public interface EditarClienteCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void editarCliente(Context context, String clienteId, String clienteNumeroDocumento,
                              String clienteNombre, String clienteEmail, String clienteCelular,
                              EditarClienteCallback callback) {

        String baseUrl = context.getString(R.string.url_services);

        // Construir la URL con los parámetros
        String url = baseUrl + "?Accion=EditarCliente"
                + "&ClienteId=" + clienteId
                + "&ClienteNumeroDocumento=" + clienteNumeroDocumento
                + "&ClienteNombre=" + clienteNombre
                + "&ClienteEmail=" + clienteEmail
                + "&ClienteCelular=" + clienteCelular;

        Log.d("ClienteController", "URL: " + url);

        // Crear una solicitud GET
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ClienteController", "Response: " + response);

                        // Procesar la respuesta
                        if (response.contains("L009")) {
                            callback.onSuccess("Datos actualizados exitosamente.");
                        } else if (response.contains("L010")) {
                            callback.onFailure("Error al editar los datos en la base de datos.");
                        } else if (response.contains("L028")) {
                            callback.onFailure("ClienteId no válido.");
                        } else {
                            callback.onFailure("Error inesperado: " + response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("ClienteController", "Error: " + error.getMessage());
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                    }
                }
        );

        // Agregar la solicitud a la cola
        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }
}
