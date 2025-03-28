package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.controller.PhoneVerificationController;

public class VerifyCodeActivity extends AppCompatActivity {

    private String verificationCode;
    private String phoneNumber;
    private String identify;
    private TextView tvResendCode;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        // Obtener datos del Intent
        verificationCode = getIntent().getStringExtra("ClienteCodigoVerificacion");
        phoneNumber = getIntent().getStringExtra("ClienteCelularCompleto");
        identify = getIntent().getStringExtra("Clienteidentificador");

        // Configurar el mensaje con el número de celular
        TextView tvConfirmationMessage = findViewById(R.id.tvConfirmationMessage);
        tvConfirmationMessage.setText("El código ha sido enviado al \n" + phoneNumber + " por SMS");

        // Configurar el TextView para reenviar el código
        tvResendCode = findViewById(R.id.tvResendCode);
        tvResendCode.setEnabled(false); // Inicialmente deshabilitado
        iniciarContador(); // Iniciar el contador

        tvResendCode.setOnClickListener(v -> {
            tvResendCode.setEnabled(false); // Deshabilitar el clic
            tvResendCode.setTextColor(getResources().getColor(R.color.textColor)); // Restablecer estilo
            reenviarCodigo(); // Reenviar código
        });

        // Configurar los cuadros de código
        configurarCuadrosDeCodigo();

        // Configurar el botón para verificar el código
        Button btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnVerifyCode.setOnClickListener(v -> verificarCodigo());

