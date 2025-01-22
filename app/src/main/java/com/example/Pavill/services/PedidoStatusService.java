package com.example.Pavill.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.Pavill.R;
import com.example.Pavill.controller.PedidoStatusController;
import com.example.Pavill.view.ProgressActivity;

public class PedidoStatusService extends Service {

    private static final String TAG = "PedidoStatusService";
    private static final long CHECK_INTERVAL = 3000; // Intervalo de verificación en milisegundos
    private static final String CHANNEL_ID = "pedido_status_channel"; // ID único para el canal de notificaciones
    private Handler handler;
    private Runnable statusChecker;

    // Variable para manejar el subestado del estado "ACEPTADO"
    private String subEstadoAceptado = "ESPERA_CONDUCTOR"; // Subestado inicial predeterminado
    private String pedidoStatus = "EN_ESPERA"; // Estado inicial predeterminado

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        createNotificationChannel();
        startForegroundServiceWithNotification("EN_ESPERA", "Buscando conductor...");
        startCheckingPedidoStatus();
        Log.d(TAG, "PedidoStatusService iniciado.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Verifica el permiso de notificación
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permiso POST_NOTIFICATIONS no otorgado. Solicitando...");
                return START_NOT_STICKY; // Detén el servicio si no hay permisos
            }
        }

        if (intent != null && intent.hasExtra("nuevoSubEstado")) {
            String nuevoSubEstado = intent.getStringExtra("nuevoSubEstado");
            cambiarSubEstadoAceptado(nuevoSubEstado); // Llama al método para cambiar el subestado
        }

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

    @Nullable
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

                        pedidoStatus = status; // Actualiza el estado global
                        switch (status) {
                            case "EN_ESPERA": // Pedido en espera
                                Log.d(TAG, "Estado: EN_ESPERA. Buscando conductor...");
                                showSearchingMessage();
                                updateNotification("EN_ESPERA", "Buscando conductor...");
                                break;

                            case "ACEPTADO": // Pedido aceptado
                                Log.d(TAG, "Estado: ACEPTADO, Subestado: " + subEstadoAceptado);

                                // Actualizar la notificación con el subestado
                                updateNotification("ACEPTADO", "Subestado: " + subEstadoAceptado);

                                // Manejar subestados
                                switch (subEstadoAceptado) {
                                    case "ESPERA_CONDUCTOR":
                                        Log.d(TAG, "Subestado: ESPERA_CONDUCTOR. Conductor asignado, en camino.");
                                        break;

                                    case "A_BORDO":
                                        Log.d(TAG, "Subestado: A_BORDO. Cliente ya está en el vehículo.");
                                        break;

                                    case "FINALIZADO":
                                        Log.d(TAG, "Subestado: FINALIZADO. Pedido completado.");
                                        updateNotification("FINALIZADO", "Pedido completado.");
                                        stopSelf(); // Finaliza el servicio
                                        return;
                                }
                                break;

                            case "CANCELADO": // Pedido cancelado
                                Log.d(TAG, "Estado: CANCELADO. El pedido ha sido cancelado.");
                                updateNotification("CANCELADO", "El pedido fue cancelado.");
                                stopSelf(); // Detener el servicio
                                break;

                            default: // Estado desconocido
                                Log.d(TAG, "Estado desconocido: " + status);
                                break;
                        }

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

    /**
     * Cambiar el subestado actual del estado "ACEPTADO".
     * @param nuevoSubEstado El nuevo subestado ("ESPERA_CONDUCTOR", "A_BORDO", "FINALIZADO").
     */
    public void cambiarSubEstadoAceptado(String nuevoSubEstado) {
        subEstadoAceptado = nuevoSubEstado;
        Log.d(TAG, "Subestado del estado ACEPTADO cambiado a: " + subEstadoAceptado);
        updateNotification("ACEPTADO", "Subestado: " + subEstadoAceptado); // Actualiza la notificación
    }

    /**
     * Inicia el servicio en primer plano con una notificación inicial.
     */
    private void startForegroundServiceWithNotification(String status, String message) {
        Intent notificationIntent = new Intent(this, ProgressActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Estado del Pedido: " + status)
                .setContentText(message)
                .setSmallIcon(R.drawable.logo) // Cambia por tu ícono de notificación
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true) // Hace la notificación persistente
                .build();

        startForeground(1, notification);
    }

    /**
     * Actualiza la notificación en tiempo real con el estado y subestado.
     */
    private void updateNotification(String status, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Estado del Pedido: " + status)
                .setContentText(message)
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification); // Actualiza la notificación
    }

    /**
     * Crea un canal de notificación para Android O y superior.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pedido Status Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
