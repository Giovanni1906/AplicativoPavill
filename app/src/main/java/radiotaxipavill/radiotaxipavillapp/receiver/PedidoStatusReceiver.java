package radiotaxipavill.radiotaxipavillapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PedidoStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "PedidoStatusReceiver";
    public static final String ACTION_PEDIDO_STATUS_UPDATE = "radiotaxipavill.radiotaxipavillapp.PEDIDO_STATUS_UPDATE";

    // Callback para manejar el estado del pedido
    public interface PedidoStatusCallback {
        void onStatusUpdate(String status);
    }

    private PedidoStatusCallback callback;

    public PedidoStatusReceiver(PedidoStatusCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_PEDIDO_STATUS_UPDATE.equals(intent.getAction())) {
            String status = intent.getStringExtra("status");
            Log.d(TAG, "Estado del pedido recibido: " + status);
            if (callback != null) {
                callback.onStatusUpdate(status);
            }
        }
    }
}
