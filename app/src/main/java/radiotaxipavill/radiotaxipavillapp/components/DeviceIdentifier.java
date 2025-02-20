package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.UUID;

public class DeviceIdentifier {

    /**
     * Obtiene el identificador único del dispositivo.
     *
     * @param context El contexto necesario para acceder a los ajustes del dispositivo.
     * @return El identificador único del dispositivo.
     */
    public static String getUniqueIdentifier(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            return UUID.randomUUID().toString();
        }
    }
}
