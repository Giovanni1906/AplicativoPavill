package com.example.Pavill.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class ProgressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker currentLocationMarker;
    private ProgressController progressController;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private Polyline routePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        initializeData();
        initializeMap();
        initializeControllers();
        initializeButtons();
        initializeBottomSheet();
    }

    /**
     * Inicializa los datos necesarios para la actividad.
     */
    private void initializeData() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        TemporaryData temporaryData = TemporaryData.getInstance();
        originCoordinates = temporaryData.getOriginCoordinates();
        destinationCoordinates = temporaryData.getDestinationCoordinates();

        if (originCoordinates == null || destinationCoordinates == null) {
            Log.e("ProgressActivity", "Coordenadas de origen o destino no disponibles en TemporaryData");
        }
    }

    /**
     * Inicializa el mapa y configura el callback.
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Inicializa los controladores.
     */
    private void initializeControllers() {
        progressController = new ProgressController(this);
    }

    /**
     * Inicializa los botones y sus listeners.
     */
    private void initializeButtons() {
        AppCompatButton btnActivateUbication = findViewById(R.id.btnActivateUbication);
        AppCompatButton btnFinishTravel = findViewById(R.id.btnFinishTravel);

        ShareLocation(destinationCoordinates, btnActivateUbication);

        btnFinishTravel.setOnClickListener(v -> progressController.finishTravel());
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (checkPermission()) {
            mMap.setMyLocationEnabled(false);
        } else {
            requestLocationPermission();
        }

        addMarkers();
        fetchAndDrawRoute();
        startDynamicLocationUpdates();
    }

    /**
     * Verifica si se han otorgado los permisos de ubicación.
     * @return
     */
    private boolean checkPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Solicita permisos de ubicación si no se han otorgado.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    /**
     * Comparte la ubicación actual con WhatsApp.
     * @param destinationCoordinates
     * @param btnActivateUbication
     */
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

    /**
     * Añade marcadores para el origen y destino.
     */
    private void addMarkers() {
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
     * Configura el inicio de actualizaciones dinámicas de ubicación.
     */
    private void startDynamicLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Intervalo de 10 segundos
        locationRequest.setFastestInterval(5000); // Intervalo rápido de 5 segundos
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || mMap == null) return;

                LatLng currentLatLng = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());

                // Actualizar o crear marcador de ubicación actual
                if (currentLocationMarker == null) {
                    currentLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("Tu ubicación actual")
                            .icon(BitmapUtils.getProportionalBitmap(ProgressActivity.this, R.drawable.ic_car, 32)));
                } else {
                    currentLocationMarker.setPosition(currentLatLng);
                }

                // Dibujar la línea de la ruta desde la ubicación actual al destino
                updateRoutePolyline(currentLatLng, destinationCoordinates);

                // Centrar la cámara opcionalmente
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    /**
     * Actualiza la Polyline que dibuja la ruta desde la ubicación actual al destino.
     *
     * @param currentLatLng La ubicación actual del usuario.
     * @param destinationLatLng La ubicación de destino.
     */
    private void updateRoutePolyline(LatLng currentLatLng, LatLng destinationLatLng) {
        if (routePolyline != null) {
            routePolyline.remove(); // Eliminar la línea anterior para evitar duplicados
        }

        // Usar el controlador de rutas para obtener la nueva ruta
        RouteController routeController = new RouteController(this);
        routeController.fetchRoute(currentLatLng, destinationLatLng, null, new RouteController.RouteCallback() {
            @Override
            public void onRouteSuccess(List<LatLng> route, String distanceText, String durationText, double estimatedCost) {
                // Dibujar la nueva Polyline
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
}
