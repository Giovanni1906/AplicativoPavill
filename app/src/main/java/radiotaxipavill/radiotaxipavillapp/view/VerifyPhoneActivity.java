package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.DeviceIdentifier;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.controller.PhoneVerificationController;

public class VerifyPhoneActivity extends AppCompatActivity {

    private EditText textPhone;
    private Button btnCode;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        textPhone = findViewById(R.id.textPhone);
        btnCode = findViewById(R.id.btnCode);
        errorText = findViewById(R.id.errorText);

        LoadingDialog loadingDialog = new LoadingDialog(this);

        // Recuperar el intent de origen
        Intent intent = getIntent();
        String origin = intent.getStringExtra("origin");
        String action = intent.getStringExtra("action");

        btnCode.setOnClickListener(v -> {
            String phoneNumber = textPhone.getText().toString().trim();
            if (isValidPhone(phoneNumber)) {
                String deviceId = DeviceIdentifier.getUniqueIdentifier(VerifyPhoneActivity.this);
                String appVersion = getAppVersion();

                // Mostrar el indicador de carga
                loadingDialog.show();

                // Llamar al controlador para enviar los datos al servicio
                PhoneVerificationController controller = new PhoneVerificationController();
                controller.verifyPhone(
                        VerifyPhoneActivity.this,
                        phoneNumber,  // El número ingresado por el usuario
                        deviceId,     // Identificador del dispositivo
                        appVersion,   // Versión de la app
                        new PhoneVerificationController.Callback() {
                            @Override
                            public void onSuccess(String verificationCode, String fullPhoneNumber) {
                                loadingDialog.dismiss(); // Ocultar el indicador de carga
                                // Aquí rediriges al VerifyCodeActivity o haces lo necesario con el código
                                Intent verifyIntent = new Intent(VerifyPhoneActivity.this, VerifyCodeActivity.class);
                                verifyIntent.putExtra("ClienteCodigoVerificacion", verificationCode); // Código recibido del servicio
                                verifyIntent.putExtra("ClienteCelularCompleto", fullPhoneNumber); // Número del usuario
                                verifyIntent.putExtra("Clienteidentificador", deviceId); // Identificador del celular

                                // Mantener el origen y acción
                                if (origin != null) {
                                    verifyIntent.putExtra("origin", origin);
                                }
                                if (action != null) {
                                    verifyIntent.putExtra("action", action);
                                }

                                startActivity(verifyIntent);
                                finish();

                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                loadingDialog.dismiss(); // Ocultar el indicador de carga

                                // Muestra el error debajo del botón en el TextView correspondiente
                                Toast.makeText(VerifyPhoneActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            } else {
                errorText.setText("Número de celular inválido.");
                errorText.setVisibility(View.VISIBLE);
            }
        });

        // Configurar botón para regresar a la actividad de origen
        CardView btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            Intent backIntent;

            if ("ProfileActivity".equals(origin)) {
                // Regresar a ProfileActivity con la acción correspondiente
                backIntent = new Intent(VerifyPhoneActivity.this, ProfileActivity.class);
                if ("changePassword".equals(action)) {
                    backIntent.putExtra("action", "changePasswordInterrupted");
                } else if ("changePhoneNumber".equals(action)) {
                    backIntent.putExtra("action", "changePhoneInterrupted");
                }
            } else {
                // Si viene de otra actividad, regresar al MainActivity
                backIntent = new Intent(VerifyPhoneActivity.this, MainActivity.class);
            }

            startActivity(backIntent);
            finish();
        });
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{9}"); // Validar formato de 9 dígitos
    }


    private String getAppVersion() {
        return getString(R.string.app_version_number); // Obtén desde strings.xml
    }
}
