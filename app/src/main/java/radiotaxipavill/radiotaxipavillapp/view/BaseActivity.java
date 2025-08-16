package radiotaxipavill.radiotaxipavillapp.view;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private long backPressedTime;
    private Toast backToast;

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) { // Si la segunda pulsación ocurre en menos de 2 segundos
            if (backToast != null) backToast.cancel();  // Cancela el mensaje anterior si existe
            super.onBackPressed();  // Cierra la aplicación
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Presiona otra vez para salir", Toast.LENGTH_SHORT);
            backToast.show();  // Muestra la advertencia
        }

        backPressedTime = System.currentTimeMillis();  // Actualiza el tiempo de la primera pulsación
    }
}