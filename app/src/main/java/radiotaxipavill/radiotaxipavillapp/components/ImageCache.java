package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class ImageCache {
    /**
     * Carga una imagen desde el caché.
     *
     * @param context El contexto de la actividad.
     * @param fileName El nombre del archivo de la imagen en caché.
     * @return El bitmap de la imagen si existe, de lo contrario, null.
     */
    public static Bitmap loadImageFromCache(Context context, String fileName) {
        try {
            File cacheDir = context.getCacheDir();
            File imageFile = new File(cacheDir, fileName);

            if (imageFile.exists()) {
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
