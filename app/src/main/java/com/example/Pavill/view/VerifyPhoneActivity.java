package com.example.Pavill.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Pavill.R;
import com.example.Pavill.components.DeviceIdentifier;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.controller.PhoneVerificationController;

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

        btnCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                    Intent intent = new Intent(VerifyPhoneActivity.this, VerifyCodeActivity.class);
                                    intent.putExtra("ClienteCodigoVerificacion", verificationCode); // Código recibido del servicio
                                    intent.putExtra("ClienteCelularCompleto", fullPhoneNumber); // Número del usuario
                                    intent.putExtra("Clienteidentificador", deviceId); // Identificador del celular
                                    startActivity(intent);
                                    finish();

                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    loadingDialog.dismiss(); // Ocultar el indicador de carga

                                    // Muestra el error debajo del botón en el TextView correspondiente
                                    errorText.setText(errorMessage);
                                    errorText.setVisibility(View.VISIBLE);
                                }
                            }
                    );
                } else {
                    errorText.setText("Número de celular inválido.");
                    errorText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{9}"); // Validar formato de 9 dígitos
    }


    private String getAppVersion() {
        return getString(R.string.app_version_number); // Obtén desde strings.xml
    }
}
