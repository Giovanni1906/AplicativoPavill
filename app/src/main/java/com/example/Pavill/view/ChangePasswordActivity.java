package com.example.Pavill.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.example.Pavill.R;
import com.example.Pavill.controller.ChangePasswordController;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editPassword, verifyPassword;
    private TextView errorText;
    private AppCompatButton btnCambiarContrasenia, btnVolverInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        // Referencias a los inputs y botones
        editPassword = findViewById(R.id.edit_password);
        verifyPassword = findViewById(R.id.verify_password);
        errorText = findViewById(R.id.errorText);
        btnCambiarContrasenia = findViewById(R.id.CambiarContrasenia);
        btnVolverInicio = findViewById(R.id.VolverInicio);

        // Acción al presionar "Guardar cambios"
        btnCambiarContrasenia.setOnClickListener(v -> cambiarContrasena());

        // Acción al presionar "Volver al inicio"
        btnVolverInicio.setOnClickListener(v -> {
            Intent intent = new Intent(ChangePasswordActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Método para manejar el cambio de contraseña.
     */
    private void cambiarContrasena() {
        String nuevaContrasena = editPassword.getText().toString().trim();
        String confirmarContrasena = verifyPassword.getText().toString().trim();

        // Validar que las contraseñas coincidan
        if (nuevaContrasena.isEmpty() || confirmarContrasena.isEmpty()) {
            mostrarError("Ambos campos son obligatorios.");
            return;
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        // Llamar al controlador para cambiar la contraseña
        new ChangePasswordController().changePassword(this, nuevaContrasena, new ChangePasswordController.ChangePasswordCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ChangePasswordActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                mostrarError(errorMessage);
            }
        });
    }

    /**
     * Muestra un mensaje de error en el TextView de error.
     */
    private void mostrarError(String mensaje) {
        errorText.setText(mensaje);
        errorText.setVisibility(View.VISIBLE);
    }
}
