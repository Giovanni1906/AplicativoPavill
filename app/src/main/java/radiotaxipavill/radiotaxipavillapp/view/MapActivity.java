package radiotaxipavill.radiotaxipavillapp.view;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.BitmapUtils;
import radiotaxipavill.radiotaxipavillapp.components.CustomInfoWindowAdapter;
import radiotaxipavill.radiotaxipavillapp.components.FriendlyAddressHelper;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.MyClusterItem;
import radiotaxipavill.radiotaxipavillapp.components.NavigationHeaderInfo;
import radiotaxipavill.radiotaxipavillapp.components.TaxiClusterRenderer;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.CalcularTarifaController;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;
import radiotaxipavill.radiotaxipavillapp.controller.NearbyTaxisController;
import radiotaxipavill.radiotaxipavillapp.controller.PlacesController;
import radiotaxipavill.radiotaxipavillapp.components.SuggestionsAdapter;
import radiotaxipavill.radiotaxipavillapp.controller.RouteController;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private EditText editTextOrigin, editTextDestination;
    private LinearLayout layoutSearchOrigin, layoutSearchDestination;
   // private RecyclerView recyclerViewSuggestionsOrigin, recyclerViewSuggestionsDestination;
    //private SuggestionsAdapter suggestionsAdapterOrigin, suggestionsAdapterDestination;
    private PlacesController placesController;

    // Variables del mapa y navegación
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ClusterManager<MyClusterItem> clusterManager;

    private String originAddress, destinationAddress;
    private String originPlaceId, destinationPlaceId;
    private LatLng originLatLng, destinationLatLng;

    private Marker originMarker; // Para almacenar el marcador del origen
    private Marker destinationMarker; // Para almacenar el marcador del destino
    private RouteController routeController;

    private Marker locationMarker; // Guardar el marcador para actualizarlo

    private boolean firstTimeLoading = true;

    private boolean isUseMapForOrigin = false; // Variable para determinar si es origen o destino
    private double defaultCost = 0.0; // costo por defecto

    private TemporaryData temporaryData;

    private LoadingDialog loadingDialog;

    private Button btnGoToDestination;
    private Button btnGoToOrigin;
    private Button btnRequestTaxi;

    private LinearLayout layout_buttons_origin;
    private LinearLayout layout_buttons_destination;


   // private ProgressBar progressBarSuggestionsOrigin;
   // private ProgressBar progressBarSuggestionsDestination;

    private AlertDialog currentDialog;


    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_map);

            loadingDialog = new LoadingDialog(this);

            // Inicializar la ubicación antes de cualquier otro proceso
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Inicializar datos y configuración antes de la UI
            preloadPlacesAPI(); // Pre-cargar Google Places para evitar demoras
            checkBatteryOptimization(); // Verificar optimización de batería
            temporaryData = TemporaryData.getInstance();
            temporaryData.loadFromPreferences(this); // Restaurar datos previos
            routeController = new RouteController(this);

            // Verificar permisos de ubicación ANTES de cargar el mapa
            if (!checkLocationPermission()) {
                requestLocationPermission();
            }

            // Inicializar UI y favoritos ANTES del mapa
            initializeUI();
            initializeBottomSheetAndDrawer();

            // Inicializar Places API (asegurarse que esté listo)
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), getString(R.string.map_api_key));
            }
            placesController = new PlacesController(this);

            // Verificar si la ubicación está activada ANTES de cargar el mapa
            checkIfLocationIsEnabled();

            // Cargar ubicación y luego inicializar el mapa
            getCurrentLocationAndLoadMap();

            // Configurar botones después de todo
            //setupLocationButtons();
            setupMapUseButtons();

        }

    private void getCurrentLocationAndLoadMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                initializeMap(); // ✅ Solo inicializar el mapa cuando la ubicación está listax
            } else {
                Log.w("MapActivity", "Ubicación no disponible, reintentando...");
                retryFetchingLocation(3); // 🔄 Reintentar 3 veces
            }
        }).addOnFailureListener(e -> {
            Log.e("MapActivity", "Error obteniendo ubicación: " + e.getMessage());
            retryFetchingLocation(3);
        });
    }
    private void retryFetchingLocation(int retries) {
        if (retries == 0) {
            Log.e("MapActivity", "No se pudo obtener la ubicación después de varios intentos.");
            originLatLng = new LatLng(-18.0066, -70.2467); // Usar Tacna si falla
            initializeMap();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                initializeMap();
            } else {
                Log.w("MapActivity", "Intento fallido, reintentando...");
                new Handler().postDelayed(() -> retryFetchingLocation(retries - 1), 2000);
            }
        });
    }



    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return; // Si no hay PowerManager, salir

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Verificar si existe una actividad que pueda manejar este Intent
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Log.w("MapActivity", "No se encontró una actividad para manejar la optimización de batería.");
                    }
                } catch (Exception e) {
                    Log.e("MapActivity", "Error al intentar abrir configuración de batería: " + e.getMessage());
                }
            }
        }
    }


    /**
     * Configura los botones para establecer la ubicación actual como origen o destino.
     */
    private void setupMapUseButtons() {
        // Button btnUseMapForOrigin = findViewById(R.id.btnUseMapForOrigin);
        // Button btnUseMapForDestination = findViewById(R.id.btnUseMapForDestination);

        Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        Button btnBack = findViewById(R.id.btn_back);
        View iconShadow = findViewById(R.id.icon_shadow);
        View iconCenterMarker = findViewById(R.id.icon_center_marker);
        CardView menuButtonLayout = findViewById(R.id.btnOpenSidebar);

        // Ocultar botones y vistas iniciales
        btnConfirmLocation.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        iconShadow.setVisibility(View.GONE);
        iconCenterMarker.setVisibility(View.GONE);

        // btnUseMapForOrigin.setVisibility(View.GONE);
        // btnUseMapForDestination.setVisibility(View.GONE);

        // Configurar los botones "Usar el mapa"
        //btnUseMapForOrigin.setOnClickListener(v -> handleUseMapForLocation(true));
       // btnUseMapForDestination.setOnClickListener(v -> handleUseMapForLocation(false));

        btnConfirmLocation.setOnClickListener(v -> {
            LatLng centerLatLng = mMap.getCameraPosition().target;

            if (isUseMapForOrigin) {
                originLatLng = centerLatLng;
                addMarkerToMap(centerLatLng, "Origen", ContextCompat.getColor(this, R.color.primaryColor), true);
                editTextOrigin.setText(getAddressFromLatLng(centerLatLng));
            } else {
                destinationLatLng = centerLatLng;
                addMarkerToMap(centerLatLng, "Destino", ContextCompat.getColor(this, R.color.secondaryColor), false);
                editTextDestination.setText(getAddressFromLatLng(centerLatLng));
            }

            // Restaurar la interfaz
            resetMapUI();
        });


        // Configurar el botón "Volver"
        btnBack.setOnClickListener(v -> {
            btnConfirmLocation.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            iconShadow.setVisibility(View.GONE);
            iconCenterMarker.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Expandir el BottomSheet
            bottomSheetBehavior.setDraggable(true); // Desbloquear el BottomSheet
            menuButtonLayout.setVisibility(View.VISIBLE);
        });
    }

    /**
     * resetear rutas
     */
    private void resetMapUI() {
        Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        Button btnBack = findViewById(R.id.btn_back);
        View iconShadow = findViewById(R.id.icon_shadow);
        ImageView iconCenterMarker = findViewById(R.id.icon_center_marker);
        CardView menuButtonLayout = findViewById(R.id.btnOpenSidebar);

        // Ocultar botones y vistas
        btnConfirmLocation.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        iconShadow.setVisibility(View.GONE);
        iconCenterMarker.setVisibility(View.GONE);

        // Restaurar BottomSheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(true);

        // Mostrar el menú
        menuButtonLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Maneja la acción de establecer la ubicación como origen o destino.
     */
    private void handleUseMapForLocation(boolean isForOrigin) {
        Log.e("MapActivity", "handleUseMapForLocation");

        isUseMapForOrigin = isForOrigin;
        Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        Button btnBack = findViewById(R.id.btn_back);
        View iconShadow = findViewById(R.id.icon_shadow);
        ImageView iconCenterMarker = findViewById(R.id.icon_center_marker);
        CardView menuButtonLayout = findViewById(R.id.btnOpenSidebar);

        // Bloquear el BottomSheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setDraggable(false);

        // Ocultar el botón del menú
        menuButtonLayout.setVisibility(View.GONE);

        // Determinar el color según si es origen o destino
        int color = isUseMapForOrigin
                ? ContextCompat.getColor(this, R.color.textColor)
                : ContextCompat.getColor(this, R.color.secondaryColor);

        // Cambiar el color del marcador antes de hacerlo visible
        if (iconCenterMarker instanceof ImageView) {
            iconCenterMarker.setColorFilter(color); // Aplicar color al ícono
            // Agregar una ligera sombra al marcador central

            iconCenterMarker.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Habilita sombras en software
            Paint paint = new Paint();
            paint.setShadowLayer(40, 0, 0, Color.BLACK); // Radio, desplazamiento X, desplazamiento Y, color de sombra
            iconCenterMarker.setLayerPaint(paint);

        } else {
            Log.e("MapActivity", "iconCenterMarker no es un ImageView.");
        }

        // Mostrar los botones y vistas necesarias
        btnConfirmLocation.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        iconShadow.setVisibility(View.VISIBLE);
        iconCenterMarker.setVisibility(View.VISIBLE);
        btnConfirmLocation.setText(isForOrigin ? "Marcar como Origen" : "Marcar como Destino");

        // Iniciar animación de ondas en iconShadow
        animateShadow(iconShadow);
    }

    /**
     * Aplica una animación de ondas al iconShadow.
     */
    private void animateShadow(View iconShadow) {
        // Crear animadores para escala y opacidad
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconShadow, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconShadow, "scaleY", 1f, 1.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(iconShadow, "alpha", 1f, 0f);

        // Configurar duración y repetición en cada ObjectAnimator
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(1000);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        alpha.setRepeatMode(ObjectAnimator.RESTART);

        // Combinar animaciones en un AnimatorSet
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setInterpolator(new LinearInterpolator());

        // Iniciar animación
        animatorSet.start();
    }


    /**
     * Obtiene la dirección a partir de una latitud y longitud
     * @param latLng
     * @return
     */
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0); // Dirección completa
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Sin dirección disponible";
    }
    /**
     * Actualiza la ruta en el mapa
     */
    private void updateRoute() {
        if (originLatLng != null && destinationLatLng != null) {
            // Llamar al controlador de rutas para calcular y trazar la ruta
            routeController.fetchRoute(originLatLng, destinationLatLng, mMap, new RouteController.RouteCallback() {
                @Override
                public void onRouteSuccess(List<LatLng> polylinePoints, String distanceText, String durationText, double estimatedCost) {
                    Log.d("Route", "Ruta trazada con éxito: " + distanceText + ", " + durationText);
                    temporaryData.setDistance(distanceText, MapActivity.this);
                    temporaryData.setDuration(durationText, MapActivity.this);
//                  temporaryData.setEstimatedCost(String.format(Locale.getDefault(), "s/ %.2f", estimatedCost));
                    defaultCost = estimatedCost;
                }

                @Override
                public void onRouteError(String errorMessage) {
                    Toast.makeText(MapActivity.this, "Error al trazar la ruta: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.d("updateRoute", "Origen o destino no definidos. No se puede trazar la ruta.");
        }
    }

    /**
     * Borra la ruta del mapa si existe.
     */
    private void clearRoute() {
        if (routeController != null) {
            routeController.clearRoute();
        }
    }

    /**
     * Configura los botones para establecer la ubicación actual como origen o destino.
     */

    /**
    private void setupLocationButtons() {
        Button btnSetOriginToCurrentLocation = findViewById(R.id.btnSetOriginToCurrentLocation);
        Button btnSetDestinationToCurrentLocation = findViewById(R.id.btnSetDestinationToCurrentLocation);

        btnSetOriginToCurrentLocation.setOnClickListener(v -> setLocationAsOrigin());
        btnSetDestinationToCurrentLocation.setOnClickListener(v -> setLocationAsDestination());

        btnSetOriginToCurrentLocation.setVisibility(View.GONE);
    }
    */

    /**
     * Establece la ubicación actual como origen.
     */
    private void setLocationAsOrigin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no otorgado", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                originLatLng = currentLatLng;

                // Actualizar marcador y dirección
                addMarkerToMap(currentLatLng, "Origen", getResources().getColor(R.color.primaryColor), true);

                FriendlyAddressHelper.getFriendlyAddress(this, currentLatLng.latitude, currentLatLng.longitude, new FriendlyAddressHelper.AddressCallback() {
                    @Override
                    public void onAddressRetrieved(String friendlyAddress) {
                        originAddress = friendlyAddress;
                        editTextOrigin.setText(friendlyAddress);
                        Log.d("OriginSet", "Origen actualizado: " + friendlyAddress);
                        updateRoute(); // Llama a updateRoute cuando se haya configurado el origen
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MapActivity.this, "Error al obtener la dirección del origen", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Establece la ubicación actual como destino.
     */
    private void setLocationAsDestination() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no otorgado", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                destinationLatLng = currentLatLng;

                // Actualizar marcador y dirección
                addMarkerToMap(currentLatLng, "Destino", getResources().getColor(R.color.secondaryColor), false);

                FriendlyAddressHelper.getFriendlyAddress(this, currentLatLng.latitude, currentLatLng.longitude, new FriendlyAddressHelper.AddressCallback() {
                    @Override
                    public void onAddressRetrieved(String friendlyAddress) {
                        destinationAddress = friendlyAddress;
                        editTextDestination.setText(friendlyAddress);
                        Log.d("DestinationSet", "Destino actualizado: " + friendlyAddress);
                        updateRoute(); // Llama a updateRoute cuando se haya configurado el destino

                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MapActivity.this, "Error al obtener la dirección del destino", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Inicializa el mapa
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync((OnMapReadyCallback) this);
        }
    }

    /**
     * Callback cuando el mapa está listo
     * @param googleMap
     */
    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar estilo del mapa
        applyMapStyle();

        // Verificar permisos de ubicación
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);

            // Ocultar el botón predeterminado de "ir a mi ubicación actual"
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Configurar ClusterManager
            setupClusterManager();

            // Obtener y manejar taxis cercanos
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    getCurrentLocationAndCenterMap();
                    fetchNearbyTaxis(currentLocation);
                } else {
                    Toast.makeText(this, "Solicitando ubicación actual...", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            requestLocationPermission();
        }
    }


    /**
     * Configura el ClusterManager para manejar clusters de taxis.
     */
    private void setupClusterManager() {
        clusterManager = new ClusterManager<>(this, mMap);
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);

        int iconSize = 32; // Tamaño deseado para el ícono

        // Configurar renderer personalizado
        BitmapDescriptor taxiIcon = BitmapUtils.getProportionalBitmap(this, R.drawable.ic_car, iconSize);
        TaxiClusterRenderer renderer = new TaxiClusterRenderer(this, mMap, clusterManager, taxiIcon);
        clusterManager.setRenderer(renderer);

        // Asignar el InfoWindowAdapter al ClusterManager para mensaje del icono personalizado
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter(getLayoutInflater()));

        // Aplicar clustering
        clusterManager.cluster();
    }

    /**
     * Llama al controlador para obtener taxis cercanos y los maneja.
     */
    private void fetchNearbyTaxis(LatLng currentLocation) {
        new NearbyTaxisController().fetchNearbyTaxis(this, currentLocation, new NearbyTaxisController.NearbyTaxisCallback() {
            @Override
            public void onTaxisReceived(List<MyClusterItem> taxis) {
                clusterManager.clearItems();
                clusterManager.addItems(taxis);
                clusterManager.cluster();
            }

            @Override
            public void onNoTaxisFound(String message) {
                Toast.makeText(MapActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
     * Solicita el permiso de ubicación al usuario
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            
            // Si es la primera vez que se solicita el permiso, mostramos explicación
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar diálogo explicativo antes de solicitar el permiso
                showLocationPermissionDialog();
            } else {
                // Solicitar el permiso directamente
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Muestra un diálogo explicativo sobre por qué se necesita el permiso de ubicación
     */
    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de Ubicación Requerido")
                .setMessage("Esta aplicación necesita acceso a tu ubicación para mostrar taxis cercanos y calcular rutas. Por favor, otorga el permiso de ubicación para continuar.")
                .setPositiveButton("Otorgar Permiso", (dialog, which) -> {
                    // Solicitar el permiso después de que el usuario confirme
                    ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                        LOCATION_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Sin permisos de ubicación, algunas funciones no estarán disponibles.", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Muestra un diálogo para ir a la configuración de la aplicación
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso Denegado Permanentemente")
                .setMessage("Has denegado el permiso de ubicación permanentemente. Para usar todas las funciones de la aplicación, ve a Configuración > Aplicaciones > Pavill Taxi > Permisos y habilita la ubicación.")
                .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Verifica si el permiso de ubicación está otorgado
     * @return true si el permiso está otorgado, false si no
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verifica si el GPS está activado
     */
    private void checkIfLocationIsEnabled() {
        // Comprobar si el GPS está activado
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            // Mostrar un cuadro de diálogo para activar la ubicación
            Toast.makeText(this, "La ubicación está desactivada. Por favor actívela.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            firstTimeLoading = true;
            getCurrentLocationAndCenterMap();
        }
    }

    /**
     * Obtiene la ubicación actual y centra el mapa con zoom más alto y ajusta la posición.
     */
    @SuppressLint("MissingPermission")
    private void getCurrentLocationAndCenterMap() {
        Log.e("MapActivity", "getCurrentLocationAndCenterMap");

        if (fusedLocationClient == null) {
            Log.e("MapActivity", "FusedLocationProviderClient es NULL, inicializando...");
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        // Obtener ubicación rápida (para mostrar algo inmediato)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateMapWithLocation(location);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Location", "Error obteniendo ubicación inicial", e);
                    requestFastLocationUpdate(); // Forzar actualización si hay un error
                });

        // forzar una actualización precisa en segundo plano
        requestFastLocationUpdate();
        // Asegurar que el círculo azul aparece inmediatamente
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Método para actualizar la ubicación en el mapa y centrar la cámara en la ubicación actual.
     */
    private void updateMapWithLocation(Location location) {
        Log.e("MapActivity", "updateMapWithLocation");

        if (mMap == null) {
            Log.e("MapActivity", "mMap es NULL, no se puede actualizar la ubicación.");
            return;
        }

        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        Log.e("MapActivity", "getLatitude: "+String.valueOf(location.getLatitude()));
        Log.e("MapActivity", "getLongitude: "+String.valueOf(location.getLongitude()));

        // Asegurar que el círculo azul y el `ic_here` estén sincronizados
        if (locationMarker == null) {
            locationMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .icon(BitmapUtils.getProportionalBitmap(this, R.drawable.ic_here, 56))
                    .anchor(0.5f, 1.0f) // Centrar el icono
            );
        } else {
            locationMarker.setPosition(currentLocation);
        }

        // Asegurar que el círculo azul también aparece más rápido
        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }

        // Mover la cámara solo si es la primera vez o el usuario no ha movido el mapa
        if (firstTimeLoading) {
            Log.e("MapActivity", "firstTimeLoading SI");

            //LatLng latLng = new LatLng(Double.parseDouble(ClienteCoordenadaX),Double.parseDouble(ClienteCoordenadaY));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentLocation)      // Sets the center of the map to Mountain Viewf
                    .zoom(19)                   // Sets the zoom
                    //.bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
            // Desplaza la vista para que el marcador no quede oculto por la interfaz
            //new Handler().postDelayed(() -> mMap.animateCamera(CameraUpdateFactory.scrollBy(0, 250)), 500);

            firstTimeLoading = false;
        }else{
            Log.e("MapActivity", "firstTimeLoading NO");
        }
    }

    /**
     *  🚀 Método que fuerza la obtención de ubicación inmediatamente si `getLastLocation()` falla.
     */
    @SuppressLint("MissingPermission")
    private void requestFastLocationUpdate() {
        Log.e("MapActivity", "requestFastLocationUpdate");

        LocationRequest locationRequest;
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location loc : locationResult.getLocations()) {
                    updateMapWithLocation(loc);
                    fusedLocationClient.removeLocationUpdates(this); // Detener actualizaciones tras recibir la primera ubicación
                    break;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
                    .setMinUpdateIntervalMillis(500) // 🔥 Obtener ubicación lo más rápido posible
                    .setWaitForAccurateLocation(false) // No esperar demasiado por precisión extrema
                    .build();
        } else {
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(500) // 🔥 Obtener ubicación rápido
                    .setFastestInterval(500);
        }

        // Solicitar una única actualización rápida
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    /**
     * Maneja la respuesta del permiso de ubicación
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado
                Toast.makeText(this, "Permiso de ubicación otorgado.", Toast.LENGTH_SHORT).show();
                checkIfLocationIsEnabled();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación denegado. Algunas funciones no estarán disponibles.", Toast.LENGTH_LONG).show();
                
                // Mostrar diálogo para ir a configuración si el usuario denegó permanentemente
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showSettingsDialog();
                }
            }
        }
    }

    /**
     * Inicializa la interfaz de usuario
     */
    private void initializeUI() {
        editTextOrigin = findViewById(R.id.editTextOrigin);
        editTextDestination = findViewById(R.id.editTextDestination);

        //progressBarSuggestionsOrigin = findViewById(R.id.progressBarSuggestionsOrigin);
        //progressBarSuggestionsDestination = findViewById(R.id.progressBarSuggestionsDestination);

        CardView btnMyLocation = findViewById(R.id.btn_my_location);

        ImageButton btnDeleteOrigin = findViewById(R.id.btnDeleteOrigin);
        ImageButton btnDeleteDestination = findViewById(R.id.btnDeleteDestination);

        btnRequestTaxi = findViewById(R.id.btnRequestTaxi);
        // btnGoToDestination = findViewById(R.id.btnGoToDestination);
        // btnGoToOrigin = findViewById(R.id.btnGoToOrigin);

        layoutSearchOrigin = findViewById(R.id.layoutSearchOrigin);
        // layout_buttons_origin = findViewById(R.id.layout_buttons_origin);
        //recyclerViewSuggestionsOrigin = findViewById(R.id.recyclerViewSuggestionsOrigin);

        layoutSearchDestination = findViewById(R.id.layoutSearchDestination);
        // layout_buttons_destination = findViewById(R.id.layout_buttons_destination);
       // recyclerViewSuggestionsDestination = findViewById(R.id.recyclerViewSuggestionsDestination);

        // Obtener el TextView
        TextView textViewName = findViewById(R.id.textViewName);

        // Recuperar el nombre del usuario desde SharedPreferences
        SharedPreferences sharedPreferences = this.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String clienteNombre = sharedPreferences.getString("ClienteNombre", "cliente");

        // Establecer el texto en el TextView
        textViewName.setText("Hola " + clienteNombre+". ¿A dónde deseas ir?");

        // Inicializar adaptadores de sugerencias
        //initializeSuggestionsAdapterOrigin();
       // initializeSuggestionsAdapterDestination();

        // Configurar listeners para los inputs
        setupTextWatchers();
        
        // Configurar click listeners para abrir diálogos de búsqueda
        editTextOrigin.setOnClickListener(v -> {
            Log.d("ClickListener", "editTextOrigin clickeado, abriendo ventana de búsqueda");
            openAddressSearchDialog(true); // true para origen
        });
        
        editTextDestination.setOnClickListener(v -> {
            Log.d("ClickListener", "editTextDestination clickeado, abriendo ventana de búsqueda");
            openAddressSearchDialog(false); // false para destino
        });

        // botones para eliminar inputs de origen y destino
        DeleteInputsText(btnDeleteOrigin, btnDeleteDestination);

        // Botón para ir a la ubicación actual
        GotoMyLocation(btnMyLocation);

        //guardar datos en Temporary Data y pasar al Confirn Activity
        RequestTaxi(btnRequestTaxi);
        // cambiar de origen a destino
        ChangeOptions();
    }

    /**
     * Cambiar de origen a destino
     * @params btnGoToOrigin, btnGoToDestination
     */
    private void ChangeOptions() {
       // btnGoToOrigin.setOnClickListener(v -> toggleSearchVisibility(true));
       // btnGoToDestination.setOnClickListener(v -> toggleSearchVisibility(false));
    }


    /**
     * Conectar el botón para solicitar un taxi
      * @param btnRequestTaxi
     */
    private void RequestTaxi(Button btnRequestTaxi) {
        btnRequestTaxi.setOnClickListener(v -> {
            if (originLatLng != null) { // validación para origen
            //if (originLatLng != null && destinationLatLng != null) { // validación para origen y destino
                // Guardar las coordenadas en TemporaryData
                temporaryData.setOriginCoordinates(originLatLng, MapActivity.this);
                if (destinationLatLng != null) {
                    temporaryData.setDestinationCoordinates(destinationLatLng, MapActivity.this);
                } else {
                    temporaryData.setDestinationCoordinates(null, MapActivity.this);
                }
                CalculateAproximatedCost(originLatLng, destinationLatLng);

            } else {
                Toast.makeText(this, "Por favor selecciona la ubicación de origen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void CalculateAproximatedCost(LatLng origin, LatLng destination) {

        // Validar coordenadas no nulas
        if (destination == null) {
            Log.e("CalculateAproximatedCost", "Coordenadas de destino no válidas.");
            Intent intent = new Intent(MapActivity.this, ConfirmActivity.class);
            // Solo pasar la dirección si proviene de un favorito
            startActivity(intent);
            return;
        }
        loadingDialog.show();

        double originLat = origin.latitude;
        double originLng = origin.longitude;
        double destLat = destination.latitude;
        double destLng = destination.longitude;

        // Llamar al controlador
        new CalcularTarifaController().calcularTarifa(this, originLat, originLng, destLat, destLng, new CalcularTarifaController.CalcularTarifaCallback() {
            @Override
            public void onSuccess(String tarifario, String respuesta) {
                loadingDialog.dismiss();


                Log.d("CalculateAproximatedCost", "Respuesta " + respuesta + ", Monto: " + tarifario);

                temporaryData.setEstimatedCost(tarifario, MapActivity.this);

                Intent intent = new Intent(MapActivity.this, ConfirmActivity.class);
                // Solo pasar la dirección si proviene de un favorito
                startActivity(intent);
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();

                temporaryData.setEstimatedCost( "N/A", MapActivity.this);
                Log.e("CalculateAproximatedCost", "Error: " + errorMessage + "monto: " + defaultCost);
                // Navegar a ConfirmActivity
                Intent intent = new Intent(MapActivity.this, ConfirmActivity.class);
                // Solo pasar la dirección si proviene de un favorito
                startActivity(intent);
            }
        });
    }

    /**
     * Configura el botón para ir a la ubicación actual
     * @param btnMyLocation
     */
    private void GotoMyLocation(CardView btnMyLocation) {
        btnMyLocation.setOnClickListener(v -> {
            if (mMap != null && checkLocationPermission()) {
                firstTimeLoading = true;
                getCurrentLocationAndCenterMap(); // Centrar el mapa en la ubicación actual
            } else {
                requestLocationPermission();
            }
        });
    }

    /**
     * Inicializa el adaptador y RecyclerView para las sugerencias de origen.
     */
   /* private void initializeSuggestionsAdapterOrigin() {

        Log.d("MapActivity", "initializeSuggestionsAdapterOrigin");

        suggestionsAdapterOrigin = new SuggestionsAdapter(new ArrayList<>(), suggestion -> {

            originAddress = suggestion.getFullText(null).toString();
            originPlaceId = suggestion.getPlaceId();

            // Obtener LatLng del Place ID y marcar en el mapa
            placesController.getLatLngFromPlaceId(originPlaceId, new PlacesController.LatLngCallback() {
                @Override
                public void onLatLngFetched(LatLng latLng) {
                    originLatLng = latLng;
                    addMarkerToMap(originLatLng, "Origen", getResources().getColor(R.color.primaryColor), true);
                    Log.d("OriginSelection", "Marcador de origen añadido: " + originLatLng);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e("OriginSelection", "Error al obtener LatLng para el origen: " + exception.getMessage());
                }
            });

            editTextOrigin.setText(originAddress);

            Log.e("OriginSelection", "Dirección de origen seleccionada: " + originAddress);

        }, () -> {
            // Callback para cuando se selecciona una sugerencia
            Log.e("OriginSelection", "setVisibility" );
           recyclerViewSuggestionsOrigin.setVisibility(View.GONE);
           progressBarSuggestionsOrigin.setVisibility(View.GONE);
        });

        recyclerViewSuggestionsOrigin.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsOrigin.setAdapter(suggestionsAdapterOrigin);

        // Agregar divisor al RecyclerView
        recyclerViewSuggestionsOrigin.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }*/

    /**
     * Inicializa el adaptador y RecyclerView para las sugerencias de destino.
     */
   /* private void initializeSuggestionsAdapterDestination() {

        suggestionsAdapterDestination = new SuggestionsAdapter(new ArrayList<>(), suggestion -> {
            destinationAddress = suggestion.getFullText(null).toString();
            destinationPlaceId = suggestion.getPlaceId();

            // Obtener LatLng del Place ID y marcar en el mapa
            placesController.getLatLngFromPlaceId(destinationPlaceId, new PlacesController.LatLngCallback() {
                @Override
                public void onLatLngFetched(LatLng latLng) {
                    destinationLatLng = latLng;
                    addMarkerToMap(destinationLatLng, "Destino", getResources().getColor(R.color.secondaryColor), false);
                    Log.d("DestinationSelection", "Marcador de destino añadido: " + destinationLatLng);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e("DestinationSelection", "Error al obtener LatLng para el destino: " + exception.getMessage());
                }
            });

            editTextDestination.setText(destinationAddress);
            Log.d("DestinationSelection", "Dirección de destino seleccionada: " + destinationAddress);
        }, () -> {
            // Callback para cuando se selecciona una sugerencia
            recyclerViewSuggestionsDestination.setVisibility(View.GONE);
            progressBarSuggestionsDestination.setVisibility(View.GONE);
        });

        recyclerViewSuggestionsDestination.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsDestination.setAdapter(suggestionsAdapterDestination);

        // Agregar divisor al RecyclerView
        recyclerViewSuggestionsDestination.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }*/


    /**
     * Agrega un marcador al mapa en la ubicación especificada y centra el mapa en esa posición.
     *
     * @param latLng  Coordenadas de la ubicación.
     * @param title   Título del marcador.
     * @param color   Color del marcador.
     * @param isOrigin Indica si el marcador es de origen (true) o destino (false).
     */
    private void addMarkerToMap(LatLng latLng, String title, int color, boolean isOrigin) {
        if (latLng == null || mMap == null) {
            Log.e("addMarkerToMap", "LatLng o mapa nulo, no se puede agregar el marcador.");
            return;
        }

        // Convertir color a hue
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // Eliminar marcador anterior si es necesario
        if (isOrigin) {
            if (originMarker != null) {
                originMarker.remove();
            }
            originMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(hsv[0])));
            originLatLng = latLng; // Actualizar coordenadas de origen

            if (locationMarker != null) {
                locationMarker.setAlpha(0.3f); // Reducir opacidad del ic_here al 30%
            }

        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(hsv[0])));
            destinationLatLng = latLng; // Actualizar coordenadas de destino
        }

        // Enfocar el mapa en el marcador con animación
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        // Intentar actualizar la ruta
        updateRoute();
    }


    /**
     * Elimina el texto de los inputs
     * @param btnDeleteOrigin
     * @param btnDeleteDestination
     */
    private void DeleteInputsText(ImageButton btnDeleteOrigin, ImageButton btnDeleteDestination) {
        // Configurar listeners para los botones de eliminar
        btnDeleteOrigin.setOnClickListener(v -> {
            editTextOrigin.setText(""); // Limpia el texto del origen
            if (originMarker != null) {
                originMarker.remove();
            }
            originLatLng = null;
            if (locationMarker != null) {
                locationMarker.setAlpha(1.0f); // volver opacidad de ic_here al 100%
            }
            clearRoute(); // Borra la ruta
            Log.d("DeleteInput", "Texto de origen eliminado");
        });

        btnDeleteDestination.setOnClickListener(v -> {
            editTextDestination.setText(""); // Limpia el texto del destino
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            destinationLatLng = null;
            clearRoute(); // Borra la ruta
            Log.d("DeleteInput", "Texto de destino eliminado");
        });

    }

    /**
     * Cierra el teclado
     */

  /*  private void closeKeyboard() {
        // Cerrar el teclado
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
*/
    /**
     * Configura los listeners para los inputs de texto
     */
    private void setupTextWatchers() {
        // TextWatcher para el input de origen
        editTextOrigin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() >= 3) {
                    Log.e("setupTextWatchers", "length>3");

                   // progressBarSuggestionsOrigin.setVisibility(View.VISIBLE); // Muestra el indicador de carga

                    placesController.getPredictions(s.toString(), predictions -> {
                       // suggestionsAdapterOrigin.updateSuggestions(predictions);
                       // recyclerViewSuggestionsOrigin.setVisibility(View.VISIBLE);
                       // progressBarSuggestionsOrigin.setVisibility(View.GONE); // Oculta el indicador al recibir datos
                    });
                } else {
                    //Si el texto es menor a 3 caracteres, ocultar todo
                   // progressBarSuggestionsOrigin.setVisibility(View.GONE);
                   // recyclerViewSuggestionsOrigin.setVisibility(View.GONE);
                   // suggestionsAdapterOrigin.updateSuggestions(new ArrayList<>()); // Limpiar sugerencias
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // El editTextOrigin es readonly, solo responde a clicks
        // No necesita OnFocusChangeListener ya que no puede recibir foco

        // TextWatcher para el input de destino
        editTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                   // progressBarSuggestionsDestination.setVisibility(View.VISIBLE); // Muestra el indicador de carga

                    placesController.getPredictions(s.toString(), predictions -> {
                       // suggestionsAdapterDestination.updateSuggestions(predictions);
                       // recyclerViewSuggestionsDestination.setVisibility(View.VISIBLE);
                       // progressBarSuggestionsDestination.setVisibility(View.GONE); // Oculta el indicador al recibir datos

                    });
                }else {
                    // Si el texto es menor a 3 caracteres, ocultar todo
                   // progressBarSuggestionsDestination.setVisibility(View.GONE);
                   // recyclerViewSuggestionsDestination.setVisibility(View.GONE);
                  //  suggestionsAdapterDestination.updateSuggestions(new ArrayList<>()); // Limpiar sugerencias
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // El editTextDestination es readonly, solo responde a clicks
        // No necesita OnFocusChangeListener ya que no puede recibir foco
    }

    /**
     * Abre un diálogo para buscar direcciones
     * @param isOrigin true si es para origen, false si es para destino
     */
    private void openAddressSearchDialog(boolean isOrigin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_address_search, null);

        TextView lblTitle = dialogView.findViewById(R.id.lblSearchAddressTittle);
        EditText searchEditText = dialogView.findViewById(R.id.searchEditText);
        RecyclerView suggestionsRecyclerView = dialogView.findViewById(R.id.suggestionsRecyclerView);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        Button btnSelectFromMap = dialogView.findViewById(R.id.btnSelectFromMap);
        ImageButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        lblTitle.setText(isOrigin ? "Dirección de origen" : "Dirección de destino");

        // Adapter de sugerencias: solo delega a applyPlaceSelection(...)
        SuggestionsAdapter suggestionsAdapter = new SuggestionsAdapter(
                new ArrayList<>(),
                suggestion -> {
                    String address = suggestion.getFullText(null).toString();
                    String placeId = suggestion.getPlaceId();
                    applyPlaceSelection(isOrigin, address, placeId);
                    if (currentDialog != null) currentDialog.dismiss();
                },
                () -> { if (currentDialog != null) currentDialog.dismiss(); }
        );

        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);
        suggestionsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Reutiliza el helper genérico para predicciones
        wirePredictions(searchEditText, progressBar, suggestionsRecyclerView, suggestionsAdapter);

        // Cerrar
        btnCloseDialog.setOnClickListener(v -> { if (currentDialog != null) currentDialog.dismiss(); });

        // Seleccionar desde mapa → reutiliza tu flujo ya existente (sin duplicar UI)
        btnSelectFromMap.setOnClickListener(v -> {
            if (currentDialog != null) currentDialog.dismiss();
            enableMapSelectionMode(isOrigin); // ya enfoca y gestiona selección (o puedes llamar handleUseMapForLocation)
        });

        builder.setView(dialogView);
        currentDialog = builder.create();
        currentDialog.setOnShowListener(d -> {
            Window w = currentDialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.CENTER;
                w.setAttributes(lp);
            }
        });

        currentDialog.show();

        // Teclado
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Helper para predicciones
     * @param input
     * @param progress
     * @param list
     * @param adapter
     */
    private void wirePredictions(EditText input, ProgressBar progress, RecyclerView list, SuggestionsAdapter adapter) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    progress.setVisibility(View.VISIBLE);
                    placesController.getPredictions(s.toString(), predictions -> {
                        adapter.updateSuggestions(predictions);
                        list.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    });
                } else {
                    progress.setVisibility(View.GONE);
                    list.setVisibility(View.GONE);
                    adapter.updateSuggestions(new ArrayList<>());
                }
            }
        });
    }

    /**
     * Helper para marcar en mapa y actualizar estado/UI sin duplicar
     * @param isOrigin
     * @param address
     * @param latLng
     */
    private void setAddressAndMarker(boolean isOrigin, String address, LatLng latLng) {
        if (latLng == null || mMap == null) return;

        if (isOrigin) {
            originLatLng = latLng;
            addMarkerToMap(latLng, "Origen", getResources().getColor(R.color.primaryColor), true);
        } else {
            destinationLatLng = latLng;
            addMarkerToMap(latLng, "Destino", getResources().getColor(R.color.secondaryColor), false);
        }

        // Si tienes un “enfoque” al entrar en selección, puedes reutilizarlo aquí si quieres
        // focusCameraForSelection(latLng);

        // La propia addMarkerToMap() ya llama updateRoute(); no hace falta repetirlo
    }

    /**
     * Helper para aplicar la selección de un Place (origen/destino)
     * @param isOrigin
     * @param address
     * @param placeId
     */
    private void applyPlaceSelection(boolean isOrigin, String address, String placeId) {
        // Setea textos y placeId inmediatamente (feedback rápido)
        if (isOrigin) {
            originAddress = address;
            originPlaceId = placeId;
            editTextOrigin.setText(address);
        } else {
            destinationAddress = address;
            destinationPlaceId = placeId;
            editTextDestination.setText(address);
        }

        // Resolve LatLng y delega colocar marcador + actualizar ruta
        placesController.getLatLngFromPlaceId(placeId, new PlacesController.LatLngCallback() {
            @Override
            public void onLatLngFetched(LatLng latLng) {
                setAddressAndMarker(isOrigin, address, latLng);
            }

            @Override
            public void onError(Exception exception) {
                Log.e("PlaceSelection", "Error obteniendo LatLng: " + exception.getMessage());
                Toast.makeText(MapActivity.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Habilita el modo de selección del mapa para seleccionar origen o destino
     * @param isOrigin true si es para origen, false si es para destino
     */
    private void enableMapSelectionMode(boolean isOrigin) {
        Log.d("MapSelection", "Habilitando modo selección del mapa para " + (isOrigin ? "origen" : "destino"));
        
        // Mostrar mensaje al usuario
        String message = isOrigin ? 
            "Toca en el mapa para seleccionar la ubicación de origen" : 
            "Toca en el mapa para seleccionar la ubicación de destino";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // Configurar el listener de clics del mapa
        if (mMap != null) {

            if (isOrigin) {
                handleUseMapForLocation(true);
            }else{
                handleUseMapForLocation(false);
            }
        }
    }

    /**
     * Deshabilita el modo de selección del mapa
     */
    private void disableMapSelectionMode() {
        Log.d("MapSelection", "Deshabilitando modo selección del mapa");
        if (mMap != null) {
            mMap.setOnMapClickListener(null);
        }
    }

    /**
     * Inicializa el BottomSheet y el DrawerLayout
     */
    private void initializeBottomSheetAndDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Botón del menú lateral
        CardView btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        if (btnOpenSidebar != null) {
            btnOpenSidebar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(findViewById(R.id.nav_view));
                }
            });
        }

        // Inicializar BottomSheet
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(100);

        setupNavigationView();
    }

    /**
     * Configura la navegación
     */
    private void setupNavigationView() {
        // Configurar el encabezado
        NavigationHeaderInfo.setupHeader(this, navigationView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    startActivity(new Intent(MapActivity.this, ProfileActivity.class));
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(MapActivity.this, HistoryActivity.class));
                } else if (id == R.id.nav_points) {
                    startActivity(new Intent(MapActivity.this, PointsActivity.class));
                } else if (id == R.id.nav_favorites) {
                    startActivity(new Intent(MapActivity.this, FavoritesActivity.class));
                } else if (id == R.id.nav_logout) {
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();
                    Intent logoutIntent = new Intent(MapActivity.this, MainActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                }

                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });
    }

    /**
     * Alterna la visibilidad de los botones e inputs de origen y destino.
     * @param isOriginActive true si se activa origen, false si se activa destino.
     */
    /*private void toggleSearchVisibility(boolean isOriginActive) {
        // Mostrar elementos de origen y ocultar destino si isOriginActive es true
        int originVisibility = isOriginActive ? View.VISIBLE : View.GONE;
        int destinationVisibility = isOriginActive ? View.GONE : View.VISIBLE;
        if (isOriginActive) {
            btnRequestTaxi.setText("Continuar sin destino");
        } else {
            btnRequestTaxi.setText("Continuar");
        }

        // Mostrar/Ocultar botones de origen y destino
        //btnGoToOrigin.setVisibility(destinationVisibility);
        //btnGoToDestination.setVisibility(originVisibility);

        // Mostrar/Ocultar secciones de búsqueda
        layoutSearchOrigin.setVisibility(originVisibility);
        layout_buttons_origin.setVisibility(originVisibility);
        recyclerViewSuggestionsOrigin.setVisibility(originVisibility);

        layoutSearchDestination.setVisibility(destinationVisibility);
        layout_buttons_destination.setVisibility(destinationVisibility);
        recyclerViewSuggestionsDestination.setVisibility(destinationVisibility);
    }*/

    /**
     * Precarga Google Places
     */
    private void preloadPlacesAPI() {
        new Handler().postDelayed(() -> {
            placesController.getPredictions("test", predictions -> {
                Log.d("Preload", "Pre-carga de Google Places completada");
            });
        }, 1000); // Espera 1 segundo después de abrir la actividad
    }

}