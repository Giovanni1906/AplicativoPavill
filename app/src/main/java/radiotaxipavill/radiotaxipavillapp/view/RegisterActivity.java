package radiotaxipavill.radiotaxipavillapp.view;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.DeviceIdentifier;
import radiotaxipavill.radiotaxipavillapp.controller.AuthController;
import radiotaxipavill.radiotaxipavillapp.controller.RegisterController;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private RegisterController registerController;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar el controlador
        registerController = new RegisterController();

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
                    this,
                    dni,
                    name,
                    email,
                    phoneNumber,
                    password,
                    DeviceIdentifier.getUniqueIdentifier(this), //
                    getString(R.string.app_version_number),
                    new RegisterController.RegisterCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            loadingDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "¡Registro exitoso!", Toast.LENGTH_LONG).show();
                            // Iniciar sesion
                            login(email, password, DeviceIdentifier.getUniqueIdentifier(RegisterActivity.this));
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

                // Redirigir al MainActivity
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Si deseas cerrar esta actividad
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
        Intent intent = new Intent(RegisterActivity.this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Redirige a HomeActivity.
     */
    private void goToHomeActivity() {
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}