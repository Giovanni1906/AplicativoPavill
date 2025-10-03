package radiotaxipavill.radiotaxipavillapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.PedidoStatusController;
import radiotaxipavill.radiotaxipavillapp.view.MainActivity;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, createNotification());
            }

        handler = new Handler();
        isServiceStopped = false; // Asegúrate de reiniciar la bandera al crear el servicio
        startCheckingPedidoStatus();
        Log.d(TAG, "PedidoStatusService iniciado.");
    }

    /**
     * Crea una notificación para el servicio en primer plano.
     * @return
     */
    private Notification createNotification() {
        String channelId = "pedido_status_channel";

        // Crear el canal de notificación (Solo para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Estado del Pedido",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 👉 Launch intent del paquete: trae la app al frente sin resetear el estado
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent == null) {
            // Fallback por si acaso (no debería pasar)
            launchIntent = new Intent(this, MainActivity.class);
        }
        // Aseguramos que se comporte como desde el launcher
        launchIntent.setAction(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Flags clave:
        // - NEW_TASK: necesario desde servicio
        // - RESET_TASK_IF_NEEDED: si la tarea existe, la trae al frente con su estado;
        //   si no, la crea respetando el stack declarado en el manifest.
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Crear la notificación
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Pavill")
                .setContentText("Tienes una carrera en proceso...")
                .setSmallIcon(R.drawable.icono_pavill) // Cambia esto por un ícono válido en tu app
                .setOngoing(true) // La notificación no se puede deslizar para cerrar
                .setContentIntent(pi)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ("STOP_FOREGROUND_SERVICE".equals(intent.getAction())) {
                stopServiceAndBroadcast();
                return START_NOT_STICKY;
            }

            if (intent.hasExtra("nuevoSubEstado")) {
                String nuevoSubEstado = intent.getStringExtra("nuevoSubEstado");
                cambiarSubEstadoAceptado(nuevoSubEstado);
            }
        }

        // Verificar optimización de batería
        checkBatteryOptimization();

        return START_STICKY; // Mantiene el servicio activo
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return; // Si no hay PowerManager, salir

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Verificar si existe una actividad que pueda manejar este Intent
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Log.w("MapActivity", "No se encontró una actividad para manejar la optimización de batería.");
                    }
                } catch (Exception e) {
                    Log.e("MapActivity", "Error al intentar abrir configuración de batería: " + e.getMessage());
                }
            }
        }
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

                        // Guardar estado en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("PedidoPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("pedido_status", status);
                        editor.putString("pedido_subestado", subEstadoAceptado); // Guardar el subestado si aplica
                        editor.apply();

                        switch (status) {
                            case "EN_ESPERA": // Pedido en espera
                                Log.d(TAG, "Estado: EN_ESPERA. Buscando conductor...");
                                
                                // Enviar broadcast para dirigir a SearchingActivity
                                sendStatusBroadcast(status, "Buscando conductor disponible...");
                                break;

                            case "ACEPTADO": // Pedido aceptado
                                Log.d(TAG, "Estado: ACEPTADO, Subestado: " + subEstadoAceptado);

                                // 🔹 Enviar broadcast para actualizar la UI
                                sendStatusBroadcast(status, "Pedido aceptado, actualizando subestado...");


                                switch (subEstadoAceptado) {
                                    case "ESPERA_CONDUCTOR":
                                        Log.d(TAG, "Subestado: ESPERA_CONDUCTOR. Conductor asignado, en camino.");
                                        break;

                                    case "A_BORDO":
                                        Log.d(TAG, "Subestado: A_BORDO. Cliente ya está en el vehículo.");
                                        break;

                                    case "FINALIZADO":
                                        Log.d(TAG, "Subestado: FINALIZADO. Pedido completado.");
                                        sendBroadcastAndStop("CANCELADO");
                                        return;
                                }
                                break;

                            case "CANCELADO": // Pedido cancelado
                                Log.d(TAG, "Estado: CANCELADO. El pedido ha sido cancelado.");
                                sendBroadcastAndStop("CANCELADO");
                                return;

                            default: // Estado desconocido
                                Log.d(TAG, "Estado desconocido: " + status);
                                break;
                        }

