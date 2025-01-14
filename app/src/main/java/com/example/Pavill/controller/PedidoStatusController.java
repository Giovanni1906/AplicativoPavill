package com.example.Pavill.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;
import com.example.Pavill.components.TemporaryData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PedidoStatusController {

    public interface PedidoStatusCallback {
        void onStatusReceived(String status, String message);
        void onError(String errorMessage);
    }

    public void checkPedidoStatus(Context context, PedidoStatusCallback callback) {
        String url = context.getString(R.string.url_services_pedido);

        String pedidoId = TemporaryData.getInstance().getPedidoId();
        if (pedidoId == null || pedidoId.isEmpty()) {
            callback.onError("No se encontró un ID de pedido válido.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");
                        int pedidoEstado = jsonResponse.optInt("PedidoEstado", -1);

                        switch (respuesta) {
                            case "P005":
                                // Pedido aprobado
                                assignConductorAndVehicleData(jsonResponse, context);
                                callback.onStatusReceived("P005", "Unidad asignada. ¡Unidad asignada!");
                                break;

                            case "P006":
                                if (pedidoEstado == 3) {
                                    // Pedido cancelado
                                    Log.e("PedidoStatusController", "Pedido cancelado: " + pedidoEstado);
                                    callback.onStatusReceived("CANCELADO", "El pedido fue cancelado.");

                                } else if (pedidoEstado == 1) {
                                    // Pedido En espera
                                    Log.e("PedidoStatusController", "Pedido en espera: " + pedidoEstado);
                                    callback.onStatusReceived("EN_ESPERA", "Se está buscando un taxista");
                                } else {
                                    // Pedido en espera
                                    Log.e("PedidoStatusController", "PedidoDESCONOCIDO: " + pedidoEstado + respuesta);
                                    callback.onStatusReceived("EN_ESPERA", "Pedido en espera");

                                }
                                break;

                            default:
                                // Respuesta no esperada
                                callback.onError("Respuesta no reconocida: " + respuesta);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Error al procesar la respuesta del servidor.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onError("Error de conexión con el servidor.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ConsultarPedido");
                params.put("PedidoId", pedidoId);
                return params;
            }
        };

        queue.add(request);
    }

    /**
     * Obtiene la foto del conductor y la guarda en TemporaryData.
     * @param context
     * @param conductorId
     * @param callback
     */
    public void fetchConductorPhoto(Context context, String conductorId, FetchConductorPhotoCallback callback) {
        String url = context.getString(R.string.url_services_conductor);

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);

                        // Guarda la foto en TemporaryData
                        String conductorFoto = jsonResponse.optString("ConductorFoto", "Desconocido");
                        TemporaryData.getInstance().setConductorFoto(conductorFoto);

                        // Llama al callback de éxito
                        if (callback != null) {
                            callback.onSuccess(conductorFoto);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        // Llama al callback de error
                        if (callback != null) {
                            callback.onError("Error al parsear la respuesta");
                        }
                    }
                },
                error -> {
                    error.printStackTrace();

                    // Llama al callback de error
                    if (callback != null) {
                        callback.onError("Error en la solicitud de red");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "ObtenerConductor");
                params.put("ConductorId", conductorId);
                return params;
            }
        };

        queue.add(request);
    }

    // Interfaz del Callback
    public interface FetchConductorPhotoCallback {
        void onSuccess(String conductorFoto);
        void onError(String errorMessage);
    }


    /**
     * Asigna los datos del conductor y vehículo a TemporaryData si están disponibles.
     */
    private void assignConductorAndVehicleData(JSONObject jsonResponse, Context context) {
        TemporaryData temporaryData = TemporaryData.getInstance();

        temporaryData.setConductorId(jsonResponse.optString("ConductorId", "Desconocido"));
        temporaryData.setConductorNombre(jsonResponse.optString("ConductorNombre", "Desconocido"));
        temporaryData.setConductorTelefono(jsonResponse.optString("ConductorCelular", "N/A"));
        temporaryData.setUnidadPlaca(jsonResponse.optString("VehiculoPlaca", "N/A"));
        temporaryData.setUnidadModelo(jsonResponse.optString("VehiculoModelo", "N/A"));
        temporaryData.setUnidadColor(jsonResponse.optString("VehiculoColor", "N/A"));
        temporaryData.setUnidadCalificacion(jsonResponse.optString("ConductorCalificacion", "N/A"));
        Log.d("PedidoStatusController", "Foto de conductor inic" + jsonResponse.optString("ConductorFoto", ""));

    }
}