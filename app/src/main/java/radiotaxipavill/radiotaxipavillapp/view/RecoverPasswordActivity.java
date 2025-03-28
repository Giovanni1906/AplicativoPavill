package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.controller.RecuperarContrasenaController;

public class RecoverPasswordActivity extends AppCompatActivity {

    private EditText textEmail;
    private Button btnRecover;
    private TextView errorText;
    private LoadingDialog loadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_password);

        // Inicializar LoadingDialog
        loadingDialog = new LoadingDialog(this);

        // Inicializar vistas
        textEmail = findViewById(R.id.TextEmail);
        btnRecover = findViewById(R.id.btnRecover);
        errorText = findViewById(R.id.ErrorText);

        // Configurar el botón de recuperación
        btnRecover.setOnClickListener(v -> {
            String email = textEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !email.contains("@")) {
                errorText.setText("Por favor, ingresa un correo electrónico válido.");
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
                callRecuperarContrasenaController(email);
            }
        });

        // va hacia Registrarse
        TextView recoverPasswordText = findViewById(R.id.textViewRegister);
        recoverPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(RecoverPasswordActivity.this, VerifyPhoneActivity.class);
            startActivity(intent);
        });

        // Configurar botón para regresar al MainActivity
        CardView btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(RecoverPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Llama al controlador para recuperar la contraseña.
     */
    private void callRecuperarContrasenaController(String email) {
        RecuperarContrasenaController controller = new RecuperarContrasenaController();
        // Mostrar indicador de carga
        loadingDialog.show();
        controller.recuperarContrasena(this, email, new RecuperarContrasenaController.RecuperarContrasenaCallback() {
            @Override
            public void onSuccess(String message) {
                // Ocultar indicador de carga
                loadingDialog.dismiss();
                // Mostrar el ArrivalMessageDialog si el correo se envió correctamente
                ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
                arrivalMessageDialog.setDriverName("En unos minutos le llegará un email al correo " + maskEmail(email) + " con los pasos a seguir.");
                arrivalMessageDialog.setButtonText("Continuar");
                arrivalMessageDialog.setOnConfirmClickListener(() -> {
                    // Ir a LoginActivity
                    Intent intent = new Intent(RecoverPasswordActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
                arrivalMessageDialog.show(getSupportFragmentManager(), "arrivalMessageDialog");
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                // Mostrar el mensaje de error
                Toast.makeText(RecoverPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Enmascara un correo electrónico para mostrarlo parcialmente oculto.
     */
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length == 2) {
            String localPart = parts[0];
            String domainPart = parts[1];

            // Enmascarar la parte local del correo
            String maskedLocal = localPart.length() > 3
                    ? localPart.substring(0, 3) + "******"
                    : localPart.charAt(0) + "******";

            // Enmascarar el dominio
            String maskedDomain = domainPart.length() > 4
                    ? domainPart.substring(0, 4) + "****"
                    : domainPart + "****";

            return maskedLocal + "@" + maskedDomain;
        }
        return "correo inválido";
    }
}
