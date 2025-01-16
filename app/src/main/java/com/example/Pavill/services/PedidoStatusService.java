package com.example.Pavill.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.Pavill.controller.PedidoStatusController;

public class PedidoStatusService extends Service {

    private static final String TAG = "PedidoStatusService";
    private static final long CHECK_INTERVAL = 3000; // Intervalo de verificación en milisegundos
    private Handler handler;
    private Runnable statusChecker;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        startCheckingPedidoStatus();
        Log.d(TAG, "PedidoStatusService iniciado.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Permite que el servicio se reinicie automáticamente si se detiene
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && statusChecker != null) {
            handler.removeCallbacks(statusChecker);
        }
        Log.d(TAG, "PedidoStatusService destruido.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startCheckingPedidoStatus() {
        statusChecker = new Runnable() {
            @Override
            public void run() {
                new PedidoStatusController().checkPedidoStatus(PedidoStatusService.this, new PedidoStatusController.PedidoStatusCallback() {
                    @Override
                    public void onStatusReceived(String status, String message) {
                        Log.d(TAG, "Estado del pedido recibido: " + status + " - " + message);

                        // Log para cada estado
                        switch (status) {
                            case "EN_ESPERA": // Pedido en espera
                                Log.d(TAG, "Estado: EN_ESPERA. Buscando conductor...");
                                showSearchingMessage();
                                break;

                            case "ACEPTADO": // Pedido aceptado
                                Log.d(TAG, "Estado: ACEPTADO. Conductor asignado, esperando confirmación.");
                                break;

                            case "CANCELADO": // Pedido cancelado
                                Log.d(TAG, "Estado: CANCELADO. El pedido ha sido cancelado.");
                                stopSelf(); // Detener el servicio
                                break;

                            case "A_BORDO": // Conductor a bordo
                                Log.d(TAG, "Estado: A_BORDO. Se está procediendo a llevar al cliente.");
                                stopSelf(); // Detener el servicio
                                break;

                            case "FINALIZADO": // Pedido finalizado
                                Log.d(TAG, "Estado: FINALIZADO. El pedido ha sido completado.");
                                stopSelf(); // Detener el servicio
                                break;

                            case "DESCONOCIDO": // estado desconocido del P006
                                Log.d(TAG, "Estado desconocido del P006: " + status);

                            default: // Estado desconocido
                                Log.d(TAG, "Estado desconocido: " + status);
                                break;
                        }

                        // Enviar un broadcast con el estado actualizado
                        Intent broadcastIntent = new Intent("com.example.Pavill.PEDIDO_STATUS_UPDATE");
                        broadcastIntent.putExtra("status", status);
                        broadcastIntent.putExtra("message", message);
                        sendBroadcast(broadcastIntent);

                        // Continuar verificando mientras no sea cancelado o finalizado
                        if (!status.equals("CANCELADO") && !status.equals("FINALIZADO")) {
                            handler.postDelayed(statusChecker, CHECK_INTERVAL);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error al verificar el estado del pedido: " + errorMessage);
                        handler.postDelayed(statusChecker, CHECK_INTERVAL);
                    }
                });
            }
        };

        handler.post(statusChecker); // Iniciar la primera ejecución
    }

    private void showSearchingMessage() {
            Toast.makeText(this, "Seguimos buscando...", Toast.LENGTH_LONG).show();
    }
}