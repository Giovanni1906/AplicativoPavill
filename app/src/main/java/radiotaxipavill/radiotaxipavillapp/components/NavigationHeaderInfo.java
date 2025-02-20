package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import radiotaxipavill.radiotaxipavillapp.R;
import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class NavigationHeaderInfo {
    public static void setupHeader(Context context, NavigationView navigationView) {
        // Cambiar el color de fondo del NavigationView
        navigationView.setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundColor));

        // Accede a la vista del encabezado del NavigationView
        View headerView = navigationView.getHeaderView(0);

        // Obtén referencias a los elementos en el header
        TextView textViewName = headerView.findViewById(R.id.user_name);
        TextView textViewEmail = headerView.findViewById(R.id.user_email);
        CircularImageView profileImage = headerView.findViewById(R.id.profile_image);

        // Accede a SharedPreferences para obtener el nombre y correo del cliente
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String clienteNombre = sharedPreferences.getString("ClienteNombre", "Nombre no disponible");
        String clienteEmail = sharedPreferences.getString("ClienteEmail", "Correo no disponible");

        // Establece los textos en los TextView
        textViewName.setText(clienteNombre);
        textViewEmail.setText(clienteEmail);

        // Cargar imagen desde el caché (si existe) o usar la predeterminada
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, "profile_image.png");

        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            profileImage.setImageBitmap(bitmap);
        } else {
            // Usa la imagen predeterminada si no hay imagen en caché
            profileImage.setImageResource(R.drawable.img_conductor);
        }
    }
}
