package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BitmapUtils extends AppCompatActivity {

    /**
     * Redimensiona el ícono para los marcadores manteniendo las proporciones de la imagen.
     *
     * @param context El contexto desde donde se llama la función.
     * @param drawableRes El recurso drawable que será redimensionado.
     * @param targetHeight La altura deseada.
     * @return Un objeto BitmapDescriptor con el tamaño ajustado proporcionalmente.
     */
    public static BitmapDescriptor getProportionalBitmap(Context context, int drawableRes, int targetHeight) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(context.getResources(), drawableRes);
        targetHeight = (int) (targetHeight * context.getResources().getDisplayMetrics().density);
        // Calcular el ancho proporcional basado en la altura objetivo
        int originalWidth = imageBitmap.getWidth();
        int originalHeight = imageBitmap.getHeight();
        float aspectRatio = (float) originalWidth / originalHeight;
        int targetWidth = Math.round(targetHeight * aspectRatio);

        // Redimensionar la imagen manteniendo las proporciones
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, targetWidth, targetHeight, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

}