//                        // Enviar un broadcast con el estado actualizado
//                        Intent broadcastIntent = new Intent("radiotaxipavill.radiotaxipavillapp.PEDIDO_STATUS_UPDATE");
//                        broadcastIntent.putExtra("status", status);
//                        broadcastIntent.putExtra("message", message);
//                        broadcastIntent.putExtra("subEstado", subEstadoAceptado); // Incluye el subestado en caso sea relevante
//                        LocalBroadcastManager.getInstance(PedidoStatusService.this).sendBroadcast(broadcastIntent);

                        // Continuar verificando mientras no sea cancelado o finalizado
                        if (!isServiceStopped) {
                            handler.postDelayed(statusChecker, CHECK_INTERVAL);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isServiceStopped) return;

                        Log.e(TAG, "Error al verificar el estado del pedido: " + errorMessage);

                        // Verifica si el error parece una pérdida de red (esto puede variar según tu implementación del controlador)
                        if (errorMessage.contains("Unable to resolve host") || errorMessage.contains("timeout") || errorMessage.contains("failed to connect")) {
                            // Reintentar más tarde sin detener el servicio
                            Log.w(TAG, "Problemas de conección. Reintentando en 5 segundos...");
                            handler.postDelayed(statusChecker, 5000); // Reintenta en 5 segundos
                        } else {
                            // Si es un error real no relacionado a red, entonces sí detenemos
                            sendBroadcastAndStop("ERROR");
                        }
                    }

                });
            }
        };

        handler.post(statusChecker); // Iniciar la primera ejecución
    }

    /**
     * Envía un broadcast con el estado del pedido sin detener el servicio.
     * @param status Estado actual del pedido.
     * @param message Mensaje informativo para la UI.
     */
    private void sendStatusBroadcast(String status, String message) {
        Intent broadcastIntent = new Intent("radiotaxipavill.radiotaxipavillapp.PEDIDO_STATUS_UPDATE");
        broadcastIntent.putExtra("status", status);
        broadcastIntent.putExtra("message", message);
        broadcastIntent.putExtra("subEstado", subEstadoAceptado);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        Log.d(TAG, "📢 Broadcast enviado: " + status + " - " + message);
    }

    /**
     * Envía un broadcast con el estado final del pedido y detiene el servicio.
     * @param status Estado final del pedido (CANCELADO, FINALIZADO, ERROR).
     */
    private void sendBroadcastAndStop(String status) {
        // 🔹 Enviar broadcast ANTES de detener el servicio
        Intent broadcastIntent = new Intent("radiotaxipavill.radiotaxipavillapp.PEDIDO_STATUS_UPDATE");
        broadcastIntent.putExtra("status", status);
        broadcastIntent.putExtra("message", "Pedido finalizado con estado: " + status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        Log.d(TAG, "📢 Broadcast enviado: " + status);

        // 🔹 Guardar en SharedPreferences que el pedido ha finalizado
        SharedPreferences prefs = getSharedPreferences("PedidoPrefs", MODE_PRIVATE);
        prefs.edit()
                .remove("pedido_status")
                .remove("pedido_subestado")
                .putBoolean("pedido_activo", false)
                .apply();

        if (status.equals("FINALIZADO") || status.equals("CANCELADO")) {
            TemporaryData temporaryData = TemporaryData.getInstance();
            temporaryData.loadFromPreferences(this);
            temporaryData.clearData(this);
        }

        // 🔹 Detener el servicio correctamente
        stopStatusChecker();
        stopForeground(true);
        stopSelf();
        Log.d(TAG, "✅ PedidoStatusService detenido correctamente.");
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
        Log.e(TAG, "PedidoStatusService detenido debido a un error crítico.");
        // Marcar el pedido como finalizado en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("PedidoPrefs", MODE_PRIVATE);
        prefs.edit()
                .remove("pedido_status")
                .remove("pedido_subestado")
                .putBoolean("pedido_activo", false)
                .apply();

        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);  // 🔹 Restaurar datos guardados
        // 🔹 Limpiar `TemporaryData`
        temporaryData.clearData(this);

        stopForeground(true); // Detener el ForegroundService
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