package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.Pavill.R;
import com.example.Pavill.components.VolleySingleton;

import java.util.HashMap;
import java.util.Map;

public class ClienteController {

    public interface EditarClienteCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void editarCliente(Context context, String clienteId, String clienteNumeroDocumento,
                              String clienteNombre, String clienteEmail, String clienteCelular,
                              EditarClienteCallback callback) {

        String url = context.getString(R.string.url_services);

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
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
                        callback.onFailure("Error de conexión al servidor.");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "EditarCliente");
                params.put("ClienteId", clienteId);
                params.put("ClienteNumeroDocumento", clienteNumeroDocumento);
                params.put("ClienteNombre", clienteNombre);
                params.put("ClienteEmail", clienteEmail);
                params.put("ClienteCelular", clienteCelular);
                return params;
            }
        };

        // Agregar la solicitud a la cola
        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }
}
