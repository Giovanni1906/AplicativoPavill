package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import radiotaxipavill.radiotaxipavillapp.components.VolleySingleton;

import java.util.HashMap;
import java.util.Map;


public class PedidoController {

    public interface AbordoCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public void marcarAbordo(Context context, String pedidoId, String conductorId, double lat, double lng, int tardanza, AbordoCallback callback) {
        String url = context.getString(R.string.url_services_pedido);

        Map<String, String> params = new HashMap<>();
        params.put("Accion", "AbordoPedido");
        params.put("PedidoId", pedidoId);
        params.put("ConductorId", conductorId);
        params.put("VehiculoCoordenadaX", String.valueOf(lat));
        params.put("VehiculoCoordenadaY", String.valueOf(lng));
        params.put("Tardanza", String.valueOf(tardanza));

        VolleySingleton.getInstance(context).addToRequestQueue(new StringRequest(Request.Method.POST, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String respuesta = jsonObject.getString("Respuesta");

                switch (respuesta) {
                    case "P016":
                        callback.onSuccess("Cliente marcado como a bordo con éxito.");
                        Log.d("PedidoStatusController", "Marcado a bordo con exito: " + jsonObject);
                        break;
                    case "P017":
                        callback.onFailure("Error al actualizar el estado del pedido.");
                        break;
                    case "P018":
                        callback.onFailure("El pedido no está en un estado válido para la acción.");
                        break;
                    default:
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                        break;
                }
            } catch (JSONException e) {
                callback.onFailure("Problemas de conexión, intente de nuevo.");
            }
        }, error -> callback.onFailure("Error en la solicitud: " + error.getMessage())) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        });
    }
}
