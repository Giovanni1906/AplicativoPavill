package com.example.Pavill.view;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.Pavill.R;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.ProgressController;
import com.example.Pavill.controller.RouteController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class ProgressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private GoogleMap mMap;
    private ProgressController progressController;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private Polyline routePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        // Obtener las coordenadas desde TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        originCoordinates = temporaryData.getOriginCoordinates();
        destinationCoordinates = temporaryData.getDestinationCoordinates();

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

        // Vincular el botón btnFinishTravel con el controlador
        AppCompatButton btnFinishTravel = findViewById(R.id.btnFinishTravel);
        btnFinishTravel.setOnClickListener(v -> {
            // Llamar al método finishTravel() del controlador
            progressController.finishTravel();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verificar permisos antes de desactivar la ubicación
        if (checkPermission()) {
            // Desactivar el círculo azul predeterminado de Google
            mMap.setMyLocationEnabled(false);
        } else {

            // Si no se tienen permisos, muestra un mensaje de advertencia o solicita los permisos
            Log.w("ProgressActivity", "Permisos de ubicación no otorgados.");
            requestLocationPermission();
        }

        // Añadir marcadores para origen y destino
        addMarkers();

        // Dibujar la ruta desde la API de Google Directions
        fetchAndDrawRoute();

        // Mostrar la ubicación actual con un ícono personalizado
        showCurrentLocation();
    }

    private void requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Se requieren permisos de ubicación para desactivar la capa predeterminada", Toast.LENGTH_SHORT).show();
        }
        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    /**
     * Verifica si los permisos de ubicación han sido otorgados.
     */
    private boolean checkPermission() {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Añade marcadores para el origen y destino.
     */
    private void addMarkers() {
        mMap.addMarker(new MarkerOptions()
                .position(originCoordinates)
                .title("Ubicación de origen")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        mMap.addMarker(new MarkerOptions()
                .position(destinationCoordinates)
                .title("Ubicación de destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Ajustar la cámara para mostrar ambos puntos
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(originCoordinates)
                .include(destinationCoordinates)
                .build();

        int padding = 200; // Padding en píxeles
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    /**
     * Muestra la ubicación actual con un ícono personalizado.
     */
    private void showCurrentLocation() {
        // Simula una ubicación actual (ajusta según sea necesario)
        LatLng currentLocation = originCoordinates;

        mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title("Tu ubicación actual")
                .icon(getCarIcon()));
    }

    /**
     * Obtiene la ruta desde la API de Google Directions y la dibuja en el mapa.
     */
    private void fetchAndDrawRoute() {
        String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + originCoordinates.latitude + "," + originCoordinates.longitude +
                "&destination=" + destinationCoordinates.latitude + "," + destinationCoordinates.longitude +
                "&key=" + getString(R.string.map_api_key);

        RouteController routeController = new RouteController(this);
        routeController.fetchRoute(directionsUrl, new RouteController.RouteCallback() {
            @Override
            public void onRouteReceived(List<LatLng> route) {
                if (routePolyline != null) {
                    routePolyline.remove();
                }

                routePolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10)
                        .color(getResources().getColor(R.color.secondaryColor))
                        .geodesic(true));
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProgressActivity.this, "Error al obtener la ruta: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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

    /**
     * Devuelve un icono personalizado redimensionado para la ubicación actual.
     */
    private BitmapDescriptor getCarIcon() {
        int width = 50; // Tamaño deseado en píxeles
        int height = 50;
        return BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_car), width, height, false));
    }
}