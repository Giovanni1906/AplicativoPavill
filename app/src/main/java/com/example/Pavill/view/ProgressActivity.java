package com.example.Pavill.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.Pavill.R;
import com.example.Pavill.controller.ProgressController;
import com.example.Pavill.components.TemporaryData; // Asegúrate de que la ruta sea correcta
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class ProgressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private GoogleMap mMap;
    private ProgressController progressController;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        // Obtener las coordenadas desde TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        originCoordinates = temporaryData.getOriginCoordinates();
        destinationCoordinates = temporaryData.getDestinationCoordinates();

        // Verificar si las coordenadas están disponibles
        if (originCoordinates == null || destinationCoordinates == null) {
            Log.e("ProgressActivity", "Coordenadas de origen o destino no disponibles en TemporaryData");
            return;
        }

        // Inicializar el controlador
        progressController = new ProgressController(this);

        // Inicializar el BottomSheet
        initializeBottomSheet();

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón para finalizar el viaje
        Button btnFinishTravel = findViewById(R.id.btnFinishTravel);
        btnFinishTravel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Llamar al controlador para finalizar el viaje
                progressController.finishTravel();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (originCoordinates != null && destinationCoordinates != null) {
            // Añadir marcadores para el origen y destino
            mMap.addMarker(new MarkerOptions()
                    .position(originCoordinates)
                    .title("Ubicación de origen")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            mMap.addMarker(new MarkerOptions()
                    .position(destinationCoordinates)
                    .title("Ubicación de Destino")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            // Definir límites del mapa para incluir ambos puntos
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(originCoordinates)
                    .include(destinationCoordinates)
                    .build();

            // Ajustar la cámara para mostrar ambos puntos
            int padding = 200; // Padding en píxeles (espacio alrededor de los puntos)
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } else {
            Log.e("ProgressActivity", "Coordenadas de origen o destino no disponibles");
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
