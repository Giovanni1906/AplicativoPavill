package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.view.MapActivity;

public class PedidoCancellationHelper {

    /**
     * Procedimiento para cancelar pedido.
     *
     * @param context El contexto desde donde se llama (puede ser Activity o Service).
     */
    public static void cancelProcess(Context context) {
        // Mostrar mensaje de cancelación
        showCancelledMessage(context);

        // Limpiar TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(context);  // 🔹 Restaurar datos guardados
        temporaryData.clearData(context);

        // Detener el servicio PedidoStatusService
        PedidoServiceHelper.stopPedidoStatusService(context);

        // Crear un intent para regresar al MapActivity
        Intent intent = new Intent(context, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
        context.startActivity(intent);

        // Si es una actividad, finalizarla
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }

    /**
     * Muestra un mensaje de cancelación.
     *
     * @param context El contexto desde donde se llama.
     */
    private static void showCancelledMessage(Context context) {
        Toast.makeText(context, "El pedido fue cancelado", Toast.LENGTH_LONG).show();
    }
}
