package com.example.Pavill.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.Pavill.R;
import com.example.Pavill.components.PedidoCancellationHelper;
import com.example.Pavill.components.PedidoServiceHelper;
import com.example.Pavill.components.TemporaryData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

public class RatingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configuración de las estrellas para la calificación (Opcional)
        setupStarRating();

        // Configurar el botón de envío de calificación
        findViewById(R.id.btn_submit_rating).setOnClickListener(v -> {
            // Enviar la calificación y comentarios, luego regresar al MainActivity
            Toast.makeText(this, "Gracias por tu calificación", Toast.LENGTH_SHORT).show();

            //Marcar viaje finalizado
            PedidoServiceHelper.updateSubPedidoState(this, "Finalizado");
            PedidoCancellationHelper.cancelProcess(this);
            TemporaryData.getInstance().clearData();

            // Después de finalizar el viaje, redirigir a la MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            // Establecer flags para limpiar la pila de actividades y evitar retroceso
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.startActivity(intent);

            // Finalizar la actividad actual para asegurar que no se pueda volver atrás
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Verificar permisos de ubicación y habilitar la funcionalidad del mapa
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            // Solicitar permisos de ubicación
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Obtener la ubicación actual del usuario
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Mover la cámara a la ubicación actual
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    Toast.makeText(RatingActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Manejo del resultado de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Configurar las estrellas para la calificación
    private void setupStarRating() {
        ImageView[] stars = new ImageView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                // Cambiar el color de las estrellas seleccionadas a amarillo
                for (int j = 0; j <= index; j++) {
                    stars[j].setColorFilter(ContextCompat.getColor(this, com.google.android.libraries.places.R.color.quantum_yellow));
                }

                // Cambiar el color de las estrellas no seleccionadas a gris
                for (int j = index + 1; j < stars.length; j++) {
                    stars[j].setColorFilter(ContextCompat.getColor(this, com.google.android.libraries.places.R.color.quantum_grey));
                }
            });
        }
    }
}
