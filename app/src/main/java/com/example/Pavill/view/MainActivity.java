package com.example.Pavill.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.Pavill.R;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar si el usuario ya está logueado
        if (checkUserLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

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

    // Verificar si el usuario está logueado utilizando SharedPreferences
    private boolean checkUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }


}