package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;

import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RequestTaxiController {

    public interface RequestTaxiCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    private static boolean isRequesting = false;
    private static final int REQUEST_RESET_DELAY = 8000; // Evita dobles solicitudes en 5s

    public void requestTaxi(
            Context context,
            String originAddress,
            String destinationAddress,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            String reference,
            Boolean saveFavorites,
            RequestTaxiCallback callback
    ) {
        Log.d("RequestTaxiController", "🚀 requestTaxi() ejecutado en: " + System.currentTimeMillis());

        if (isRequesting) {
            Log.d("RequestTaxiController", "Ya se está procesando un pedido. Ignorando nueva solicitud.");
            return;
        }

        isRequesting = true; // Marcar que estamos enviando una solicitud

        Log.d("RequestTaxiController", "Iniciando solicitud de taxi...");


        String SERVICE_URL = getServiceUrl(context); // Obtiene la URL del servicio

        //Obtén la tarifa del temporary
        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados

        String tarifa = temporaryData.getEstimatedCost();

        // Obtén los datos compartidos desde SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        String clienteId = sharedPreferences.getString("ClienteId", "");
        String clienteNumeroDocumento = sharedPreferences.getString("ClienteNumeroDocumento", "");
        String clienteNombre = sharedPreferences.getString("ClienteNombre", "");
        String clienteCelular = sharedPreferences.getString("ClienteCelular", "");
        String clienteEmail = sharedPreferences.getString("ClienteEmail", "");
        String identificador = sharedPreferences.getString("Identificador", "");
        String appVersion = context.getString(R.string.app_version_number);

        if (clienteId.isEmpty() || identificador.isEmpty()) {
            callback.onFailure("Información del cliente no encontrada.");
            isRequesting = false; // Liberar el flag
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, SERVICE_URL,
                response -> {
                    isRequesting = false; // Liberar el flag después de la respuesta
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String respuesta = jsonResponse.optString("Respuesta");

                        switch (respuesta) {
                            case "P001": // Pedido registrado correctamente
                                // Guarda datos en TemporaryData

                                temporaryData.setPedidoId(jsonResponse.optString("PedidoId"), context);
                                temporaryData.setOriginCoordinates(new LatLng(originLat, originLng), context);
                                temporaryData.setDestinationCoordinates(new LatLng(destinationLat, destinationLng), context);
                                temporaryData.setDestinationName(jsonResponse.optString("POST_PedidoDestino"), context);


                                callback.onSuccess("Pedido registrado con éxito.");
                                Log.d("RequestTaxiController", "Pedido registrado con éxito: " + jsonResponse);
                                break;

                            case "P002": // Error al registrar el pedido
                                callback.onFailure("Error al registrar el pedido. Por favor, inténtalo de nuevo.");
                                break;

                            case "P047": // Cliente con problema (estado inactivo)
                                callback.onFailure("No se puede registrar el pedido. El cliente tiene restricciones.");
                                break;

                            default:
                                callback.onFailure("Error desconocido: " + respuesta);
                                break;
                        }
                    } catch (Exception e) {
                        Log.e("RequestTaxiController", " Error al procesar la respuesta del servidor.", e);
                        e.printStackTrace();
                        callback.onFailure("Problemas de conexión, intente de nuevo.");
                    }
                },
                error -> {
                    Log.e("RequestTaxiController", " Error de conexión con el servidor.", error);
                    callback.onFailure("Problemas de conexión, intente de nuevo.");
                    resetRequestFlag();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("Accion", "RegistrarPedido");
                params.put("Plataforma", "ANDROID");
                params.put("ClienteId", clienteId);
                params.put("PedidoDireccion", originAddress);
                params.put("PedidoReferencia", reference);
                // 🚀 Solo enviar destino si no es null
                if (destinationLat != 00.00 || destinationLng != 00.00) {
                    params.put("PedidoDestino", destinationAddress);
                    params.put("PedidoDestinoCoordenadaX", String.valueOf(destinationLat));
                    params.put("PedidoDestinoCoordenadaY", String.valueOf(destinationLng));
                }
                params.put("PedidoCoordenadaX", String.valueOf(originLat));
                params.put("PedidoCoordenadaY", String.valueOf(originLng));
                params.put("ClienteNumeroDocumento", clienteNumeroDocumento);
                params.put("ClienteNombre", clienteNombre);
                params.put("ClienteCelular", clienteCelular);
                params.put("ClienteEmail", clienteEmail);
                if (saveFavorites) {
                    params.put("Favorito", "1");
                    Log.d("RequestTaxiController", "Se guardará en favoritos");
                } else {
                    params.put("Favorito", "2");
                    Log.d("RequestTaxiController", "no se guardará en favoritos");
                }
                params.put("Identificador", identificador);
                params.put("ClienteAppVersion", appVersion);
                if (tarifa != null && !tarifa.isEmpty() && !tarifa.equals("N/A")  ) {
                    float estimatedCostFloat = Float.parseFloat(tarifa);
                    if (estimatedCostFloat > 0.0f) {
                        params.put("PedidoPrecioServicioInicial", tarifa);
                        params.put("Precio", tarifa);
                    }
                }
                return params;
            }
        };

        // Configurar política para evitar reintentos automáticos
        request.setRetryPolicy(new DefaultRetryPolicy(
                8000, // Tiempo de espera antes de fallar
                0, // No reintentar automáticamente
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }

    private void resetRequestFlag() {
        new Handler().postDelayed(() -> {
            isRequesting = false;
            Log.d("RequestTaxiController", "🔄 Flag `isRequesting` liberado.");
        }, REQUEST_RESET_DELAY);
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services_pedido); // Define la URL en strings.xml
    }
}
