package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class FavoriteController {

    private static final String TAG = "FavoriteController";

    public void addFavoriteDestination(Context context, String clienteId, String direccion, double coordenadaX, double coordenadaY) {
        new Thread(() -> {
            try {
                // Log de datos que se enviarán
                Log.d(TAG, "ClienteId: " + clienteId);
                Log.d(TAG, "Direccion: " + direccion);
                Log.d(TAG, "Coordenada X: " + coordenadaX);
                Log.d(TAG, "Coordenada Y: " + coordenadaY);

                // Validar ClienteId antes de enviar
                if (clienteId == null || clienteId.isEmpty()) {
                    showToast(context, "Tenemos problemas con su cuenta de usuario");
                    Log.e(TAG, "Error: ClienteId vacío.");
                    return;
                }

                // Construir la URL con parámetros
                String serviceUrl = context.getString(R.string.url_services_fav) +
                        "?Accion=RegistrarClienteDestinoFavorito" +
                        "&ClienteId=" + clienteId +
                        "&ClienteDestinoFavoritoDireccion=" + Uri.encode(direccion) +
                        "&ClienteDestinoFavoritoCoordenadaX=" + coordenadaX +
                        "&ClienteDestinoFavoritoCoordenadaY=" + coordenadaY;

                // Log de la URL
                Log.d(TAG, "URL construida: " + serviceUrl);

                // Crear conexión HTTP
                URL url = new URL(serviceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                // Leer la respuesta del servidor
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder responseBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    responseBuilder.append(scanner.nextLine());
                }
                scanner.close();

                String response = responseBuilder.toString();

                // Log de la respuesta
                Log.d(TAG, "Respuesta recibida: " + response);

                // Validar si la respuesta está vacía
                if (response.isEmpty()) {
                    showToast(context, "Problemas de conexión, intente de nuevo");
                    Log.e(TAG, "Respuesta del servidor vacía.");
                    return;
                }

                // Procesar la respuesta como JSON
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    handleResponse(context, jsonResponse);
                } catch (JSONException e) {
                    showToast(context, "Problemas de conexión, intente de nuevo.");
                    Log.e(TAG, "Error al procesar la respuesta JSON: " + e.getMessage(), e);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showToast(context, "Error al agregar destino favorito: " + e.getMessage());
                Log.e(TAG, "Error al realizar la solicitud", e);
            }
        }).start();
    }

    /**
     * Maneja la respuesta del servidor y muestra un Toast en función del código de respuesta.
     * @param context
     * @param jsonResponse
     */
    private void handleResponse(Context context, JSONObject jsonResponse) {
        try {
            String respuesta = jsonResponse.getString("Respuesta");
            Log.d(TAG, "Código de respuesta: " + respuesta);

            switch (respuesta) {
                case "D007": // Registro exitoso
                    showToast(context, "Destino favorito registrado correctamente.");
                    Log.i(TAG, "Registro exitoso. Código: " + respuesta);
                    break;

                case "D008": // Error al registrar destino favorito
                    showToast(context, "Ocurrió un error al registrar el destino favorito.");
                    Log.e(TAG, "Error al registrar destino favorito. Código: " + respuesta);
                    break;

                case "D009": // ClienteId vacío
                    showToast(context, "No se proporcionó un ClienteId válido.");
                    Log.e(TAG, "Error: ClienteId no proporcionado. Código: " + respuesta);
                    break;

                default: // Respuesta desconocida
                    showToast(context, "Problemas de conexión, intente de nuevo.");
                    Log.w(TAG, "Respuesta desconocida: " + respuesta);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast(context, "EProblemas de conexión, intente de nuevo.");
            Log.e(TAG, "Error al manejar la respuesta", e);
        }
    }

    private void showToast(Context context, String message) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        } else {
            Log.e(TAG, "Context no es una instancia de Activity. No se puede mostrar el Toast.");
        }
    }
}
