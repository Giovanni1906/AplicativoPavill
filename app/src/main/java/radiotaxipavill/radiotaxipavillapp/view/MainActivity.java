package radiotaxipavill.radiotaxipavillapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import radiotaxipavill.radiotaxipavillapp.R;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_IMAGE_URL = "background_image_url";

    private ImageView backgroundPavill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundPavill = findViewById(R.id.backgroundPavill);

        // Cargar la imagen almacenada en SharedPreferences
        cargarFondoDesdeStorage();

        // Solicitar permiso de notificación
        requestNotificationPermission();

        // Verificar si el usuario ya está logueado
        if (checkUserLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Inicializar la UI
        initializeUI();
    }

    /**
     * Cargar fondo aleatorio
     */
    private void cargarFondoDesdeStorage() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String urlImagen = sharedPreferences.getString(KEY_IMAGE_URL, null);

        if (urlImagen != null) {
            Glide.with(this)
                    .load(urlImagen)
                    .centerCrop()
                    .error(R.drawable.main_background) // Si falla, usa la imagen predeterminada
                    .into(backgroundPavill);
        } else {
            backgroundPavill.setImageResource(R.drawable.main_background);
        }
    }

    /**
     * Inicializa la interfaz de usuario.
     */
    private void initializeUI(){
        // Estilizar el TextView "Pide un Pavill"
        TextView textView = findViewById(R.id.textViewPavill);
        String text = "Pide un Pavill.";
        SpannableString spannableString = new SpannableString(text);

        // Aplicar color secundario solo a la palabra "Pavill"
        int start = text.indexOf("Pavill");
        int end = start + "Pavill".length();
        int secondaryColor = getResources().getColor(R.color.secondaryColor);
        spannableString.setSpan(new ForegroundColorSpan(secondaryColor), start, end, 0);

        // Establecer el texto estilizado en el TextView
        textView.setText(spannableString);

        // para los botones

        Button btnLogin = findViewById(R.id.BtnMainIniciarSesion);
        Button btnRegister = findViewById(R.id.BtnMainCrearCuenta);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VerifyPhoneActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Verificar si el usuario ya está logueado.
     * @return true si el usuario está logueado, false de lo contrario.
     */
    private boolean checkUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    /**
     * Maneja la respuesta de la solicitud de permisos.
     * @param requestCode The request code passed in
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso de notificaciones otorgado
                        Log.d("Permission", "Permiso POST_NOTIFICATIONS otorgado");
                    } else {
                        // Permiso de notificaciones denegado
                        //Toast.makeText(this, "Permiso para publicar notificaciones denegado.", Toast.LENGTH_SHORT).show();
                    }
                } else if (permission.equals(Manifest.permission.FOREGROUND_SERVICE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso FOREGROUND_SERVICE otorgado
                        Log.d("Permission", "Permiso FOREGROUND_SERVICE otorgado");
                    } else {
                        // Permiso FOREGROUND_SERVICE denegado
                        //Toast.makeText(this, "Permiso para usar servicios en primer plano denegado.", Toast.LENGTH_SHORT).show();
                    }
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso de ubicación precisa otorgado
                        Log.d("Permission", "Permiso ACCESS_FINE_LOCATION otorgado");
                    } else {
                        // Permiso de ubicación precisa denegado
                        //Toast.makeText(this, "Permiso para acceder a ubicación precisa denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * Solicitar permiso de notificación en versiones superiores a Android 13.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            boolean shouldRequest = false;
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    shouldRequest = true;
                    break;
                }
            }

            if (shouldRequest) {
                requestPermissions(permissions, 1);
            }
        }
    }

}