package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.controller.ChangePasswordController;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editPassword, verifyPassword;
    private AppCompatButton btnCambiarContrasenia, btnVolverInicio;
    LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        loadingDialog = new LoadingDialog(this);

        // Referencias a los inputs y botones
        editPassword = findViewById(R.id.edit_password);
        verifyPassword = findViewById(R.id.verify_password);
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
        loadingDialog.show();
        // Validar que las contraseñas coincidan
        if (nuevaContrasena.isEmpty() || confirmarContrasena.isEmpty()) {
            loadingDialog.dismiss();
            mostrarError("Ambos campos son obligatorios.");
            return;
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            loadingDialog.dismiss();
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        // Llamar al controlador para cambiar la contraseña
        new ChangePasswordController().changePassword(this, nuevaContrasena, new ChangePasswordController.ChangePasswordCallback() {
            @Override
            public void onSuccess(String message) {
                loadingDialog.dismiss();
                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ChangePasswordActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                mostrarError(errorMessage);
            }
        });
    }

    /**
     * Muestra un mensaje de error en el TextView de error.
     */
    private void mostrarError(String mensaje) {
        Toast.makeText(ChangePasswordActivity.this, "Error: " + mensaje, Toast.LENGTH_SHORT).show();
    }
}
