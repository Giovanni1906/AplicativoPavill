package radiotaxipavill.radiotaxipavillapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import radiotaxipavill.radiotaxipavillapp.services.PedidoStatusService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootCompletedReceiver", "Dispositivo reiniciado. Verificando si hay un pedido en curso...");

            // Obtener el estado del pedido desde SharedPreferences o la base de datos
            SharedPreferences prefs = context.getSharedPreferences("PedidoPrefs", Context.MODE_PRIVATE);
            boolean pedidoEnCurso = prefs.getBoolean("pedido_activo", false);

            if (pedidoEnCurso) {
                Log.d("BootCompletedReceiver", "Hay un pedido activo. Reiniciando PedidoStatusService...");
                Intent serviceIntent = new Intent(context, PedidoStatusService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d("BootCompletedReceiver", "No hay pedidos activos. No se iniciará el servicio.");
            }
        }
    }

}
