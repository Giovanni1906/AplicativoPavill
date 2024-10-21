package com.example.Pavill.view;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.Pavill.R;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // El usuario activó la ubicación, redirige al MapActivity
                goToMapActivity();
            } else {
                // El usuario no activó la ubicación, muestra un mensaje
                Toast.makeText(this, "Por favor, active la ubicación para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
