package com.example.Pavill.view;

import com.example.Pavill.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
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
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private ImageView iconOrigin;
    private ImageView iconDestination;
    private View lineBetween;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        // Botón de cancelar
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Simula la acción de retroceder
                onBackPressed();
            }
        });

        // Obtener las coordenadas de origen y destino desde el Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Obtener las coordenadas pasadas desde MapActivity
            double originLat = extras.getDouble("origin_lat", 0.0);
            double originLng = extras.getDouble("origin_lng", 0.0);
            double destinationLat = extras.getDouble("destination_lat", 0.0);
            double destinationLng = extras.getDouble("destination_lng", 0.0);

            // Crear los objetos LatLng para las coordenadas
            originCoordinates = new LatLng(originLat, originLng);
            destinationCoordinates = new LatLng(destinationLat, destinationLng);

            // Usar Geocoder para obtener direcciones amigables
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            String originAddress = getAddressFromCoordinates(geocoder, originLat, originLng);
            String destinationAddress = getAddressFromCoordinates(geocoder, destinationLat, destinationLng);

            // Mostrar las direcciones en los TextViews
            TextView originTextView = findViewById(R.id.textOrigin);
            TextView destinationTextView = findViewById(R.id.textDestination);

            originTextView.setText(originAddress);
            destinationTextView.setText(destinationAddress);
        }

        // Obtener el botón "Pedir un Pavill"
        Button btnRequestTaxi = findViewById(R.id.btnConfirm);
        btnRequestTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirigir a la actividad de búsqueda de taxi
                Intent intent = new Intent(ConfirmActivity.this, SearchingActivity.class);
                // Pasar las coordenadas a la actividad de búsqueda
                intent.putExtra("origin_lat", originCoordinates.latitude);
                intent.putExtra("origin_lng", originCoordinates.longitude);
                intent.putExtra("destination_lat", destinationCoordinates.latitude);
                intent.putExtra("destination_lng", destinationCoordinates.longitude);
                startActivity(intent);
            }
        });
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
