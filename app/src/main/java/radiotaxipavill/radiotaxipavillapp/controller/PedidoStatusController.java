package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PedidoStatusController {

    TemporaryData temporaryData;
    public interface PedidoStatusCallback {
        void onStatusReceived(String status, String message);
        void onError(String errorMessage);
    }

    public void checkPedidoStatus(Context context, PedidoStatusCallback callback) {
        String url = context.getString(R.string.url_services_pedido);

        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados

        String pedidoId = temporaryData.getPedidoId();
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
                        int pedAbordoCliente = jsonResponse.optInt("PedAbordoCliente", -1);

                        switch (respuesta) {
                            case "P005":
                                // Pedido aprobado
                                assignConductorAndVehicleData(jsonResponse, context);
                                callback.onStatusReceived("ACEPTADO", "Unidad asignada. ¡Unidad asignada!");
                                Log.e("PedidoStatusController", "Pedido aceptado, respuesta: " + respuesta + " pedidoEstado: " + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                Log.d("PedidoStatusController", "respuesta: " + jsonResponse);
                                break;

                            case "P006":
                                if (pedidoEstado == 3) {
                                    // Pedido cancelado
                                    Log.e("PedidoStatusController", "El pedido fue cancelado, respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                    callback.onStatusReceived("CANCELADO", "El pedido fue cancelado, respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);

                                } else if (pedidoEstado == 1) {
                                    // Pedido En espera
                                    Log.e("PedidoStatusController", "Se está buscando un taxista, , respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                    Log.d("PedidoStatusController", "respuesta: " + jsonResponse);
                                    callback.onStatusReceived("EN_ESPERA", "Se está buscando un taxista, , respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                } else {
                                    // Pedido en espera
                                    Log.e("PedidoStatusController", "Pedido desconocido, verificar el estado; , respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                    Log.d("PedidoStatusController", "respuesta: " + jsonResponse);
                                    callback.onStatusReceived("DESCONOCIDO", "Pedido desconocido, verificar el estado; , respuesta: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                }
                                break;

                            default:
                                // Respuesta no esperada
                                callback.onError("Respuesta no reconocida: " + respuesta + "pedidoEstado" + pedidoEstado + " a BORDO: " + pedAbordoCliente);
                                Log.d("PedidoStatusController", "respuesta: " + jsonResponse);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onError("Problemas de conexión, intente de nuevo.");
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
                        TemporaryData temporaryData = TemporaryData.getInstance();
                        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados
                        temporaryData.setConductorFoto(conductorFoto, context);

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
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados

        temporaryData.setConductorId(jsonResponse.optString("ConductorId", "Desconocido"), context);
        temporaryData.setConductorNombre(jsonResponse.optString("ConductorNombre", "Desconocido"), context);
        temporaryData.setConductorTelefono(jsonResponse.optString("ConductorCelular", "N/A"), context);
        temporaryData.setUnidadPlaca(jsonResponse.optString("VehiculoPlaca", "N/A"), context);
        temporaryData.setUnidadModelo(jsonResponse.optString("VehiculoModelo", "N/A"), context);
        temporaryData.setUnidadColor(jsonResponse.optString("VehiculoColor", "N/A"), context);
        temporaryData.setUnidadCalificacion(jsonResponse.optString("ConductorCalificacion", "N/A"), context);
        temporaryData.setVehiculoUnidad(jsonResponse.optString("VehiculoUnidad", "N/A"), context);
        temporaryData.setOriginName(jsonResponse.optString("PedidoDireccion", "N/A"), context);
        temporaryData.setOriginReference(jsonResponse.optString("PedidoReferencia", "N/A"), context);
        Log.d("PedidoStatusController", "Datos conductor" + jsonResponse);
        Log.d("PedidoStatusController", "Foto de conductor inic" + jsonResponse.optString("ConductorFoto", ""));

    }
}