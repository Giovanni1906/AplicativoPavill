package com.example.Pavill.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.Pavill.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    private Context context;

    public interface RegisterCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public RegisterController(Context context) {
        this.context = context;
    }

    public void registerClient(
            String dni,
            String name,
            String email,
            String phoneNumber,
            String password,
            String identifier,
            String appVersion,
            RegisterCallback callback
    ) {

        // Ejecutar en un hilo separado
        new Thread(() -> {
            try {
                // Configurar la conexión
                String SERVICE_URL = getServiceUrl(context); // Obtiene la URL correcta

                URL url = new URL(SERVICE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Preparar los datos
                HashMap<String, String> postData = new HashMap<>();
                postData.put("Accion", "RegistrarCliente");
                postData.put("ClienteNumeroDocumento", dni);
                postData.put("ClienteNombre", name);
                postData.put("ClienteEmail", email);
                postData.put("ClienteCelular", phoneNumber);
                postData.put("ClienteContrasena", password);
                postData.put("ClienteNacionalidadId", "PER");
                postData.put("Identificador", identifier);
                postData.put("AppVersion", appVersion);

                // Escribir los datos en el cuerpo de la solicitud
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postData));
                writer.flush();
                writer.close();
                os.close();

                // Leer la respuesta
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                // Manejar la respuesta en el hilo principal
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String respuesta = jsonResponse.getString("Respuesta");

                        if (respuesta.equals("L001")) {
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(getErrorMessage(respuesta));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error al procesar la respuesta del servidor.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error de conexión."));
            }
        }).start();
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    private String getErrorMessage(String code) {
        switch (code) {
            case "L002": return "Error al registrar al cliente.";
            case "L003": return "El correo electrónico ya está registrado.";
            case "L040": return "Número de celular inválido.";
            case "L041": return "Dígito inicial del celular incorrecto.";
            default: return "Error desconocido.";
        }
    }

    private String getServiceUrl(Context context) {
        return context.getString(R.string.url_services); // Define la URL en strings.xml
    }
}
