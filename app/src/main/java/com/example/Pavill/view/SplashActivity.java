package com.example.Pavill.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pavill.R;
import com.example.Pavill.controller.ObtenerFondoLoginController;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIMEOUT = 3000; // 3 segundos de espera opcional
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_IMAGE_URL = "background_image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Iniciar la carga de la imagen del fondo
        cargarFondoLogin();
    }

    private void cargarFondoLogin() {
        ObtenerFondoLoginController controller = new ObtenerFondoLoginController();

        controller.obtenerFondoLogin(this, new ObtenerFondoLoginController.FondoLoginCallback() {
            @Override
            public void onSuccess(String urlImagen) {
                Log.d("SplashActivity", "Imagen de fondo obtenida: " + urlImagen);
                guardarImagenEnStorage(urlImagen);
                abrirMainActivity();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("SplashActivity", "Error obteniendo fondo: " + errorMessage);
                abrirMainActivity();
            }
        });
    }

    /**
     * guardar la imagen aleatoria en el shared
     * @param urlImagen
     */
    private void guardarImagenEnStorage(String urlImagen) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_IMAGE_URL, urlImagen).apply();
    }

    private void abrirMainActivity() {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // Cerrar `SplashActivity` para que no vuelva atrás
    }
}
