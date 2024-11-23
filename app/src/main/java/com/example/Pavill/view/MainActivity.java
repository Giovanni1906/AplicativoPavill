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

public class MainActivity extends BaseActivity {

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

    private long backPressedTime;
    private Toast backToast;

    // Verificar si el usuario está logueado utilizando SharedPreferences
    private boolean checkUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
}