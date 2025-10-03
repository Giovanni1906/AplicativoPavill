package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.DeviceIdentifier;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.controller.AuthController;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText textEmail, textPassword;
    private Button btnLogin;
    private TextView errorText;
    private LoadingDialog loadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar LoadingDialog
        loadingDialog = new LoadingDialog(this);

        textEmail = findViewById(R.id.TextEmail);
        textPassword = findViewById(R.id.TextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        errorText = findViewById(R.id.ErrorText);

        EditText passwordEditText = findViewById(R.id.TextPassword);
        ImageView togglePassword = findViewById(R.id.ivTogglePassword);


        // Configurar botón para regresar al MainActivity
        CardView btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Configurar el botón de inicio de sesión
        btnLogin.setOnClickListener(v -> {
            String email = textEmail.getText().toString().trim();
            String password = textPassword.getText().toString().trim();

            if (isValidInput(email, password)) {
                String deviceId =  DeviceIdentifier.getUniqueIdentifier(this);
                login(email, password, deviceId);
            } else {
                errorText.setText("Por favor ingresa un correo y/o contraseña válidos.");
                errorText.setVisibility(View.VISIBLE);
            }
        });

        // Texto para redirigir al Register
        TextView textViewLogin = findViewById(R.id.textViewRegister);
        textViewLogin.setOnClickListener(v -> {
            // Redirigir al LoginActivity
            Intent intent = new Intent(LoginActivity.this, VerifyPhoneActivity.class);
            startActivity(intent);
        });

        // va hacia el recuperar contraseña
        TextView recoverPasswordText = findViewById(R.id.RecuperarContraseña);
        recoverPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RecoverPasswordActivity.class);
            startActivity(intent);
        });

        // mostrar/ocultar contraseña
        togglePassword.setOnClickListener(v -> {
            if (passwordEditText.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Mostrar contraseña
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                //togglePassword.setImageResource(R.drawable.ic_eye_open); // icono ojo abierto
            } else {
                // Ocultar contraseña
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                //togglePassword.setImageResource(R.drawable.ic_eye_closed); // icono ojo cerrado
            }
            // Mover cursor al final
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

    }

    /**
     * Verifica si los campos de entrada son válidos.
     */
    private boolean isValidInput(String email, String password) {
        return email.contains("@") && !password.isEmpty();
    }

    /**
     * Maneja el proceso de inicio de sesión.
     */
    private void login(String email, String password, String deviceId) {


        AuthController authController = new AuthController();

        // Mostrar indicador de carga
        loadingDialog.show();

        authController.login(this, email, password, deviceId, new AuthController.Callback() {
            @Override
            public void onSuccess(JSONObject clientData) {
                // Ocultar indicador de carga
                loadingDialog.dismiss();

                // Guardar sesión en SharedPreferences
                saveSession(clientData);

                // Verificar ubicación y redirigir
                checkLocationSettings();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Ocultar indicador de carga
                loadingDialog.dismiss();

                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

            }
        });
    }

    /**
     * Guarda la sesión del usuario en SharedPreferences.
     */
    private void saveSession(JSONObject clientData) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            editor.putBoolean("isLoggedIn", true);
            editor.putString("ClienteId", clientData.getString("ClienteId"));
            editor.putString("ClienteNumeroDocumento", clientData.getString("ClienteNumeroDocumento"));
            editor.putString("ClienteNombre", clientData.getString("ClienteNombre"));
            editor.putString("ClienteEmail", clientData.getString("ClienteEmail"));
            editor.putString("ClienteCelular", clientData.getString("ClienteCelular"));
            editor.putString("Identificador", clientData.getString("Identificador"));
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica el estado de la ubicación antes de redirigir.
     */
    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {

            try {
                task1.getResult(Exception.class);
                // Si la ubicación está activada, ve a MapActivity
                goToMapActivity();
            } catch (Exception e) {
                // Si la ubicación no está activada, redirige a HomeActivity
                goToHomeActivity();
            }
        });
    }

    /**
     * Redirige a MapActivity.
     */
    private void goToMapActivity() {
        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Redirige a HomeActivity.
     */
    private void goToHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

}
