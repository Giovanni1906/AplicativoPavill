package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.view.ProgressActivity;
import radiotaxipavill.radiotaxipavillapp.view.RatingActivity;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProgressController {

    private Context context;

    private LoadingDialog loadingDialog;


    /**
     * Constructor de la clase ProgressController.
     * @param context Contexto de la actividad actual.
     */
    public ProgressController(Context context) {
        this.context = context;
    }

    /**
     * Finaliza el viaje actual.
     */
    public void finishTravel() {
        loadingDialog = new LoadingDialog(context);

        // Obtener datos necesarios desde TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados

        String pedidoId = temporaryData.getPedidoId();
        String conductorId = temporaryData.getConductorId();

        // Validar que los datos necesarios estén disponibles
        if (pedidoId == null || conductorId == null) {
            Toast.makeText(context, "Faltan datos para finalizar el viaje.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Mostrar el diálogo de carga
        loadingDialog.show();

        // Crear un nuevo hilo para realizar la solicitud HTTP
        new Thread(() -> {
            try {
                // URL base del servicio (definido en strings.xml)
                String serviceUrl = context.getString(R.string.url_services);

                // Construir la URL completa con parámetros de consulta
                String fullUrl = serviceUrl + "?Accion=FinalizarViaje&PedidoId=" + pedidoId + "&ConductorId=" + conductorId;

                // Crear conexión HTTP
                URL url = new URL(fullUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); // Usar GET si el servicio espera parámetros en la URL

                // Leer la respuesta del servidor
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder responseBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    responseBuilder.append(scanner.nextLine());
                }
                scanner.close();

                // Procesar la respuesta
                String response = responseBuilder.toString();
                JSONObject jsonResponse = new JSONObject(response);
                String respuesta = jsonResponse.getString("Respuesta");

                ((ProgressActivity) context).runOnUiThread(() -> loadingDialog.dismiss());

                handleResponse(respuesta, jsonResponse);

            } catch (Exception e) {
                e.printStackTrace();
                // 🔹 Ocultar el loading dialog si ocurre un error
                ((ProgressActivity) context).runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(context, "Error al finalizar el viaje: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    /**
     * Maneja la respuesta del servidor para finalizar el viaje.
     * @param respuesta Respuesta del servidor.
     * @param jsonResponse Objeto JSON con la respuesta del servidor.
     */
    private void handleResponse(String respuesta, JSONObject jsonResponse) {
        switch (respuesta) {
            case "L021": // Caso exitoso
                showToast("Viaje finalizado correctamente.");
                Log.e("ProgressController", "Pedido finalizado: " + jsonResponse);
                redirectToRating(); // Redirigir a la actividad de RatingActivity
                break;

            case "L022": // Error al finalizar el pedido
                showToast("Error al finalizar el pedido. Intente nuevamente.");
                Log.e("ProgressController", "Pedido finalizado mal: " + jsonResponse);

                break;

            case "L023": // Error por falta de datos
                showToast("Faltan datos para finalizar el viaje.");
                Log.e("ProgressController", "Pedido finalizado mal, Sin datos: " + jsonResponse);

                break;

            default:
                showToast("Problemas con su conexión de internet: " + respuesta);
                Log.e("ProgressController", "Pedido finalizado mal, servidor: " + jsonResponse);

                break;
        }
    }

    /**
     * Muestra un mensaje Toast en la actividad actual.
     * @param message Mensaje a mostrar.
     */
    private void showToast(String message) {
        // Necesario para ejecutar en el hilo principal
        ((ProgressActivity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Redirige a la actividad de RatingActivity.
     */
    private void redirectToRating() {
        Intent intent = new Intent(context, RatingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}