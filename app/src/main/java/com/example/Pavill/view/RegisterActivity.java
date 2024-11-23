package com.example.Pavill.view;

import com.example.Pavill.R;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.DeviceIdentifier;
import com.example.Pavill.controller.RegisterController;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private RegisterController registerController;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar el controlador
        registerController = new RegisterController(this);

        // Botón para registrar
        Button btnRegister = findViewById(R.id.btnCreateAccount);
        btnRegister.setOnClickListener(v -> {
            // Obtener el número de celular desde el Intent
            String phoneNumber = getIntent().getStringExtra("phoneNumber");

            // Recolectar datos del formulario
            EditText editTextDNI = findViewById(R.id.editTextDNI);
            EditText editTextName = findViewById(R.id.editTextName);
            EditText editTextEmail = findViewById(R.id.editTextEmail);
            EditText editTextPassword = findViewById(R.id.editTextPassword);

            String dni = editTextDNI.getText().toString();
            String name = editTextName.getText().toString();
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();

            // Validaciones básicas
            if (dni.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Inicializar LoadingDialog
            loadingDialog = new LoadingDialog(this);
            loadingDialog.show();

            // Llamar al controlador para registrar
            registerController.registerClient(
                    dni,
                    name,
                    email,
                    phoneNumber,
                    password,
                    DeviceIdentifier.getUniqueIdentifier(this), // Asegúrate de que esta clase existe
                    getString(R.string.app_version_number),
                    new RegisterController.RegisterCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            loadingDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "¡Registro exitoso!", Toast.LENGTH_LONG).show();
                            // Redirigir al MainActivity
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            loadingDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        // Botón para regresar al MainActivity
        CardView btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            // Redirigir al MainActivity
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Si deseas cerrar esta actividad
        });

        // Texto para redirigir al LoginActivity
        TextView textViewLogin = findViewById(R.id.textViewLogin);
        textViewLogin.setOnClickListener(v -> {
            // Redirigir al LoginActivity
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}