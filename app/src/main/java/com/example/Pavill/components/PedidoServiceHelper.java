package com.example.Pavill.components;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.Pavill.services.PedidoStatusService;

public class PedidoServiceHelper {

    /**
     * Actualiza el subestado de pedido.
     *
     * @param context   El contexto desde donde se llama.
     * @param newState  El nuevo subestado ("ESPERA_CONDUCTOR", "A_BORDO", "FINALIZADO").
     */
    public static void updateSubPedidoState(Context context, String newState) {
        Intent intent = new Intent(context, PedidoStatusService.class);
        intent.putExtra("nuevoSubEstado", newState);
        context.startService(intent);
    }

    /**
     * Inicia el servicio PedidoStatusService.
     *
     * @param context El contexto desde donde se llama.
     */
    public static void startPedidoStatusService(Context context) {
        Intent serviceIntent = new Intent(context, PedidoStatusService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // Usa startForegroundService para Android 8+.
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Detiene el servicio PedidoStatusService.
     *
     * @param context El contexto desde donde se llama.
     */
    public static void stopPedidoStatusService(Context context) {
        Intent serviceIntent = new Intent(context, PedidoStatusService.class);
        context.stopService(serviceIntent);
    }
}
