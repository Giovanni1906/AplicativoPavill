package radiotaxipavill.radiotaxipavillapp.view;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import radiotaxipavill.radiotaxipavillapp.R;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        checkLocationSettings();

        // Referencia al botón
        Button btnActivateUbication = findViewById(R.id.btnActivateUbication);

        // Configurar el listener para el botón
        btnActivateUbication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Solicitar al usuario que habilite los servicios de ubicación si aún no están habilitados.
                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(10000)
                        .setFastestInterval(5000);

                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

                SettingsClient settingsClient = LocationServices.getSettingsClient(HomeActivity.this);
                Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

                task.addOnSuccessListener(HomeActivity.this, locationSettingsResponse -> {
                    // Servicios de ubicación ya habilitados, redirigir a MapActivity.
                    Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                    startActivity(intent);
                    finish(); // Finaliza la actividad actual
                });

                task.addOnFailureListener(HomeActivity.this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            // Mostrar diálogo al usuario para activar la ubicación.
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Manejar el error.
                        }
                    }
                });
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // El usuario habilitó la ubicación. Redirigir a MainActivity.
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Finaliza la actividad actual
            } else {
                // El usuario decidió no habilitar la ubicación. Puedes manejar la lógica aquí.
            }
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ResolvableApiException.class);
                    // Si la ubicación está activada, puedes redirigir al usuario al MapActivity
                    goToMapActivity();
                } catch (ResolvableApiException e) {
                    // La ubicación no está activada, pide al usuario que la active
                    try {
                        e.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Error al intentar resolver la solicitud
                        Log.e("LocationSettings", "Error al pedir activar la ubicación", sendEx);
                    }
                }
            }
        });
    }

    private void goToMapActivity() {
        Intent intent = new Intent(HomeActivity.this, MapActivity.class);
        startActivity(intent);
        finish();
    }

}
