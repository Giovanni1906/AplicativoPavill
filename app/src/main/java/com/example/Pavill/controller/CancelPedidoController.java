package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CancelPedidoController {

    public interface CancelPedidoCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void cancelarPedido(
            Context context,
            String pedidoId,
            String pedidoCancelarMotivo,
            String pedidoComentario,
            CancelPedidoCallback callback
    ) {
        String SERVICE_URL = context.getString(R.string.url_services); // URL del servicio

        RequestQueue queue = Volley.newRequestQueue(context);

        String url = SERVICE_URL + "?Accion=CancelarPedidoMotivo&PedidoId=" + pedidoId +
                "&PedidoCancelarMotivo=" + pedidoCancelarMotivo + "&PedidoComentario=" + pedidoComentario;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "L022": // Cancelación exitosa
                                callback.onSuccess("Pedido cancelado con éxito.");
                                Log.d("CancelPedidoController", "cancelarPedido: " + jsonResponse );
                                break;
                            case "L023": // Error al editar datos
                                callback.onFailure("Error al cancelar el pedido.");
                                Log.d("CancelPedidoController", "error interno L023: " + jsonResponse );
                                break;
                            case "L024": // PedidoId no válido
                                callback.onFailure("Error: PedidoId no válido.");
                                Log.d("CancelPedidoController", "error interno L024: " + jsonResponse );

                                break;
                            default:
                                callback.onFailure("Error desconocido: " + respuesta);
                                Log.d("CancelPedidoController", "error interno ded conexión: " + jsonResponse );

                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure("Error al procesar la respuesta del servidor.");
                        Log.e("CancelPedidoController", "error de conexion: ", e );

                    }
                },
                error -> callback.onFailure("Error al conectar con el servidor.")
        );

        queue.add(request);
    }
}
