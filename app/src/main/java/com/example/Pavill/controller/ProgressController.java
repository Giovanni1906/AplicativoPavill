package com.example.Pavill.controller;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.Pavill.R;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.view.ProgressActivity;
import com.example.Pavill.view.RatingActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProgressController {

    private Context context;

    public ProgressController(Context context) {
        this.context = context;
    }

    // Método para finalizar el viaje
    public void finishTravel() {
        // Obtener datos necesarios desde TemporaryData
        TemporaryData tempData = TemporaryData.getInstance();
        String pedidoId = tempData.getPedidoId();
        String conductorId = tempData.getConductorId();

        // Validar que los datos necesarios estén disponibles
        if (pedidoId == null || conductorId == null) {
            Toast.makeText(context, "Faltan datos para finalizar el viaje.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                handleResponse(respuesta, jsonResponse);

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error al finalizar el viaje: " + e.getMessage());
            }
        }).start();
    }


    // Manejar las distintas respuestas del servidor
    private void handleResponse(String respuesta, JSONObject jsonResponse) {
        switch (respuesta) {
            case "L021": // Caso exitoso
                showToast("Viaje finalizado correctamente.");
                Log.e("ProgressController", "Pedido finalizado: " + respuesta);

                redirectToRating();
                break;

            case "L022": // Error al finalizar el pedido
                showToast("Error al finalizar el pedido. Intente nuevamente.");
                Log.e("ProgressController", "Pedido finalizado mal: " + respuesta);

                break;

            case "L023": // Error por falta de datos
                showToast("Faltan datos para finalizar el viaje.");
                Log.e("ProgressController", "Pedido finalizado mal, Sin datos: " + respuesta);

                break;

            default:
                showToast("Respuesta desconocida del servidor: " + respuesta);
                Log.e("ProgressController", "Pedido finalizado mal, servidor: " + respuesta);

                break;
        }
    }

    // Mostrar mensaje Toast en la UI principal
    private void showToast(String message) {
        // Necesario para ejecutar en el hilo principal
        ((ProgressActivity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    // Redirigir al usuario a la actividad de calificación
    private void redirectToRating() {
        Intent intent = new Intent(context, RatingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}