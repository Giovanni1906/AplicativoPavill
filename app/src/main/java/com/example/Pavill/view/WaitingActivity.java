package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.Pavill.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class WaitingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private String driverName = "Juan Caballero";  // Obtener valor dinamicamente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Inicializar el BottomSheet
        initializeBottomSheet();

        // Obtener las coordenadas de origen y destino desde el Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double originLat = extras.getDouble("origin_lat");
            double originLng = extras.getDouble("origin_lng");
            double destinationLat = extras.getDouble("destination_lat");
            double destinationLng = extras.getDouble("destination_lng");

            originCoordinates = new LatLng(originLat, originLng);
            destinationCoordinates = new LatLng(destinationLat, destinationLng);
        }

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configurar el botón "A bordo"
        Button btnOnBoard = findViewById(R.id.btnOnBoard);
        btnOnBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar el ArrivalMessageDialog
                ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
                arrivalMessageDialog.setDriverName("Juan Pérez"); // Pasa el nombre del conductor aquí

                // Configurar el listener para el botón "OK"
                arrivalMessageDialog.setOnConfirmClickListener(new ArrivalMessageDialog.OnConfirmClickListener() {
                    @Override
                    public void onConfirmClick() {
                        // Ir a la actividad de progreso del viaje
                        Intent intent = new Intent(WaitingActivity.this, ProgressActivity.class);
                        intent.putExtra("origin_lat", originCoordinates.latitude);
                        intent.putExtra("origin_lng", originCoordinates.longitude);
                        intent.putExtra("destination_lat", destinationCoordinates.latitude);
                        intent.putExtra("destination_lng", destinationCoordinates.longitude);
                        startActivity(intent);
                        finish();
                    }
                });

                // Mostrar el cuadro de diálogo
                arrivalMessageDialog.show(getSupportFragmentManager(), "ArrivalMessageDialog");
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Colocar marcadores de origen y destino
        if (originCoordinates != null && destinationCoordinates != null) {
            mMap.addMarker(new MarkerOptions().position(originCoordinates).title("Origen"));
            mMap.addMarker(new MarkerOptions().position(destinationCoordinates).title("Destino"));

            // Mover la cámara para que ambos puntos sean visibles
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    new LatLngBounds.Builder()
                            .include(originCoordinates)
                            .include(destinationCoordinates)
                            .build(), 100));
        }
    }

    /**
     * Inicializa el BottomSheet para que se expanda y contraiga.
     */
    private void initializeBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(100); // Altura mínima visible del BottomSheet
        bottomSheetBehavior.setDraggable(true); // Permitir arrastrar el BottomSheet
    }
}
