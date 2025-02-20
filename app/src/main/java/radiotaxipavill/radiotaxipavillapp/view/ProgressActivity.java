package radiotaxipavill.radiotaxipavillapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.BitmapUtils;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.ProgressController;
import radiotaxipavill.radiotaxipavillapp.controller.RouteController;
import radiotaxipavill.radiotaxipavillapp.receiver.PedidoStatusReceiver;
import radiotaxipavill.radiotaxipavillapp.services.PedidoStatusService;

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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class ProgressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originCoordinates; // Coordenadas de origen
    private LatLng destinationCoordinates; // Coordenadas de destino
    private GoogleMap mMap; // Mapa de Google
    private FusedLocationProviderClient fusedLocationClient; // Cliente de ubicación
    private LocationCallback locationCallback;// Callback para actualizaciones dinámicas de ubicación
    private Marker currentLocationMarker; // Marcador para la ubicación actual
    private ProgressController progressController; // Controlador de progreso
    private BottomSheetBehavior<View> bottomSheetBehavior; // Manejador del BottomSheet
    private Polyline routePolyline; // Polyline para la ruta
    private PedidoStatusReceiver pedidoStatusReceiver;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);  // 🔹 Restaurar datos guardados

        initializeData();
        initializeMap();
        initializeControllers();
        initializeButtons();
        initializeBottomSheet();

        // Iniciar el servicio de seguimiento de pedidos
        Intent serviceIntent = new Intent(this, PedidoStatusService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent); // Para Android 8+
        } else {
            this.startService(serviceIntent);
        }
    }

    /**
     * Registra el BroadcastReceiver para recibir actualizaciones del estado del pedido.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Inicializa el PedidoStatusReceiver con un callback para manejar los cambios de estado
        pedidoStatusReceiver = new PedidoStatusReceiver(status -> {
            if (status == null) return;

            switch (status) {
                case "CANCELADO":
                    Toast.makeText(ProgressActivity.this, "El pedido fue cancelado.", Toast.LENGTH_SHORT).show();
                    Intent cancelIntent = new Intent(ProgressActivity.this, MapActivity.class);
                    cancelIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(cancelIntent);
                    finish();
                    break;

                case "EN_ESPERA":
                    Toast.makeText(ProgressActivity.this, "El conductor canceló el pedido, buscando nuevo Pavill.", Toast.LENGTH_SHORT).show();
                    Intent searchIntent = new Intent(ProgressActivity.this, SearchingActivity.class);
                    searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(searchIntent);
                    finish();
                    break;

                default:
                    Log.d("ProgressActivity", "Estado del pedido: " + status);
                    break;
            }
        });

        // Registra el receptor con el intent-filter
        IntentFilter filter = new IntentFilter(PedidoStatusReceiver.ACTION_PEDIDO_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(pedidoStatusReceiver, filter);
    }

    /**
     * Desregistra el BroadcastReceiver cuando la actividad se pausa.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (pedidoStatusReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pedidoStatusReceiver);
        }
    }

    /**
     * Inicializa los datos necesarios para la actividad.
     */
    private void initializeData() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); // Inicializar el cliente de ubicación
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

    /**
     * Se utiliza para inicializar el mapa
     * @param googleMap Mapa de Google
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        applyMapStyle();


        if (checkPermission()) {
            mMap.setMyLocationEnabled(false); // Deshabilitamos la ubicación predeterminada de Google
        } else {
            requestLocationPermission();
        }



        // Dibujar la ruta estática
        drawStaticRoute();

        // Agregar marcadores personalizados para origen y destino
        addCustomMarkers();

        // Configurar la ubicación dinámica con el ícono personalizado
        startDynamicLocationUpdates();
    }

    /**
     * Aplica el estilo del mapa basado en el tema del sistema.
     */
    private void applyMapStyle() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // Modo nocturno: aplicar estilo oscuro
            try {
                boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
                if (!success) {
                    Log.e("MapActivity", "Error aplicando estilo oscuro.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("MapActivity", "Archivo de estilo oscuro no encontrado.", e);
            }
        } else {
            // Modo normal: no aplicar estilo (usar predeterminado)
            mMap.setMapStyle(null);
        }
    }

    /**
     * Dibuja la ruta estática entre el origen y el destino.
     */
    private void drawStaticRoute() {
        if (originCoordinates == null || destinationCoordinates == null) {
            Toast.makeText(this, "Origen o destino no definido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usar el controlador de rutas para obtener la ruta
        RouteController routeController = new RouteController(this);
        routeController.fetchRoute(originCoordinates, destinationCoordinates, null, new RouteController.RouteCallback() {
            @Override
            public void onRouteSuccess(List<LatLng> route, String distanceText, String durationText, double estimatedCost) {
                // Remover cualquier Polyline existente
                if (routePolyline != null) {
                    routePolyline.remove();
                }

                // Dibujar la ruta en el mapa
                routePolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10)
                        .color(getResources().getColor(R.color.secondaryColor)) // Color primario para la ruta
                        .geodesic(true));

                // Ajustar la cámara para mostrar la ruta completa
                LatLngBounds bounds = new LatLngBounds.Builder()
                        .include(originCoordinates)
                        .include(destinationCoordinates)
                        .build();
                int padding = 200; // Padding en píxeles
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }

            @Override
            public void onRouteError(String errorMessage) {
                Toast.makeText(ProgressActivity.this, "Error al obtener la ruta: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Agrega marcadores personalizados para el origen y el destino.
     */
    private void addCustomMarkers() {
        if (originCoordinates != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(originCoordinates)
                    .title("Origen")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))); // Color para el origen
        }

        if (destinationCoordinates != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(destinationCoordinates)
                    .title("Destino")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))); // Color para el destino
        }
    }


    /**
     * Verifica si se han otorgado los permisos de ubicación.
     * @return true si se han otorgado, false en caso contrario
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
     * @param destinationCoordinates Coordenadas de destino
     * @param btnActivateUbication Botón para compartir ubicación
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
     * Ubicación en tiempo real con icono personalizado
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

                // Actualizar o crear marcador dinámico para la ubicación actual
                if (currentLocationMarker == null) {
                    currentLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("Ubicación actual")
                            .icon(BitmapUtils.getProportionalBitmap(ProgressActivity.this, R.drawable.ic_car, 32))); // Ícono personalizado
                } else {
                    currentLocationMarker.setPosition(currentLatLng);
                }

                // Centrar la cámara en la ubicación actual
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}
