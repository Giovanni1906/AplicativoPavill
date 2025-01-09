package com.example.Pavill.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.example.Pavill.R;
import com.example.Pavill.components.BitmapUtils;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.ProgressController;
import com.example.Pavill.controller.RouteController;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import java.util.Locale;

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

        // Inicializar el cliente de ubicación
        AppCompatButton btnActivateUbication = findViewById(R.id.btnActivateUbication);

        ShareLocation(destinationCoordinates, btnActivateUbication);

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

    private void ShareLocation(LatLng destinationCoordinates, AppCompatButton btnActivateUbication){

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar el botón para enviar ubicación por WhatsApp
        btnActivateUbication.setOnClickListener(v -> {
            // Coordenadas simuladas de destino en Tacna
            double destinationLat = destinationCoordinates.latitude;
            double destinationLng = destinationCoordinates.longitude;

            // Obtener las coordenadas actuales
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Pedir permisos de ubicación si no están otorgados
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    // Coordenadas actuales reales
                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();

                    // Crear enlace de Google Maps
                    String googleMapsUrl = "https://www.google.com/maps/dir/?api=1" +
                            "&destination=" + destinationLat + "," + destinationLng +
                            "&waypoints=" + currentLat + "," + currentLng;

                    // Crear intent para enviar por WhatsApp
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Hola, estoy compartiendo mi ubicación en tiempo real y mi destino desde un Pavill: " + googleMapsUrl);
                    sendIntent.setType("text/plain");
                    sendIntent.setPackage("com.whatsapp");

                    try {
                        startActivity(sendIntent);
                    } catch (Exception e) {
                        // Mostrar mensaje si WhatsApp no está instalado
                        Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
                }
            });
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

        // Actualizar marcador del conductor
        int iconSize = 32; // Tamaño deseado para el ícono

        mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title("Tu ubicación actual")
                .icon(BitmapUtils.getProportionalBitmap(this, R.drawable.ic_car, iconSize)) // Llama al método utilitario
                .title("Conductor en camino"));
    }

    /**
     * Obtiene la ruta desde la API de Google Directions y la dibuja en el mapa.
     */
    private void fetchAndDrawRoute() {
        if (originCoordinates == null || destinationCoordinates == null) {
            Toast.makeText(this, "Origen o destino no definido.", Toast.LENGTH_SHORT).show();
            return;
        }

        RouteController routeController = new RouteController(this);
        routeController.fetchRoute(originCoordinates, destinationCoordinates, null, new RouteController.RouteCallback() {
            @Override
            public void onRouteSuccess(List<LatLng> route, String distanceText, String durationText, double estimatedCost) {
                // Remover la polyline existente, si hay
                if (routePolyline != null) {
                    routePolyline.remove();
                }

                // Crear y dibujar la nueva polyline en el mapa
                routePolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10)
                        .color(getResources().getColor(R.color.secondaryColor))
                        .geodesic(true));
            }

            @Override
            public void onRouteError(String errorMessage) {
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
}