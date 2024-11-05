package com.example.Pavill.view;
import com.example.Pavill.R;
import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ConfirmActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        // Obtener las coordenadas pasadas desde MapActivity
        Intent intent = getIntent();
        double originLat = intent.getDoubleExtra("origin_lat", 0.0);
        double originLng = intent.getDoubleExtra("origin_lng", 0.0);
        double destinationLat = intent.getDoubleExtra("destination_lat", 0.0);
        double destinationLng = intent.getDoubleExtra("destination_lng", 0.0);

        // Usar Geocoder para obtener direcciones amigables
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String originAddress = getAddressFromCoordinates(geocoder, originLat, originLng);
        String destinationAddress = getAddressFromCoordinates(geocoder, destinationLat, destinationLng);

        // Mostrar las direcciones en los TextViews
        TextView originTextView = findViewById(R.id.textOrigin);
        TextView destinationTextView = findViewById(R.id.textDestination);

        originTextView.setText("Origen: " + originAddress);
        destinationTextView.setText("Destino: " + destinationAddress);
    }

    /**
     * Método para obtener una dirección amigable a partir de coordenadas
     */
    private String getAddressFromCoordinates(Geocoder geocoder, double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Dirección no disponible";
    }
}