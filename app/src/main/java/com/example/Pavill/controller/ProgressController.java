package com.example.Pavill.controller;

import android.content.Context;
import android.content.Intent;
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
                // URL del servicio (definido en strings.xml)
                String serviceUrl = context.getString(R.string.url_services_pedido);

                // Crear conexión HTTP
                URL url = new URL(serviceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);

                // Crear JSON con los datos a enviar
                JSONObject postData = new JSONObject();
                postData.put("Accion", "FinalizarViaje");
                postData.put("PedidoId", pedidoId);
                postData.put("ConductorId", conductorId);

                // Escribir datos en el cuerpo de la solicitud
                OutputStream os = connection.getOutputStream();
                os.write(postData.toString().getBytes("UTF-8"));
                os.close();

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
            case "P099": // Caso exitoso
                showToast("Viaje finalizado correctamente.");
                redirectToRating();
                break;

            case "P100": // Error al finalizar el pedido
                showToast("Error al finalizar el pedido. Intente nuevamente.");
                break;

            case "P101": // Error por falta de datos
                showToast("Faltan datos para finalizar el viaje.");
                break;

            default:
                showToast("Respuesta desconocida del servidor: " + respuesta);
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