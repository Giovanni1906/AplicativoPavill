package com.example.Pavill.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.Pavill.controller.PedidoStatusController;

public class PedidoStatusService extends Service {

    private static final String TAG = "PedidoStatusService";
    private static final long CHECK_INTERVAL = 3000; // Intervalo de verificación en milisegundos
    private Handler handler;
    private Runnable statusChecker;

    // Variable para manejar el subestado del estado "ACEPTADO"
    private String subEstadoAceptado = "ESPERA_CONDUCTOR"; // Subestado inicial predeterminado
    private String pedidoStatus = "EN_ESPERA"; // Estado inicial predeterminado

    private boolean isServiceStopped = false; // Bandera para controlar la ejecución del servicio

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        isServiceStopped = false; // Asegúrate de reiniciar la bandera al crear el servicio
        startCheckingPedidoStatus();
        Log.d(TAG, "PedidoStatusService iniciado.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("nuevoSubEstado")) {
            String nuevoSubEstado = intent.getStringExtra("nuevoSubEstado");
            cambiarSubEstadoAceptado(nuevoSubEstado); // Llama al método para cambiar el subestado
        }

        return START_STICKY; // Permite que el servicio se reinicie automáticamente si se detiene
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStatusChecker(); // Detener el Runnable y limpiar el Handler
        Log.d(TAG, "PedidoStatusService destruido.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Inicia el chequeo periódico del estado del pedido.
     */
    private void startCheckingPedidoStatus() {
        statusChecker = new Runnable() {
            @Override
            public void run() {
                if (isServiceStopped) return; // No ejecutar si el servicio está detenido

                new PedidoStatusController().checkPedidoStatus(PedidoStatusService.this, new PedidoStatusController.PedidoStatusCallback() {
                    @Override
                    public void onStatusReceived(String status, String message) {
                        Log.d(TAG, "Estado del pedido recibido: " + status + " - " + message);

                        pedidoStatus = status; // Actualiza el estado global
                        switch (status) {
                            case "EN_ESPERA": // Pedido en espera
                                Log.d(TAG, "Estado: EN_ESPERA. Buscando conductor...");
                                showSearchingMessage();
                                break;

                            case "ACEPTADO": // Pedido aceptado
                                Log.d(TAG, "Estado: ACEPTADO, Subestado: " + subEstadoAceptado);
                                switch (subEstadoAceptado) {
                                    case "ESPERA_CONDUCTOR":
                                        Log.d(TAG, "Subestado: ESPERA_CONDUCTOR. Conductor asignado, en camino.");
                                        break;

                                    case "A_BORDO":
                                        Log.d(TAG, "Subestado: A_BORDO. Cliente ya está en el vehículo.");
                                        break;

                                    case "FINALIZADO":
                                        Log.d(TAG, "Subestado: FINALIZADO. Pedido completado.");
                                        stopServiceAndBroadcast(); // Detener el servicio
                                        return;
                                }
                                break;

                            case "CANCELADO": // Pedido cancelado
                                Log.d(TAG, "Estado: CANCELADO. El pedido ha sido cancelado.");
                                stopServiceAndBroadcast(); // Detener el servicio
                                return;

                            default: // Estado desconocido
                                Log.d(TAG, "Estado desconocido: " + status);
                                break;
                        }

                        // Enviar un broadcast con el estado actualizado
                        Intent broadcastIntent = new Intent("com.example.Pavill.PEDIDO_STATUS_UPDATE");
                        broadcastIntent.putExtra("status", status);
                        broadcastIntent.putExtra("message", message);
                        broadcastIntent.putExtra("subEstado", subEstadoAceptado); // Incluye el subestado en caso sea relevante
                        sendBroadcast(broadcastIntent);

                        // Continuar verificando mientras no sea cancelado o finalizado
                        if (!isServiceStopped) {
                            handler.postDelayed(statusChecker, CHECK_INTERVAL);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isServiceStopped) return; // No procesar errores si el servicio está detenido

                        Log.e(TAG, "Error al verificar el estado del pedido: " + errorMessage);
                        handler.postDelayed(statusChecker, CHECK_INTERVAL);
                    }
                });
            }
        };

        handler.post(statusChecker); // Iniciar la primera ejecución
    }

    /**
     * Detiene el servicio y limpia el `Handler` y el `Runnable`.
     */
    private void stopStatusChecker() {
        isServiceStopped = true; // Actualiza la bandera para detener el Runnable
        if (handler != null && statusChecker != null) {
            handler.removeCallbacks(statusChecker); // Limpia todas las tareas pendientes en el Handler
        }
    }

    /**
     * Detiene el servicio de manera segura.
     */
    private void stopServiceAndBroadcast() {
        stopStatusChecker(); // Detener el Runnable
        stopSelf(); // Finalizar el servicio
        Log.d(TAG, "PedidoStatusService detenido correctamente.");
    }

    private void showSearchingMessage() {
        Toast.makeText(this, "Seguimos buscando...", Toast.LENGTH_LONG).show();
    }

    /**
     * Cambiar el subestado actual del estado "ACEPTADO".
     * @param nuevoSubEstado El nuevo subestado ("ESPERA_CONDUCTOR", "A_BORDO", "FINALIZADO").
     */
    public void cambiarSubEstadoAceptado(String nuevoSubEstado) {
        subEstadoAceptado = nuevoSubEstado;
        Log.d(TAG, "Subestado del estado ACEPTADO cambiado a: " + subEstadoAceptado);
    }
}