        // Configurar botón para regresar al MainActivity
        CardView btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(VerifyCodeActivity.this, VerifyPhoneActivity.class);
            startActivity(intent);
            finish();
        });

    }

    /**
     * Configura los cuadros de texto para el código de verificación.
     */
    private void configurarCuadrosDeCodigo() {
        EditText codeBox1 = findViewById(R.id.codeBox1);
        EditText codeBox2 = findViewById(R.id.codeBox2);
        EditText codeBox3 = findViewById(R.id.codeBox3);
        EditText codeBox4 = findViewById(R.id.codeBox4);

        TextWatcher codeTextWatcher = new TextWatcher() {
            private boolean isDeleting = false;
            private boolean isHandlingFocus = false; // Bandera para evitar cambios de foco múltiples

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after; // Detectar si el usuario está borrando
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isHandlingFocus) return; // Si ya estamos manejando el foco, no hacemos nada

                if (!isDeleting && s.length() == 1) {
                    isHandlingFocus = true; // Evitar múltiples cambios de foco
                    // Mover al siguiente cuadro automáticamente
                    if (codeBox1.hasFocus()) codeBox2.requestFocus();
                    else if (codeBox2.hasFocus()) codeBox3.requestFocus();
                    else if (codeBox3.hasFocus()) codeBox4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Convertir siempre el texto ingresado a mayúsculas
                if (s.length() > 0) {
                    String upperText = s.toString().toUpperCase();
                    if (!s.toString().equals(upperText)) {
                        s.replace(0, s.length(), upperText);
                    }
                }
                isHandlingFocus = false; // Restablecer bandera después de manejar el foco
            }
        };

        View.OnKeyListener backspaceKeyListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                EditText currentBox = (EditText) v;

                if (currentBox.getText().toString().isEmpty()) {
                    // Si el cuadro actual está vacío, eliminar el contenido del cuadro anterior
                    if (currentBox == codeBox2) {
                        codeBox1.requestFocus();
                        codeBox1.setText(""); // Eliminar el contenido
                    } else if (currentBox == codeBox3) {
                        codeBox2.requestFocus();
                        codeBox2.setText(""); // Eliminar el contenido
                    } else if (currentBox == codeBox4) {
                        codeBox3.requestFocus();
                        codeBox3.setText(""); // Eliminar el contenido
                    }
                    return true; // Consumir el evento
                }
            }
            return false; // No consumir otros eventos
        };

        // Asignar el TextWatcher y OnKeyListener a cada EditText
        codeBox1.addTextChangedListener(codeTextWatcher);
        codeBox2.addTextChangedListener(codeTextWatcher);
        codeBox3.addTextChangedListener(codeTextWatcher);
        codeBox4.addTextChangedListener(codeTextWatcher);

        codeBox1.setOnKeyListener(backspaceKeyListener);
        codeBox2.setOnKeyListener(backspaceKeyListener);
        codeBox3.setOnKeyListener(backspaceKeyListener);
        codeBox4.setOnKeyListener(backspaceKeyListener);
    }


    /**
     * Inicia el contador para reenviar el código.
     */
    private void iniciarContador() {
        // Restablecer estilo inicial (sin bold) cuando se reinicia el contador
        tvResendCode.setTypeface(null, Typeface.NORMAL);
        tvResendCode.setTextColor(getResources().getColor(R.color.textColor));

        // Crear y comenzar el contador
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendCode.setText("Reenviar código (" + millisUntilFinished / 1000 + ")");
            }

            @Override
            public void onFinish() {
                tvResendCode.setEnabled(true);
                tvResendCode.setText("Reenviar código");
                tvResendCode.setTypeface(null, Typeface.BOLD);
            }
        };
        countDownTimer.start();
    }

    /**
     * Llama al servicio para reenviar el código.
     */
    private void reenviarCodigo() {
        PhoneVerificationController controller = new PhoneVerificationController();
        String deviceId = identify; // Cambiar por el identificador real
        String appVersion = getAppVersion();

        controller.verifyPhone(this, phoneNumber, deviceId, appVersion, new PhoneVerificationController.Callback() {
            @Override
            public void onSuccess(String newVerificationCode, String newPhoneNumber) {
                verificationCode = newVerificationCode; // Actualizar el código de verificación
                iniciarContador(); // Reiniciar el contador
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(VerifyCodeActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

            }
        });
    }

    /**
     * Verifica el código ingresado.
     */
    private void verificarCodigo() {
        EditText codeBox1 = findViewById(R.id.codeBox1);
        EditText codeBox2 = findViewById(R.id.codeBox2);
        EditText codeBox3 = findViewById(R.id.codeBox3);
        EditText codeBox4 = findViewById(R.id.codeBox4);

        String enteredCode = codeBox1.getText().toString() +
                codeBox2.getText().toString() +
                codeBox3.getText().toString() +
                codeBox4.getText().toString();

        if (enteredCode.equalsIgnoreCase(verificationCode)) {
            // Obtener el origen y la acción
            Intent intent = getIntent();
            String origin = intent.getStringExtra("origin");
            String action = intent.getStringExtra("action");

            Intent nextIntent;

            if ("ProfileActivity".equals(origin)) {
                if ("changePassword".equals(action)) {
                    // Si la acción es cambiar contraseña, redirigir a ChangePasswordActivity
                    nextIntent = new Intent(VerifyCodeActivity.this, ChangePasswordActivity.class);
                    startActivity(nextIntent);
                    finish();
                } else if ("changePhoneNumber".equals(action)) {
                    // Si la acción es cambiar número de teléfono, regresar a ProfileActivity
                    nextIntent = new Intent(VerifyCodeActivity.this, ProfileActivity.class);
                    nextIntent.putExtra("action", "changePhoneSuccess");
                    startActivity(nextIntent);
                    finish();
                }
            } else {
                // Si viene de otro lugar, ir a RegisterActivity (o Main si aplica)
                nextIntent = new Intent(VerifyCodeActivity.this, RegisterActivity.class);
                nextIntent.putExtra("phoneNumber", phoneNumber);
                startActivity(nextIntent);
                finish();
            }


        } else {
            // Código incorrecto, mostrar error
            TextView tvErrorMessage = findViewById(R.id.errorText);
            tvErrorMessage.setText("El código ingresado es incorrecto.");
            tvErrorMessage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Obtiene la versión de la aplicación desde strings.xml.
     */
    private String getAppVersion() {
        return getString(R.string.app_version_number); // Obtén desde strings.xml
    }
}
