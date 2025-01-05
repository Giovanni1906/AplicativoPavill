package com.example.Pavill.view;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.Pavill.R;
import com.example.Pavill.components.FavoritesAdapter;
import com.example.Pavill.components.FriendlyAddressHelper;
import com.example.Pavill.components.MyClusterItem;
import com.example.Pavill.components.NavigationHeaderInfo;
import com.example.Pavill.components.TaxiClusterRenderer;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.MapController;
import com.example.Pavill.controller.NearbyTaxisController;
import com.example.Pavill.controller.PlacesController;
import com.example.Pavill.components.SuggestionsAdapter;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private EditText editTextOrigin, editTextDestination;
    private ImageButton btnActivateOrigin, btnActivateDestination;
    private LinearLayout layoutSearchOrigin, layoutSearchDestination;
    private RecyclerView recyclerViewSuggestionsOrigin, recyclerViewSuggestionsDestination;
    private SuggestionsAdapter suggestionsAdapterOrigin, suggestionsAdapterDestination;
    private PlacesController placesController;

    // Variables del mapa y navegación
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ClusterManager<MyClusterItem> clusterManager;

    private boolean isOriginActive = true; // Por defecto, el origen está activo
    private boolean isDestinationActive = false; // Por defecto, el origen está activo

    private int originTouchCount = 0;
    private int destinationTouchCount = 0;

    private String originAddress, destinationAddress;
    private String originPlaceId, destinationPlaceId;
    private LatLng originLatLng, destinationLatLng;

    private Marker originMarker; // Para almacenar el marcador del origen
    private Marker destinationMarker; // Para almacenar el marcador del destino
    private RouteController routeController;
    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;

    private boolean isUseMapForOrigin = false; // Variable para determinar si es origen o destino

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Inicializa la sección de favoritos antes de cualquier uso
        initializeFavoritesSection();

        routeController = new RouteController(this);

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar permisos de ubicación
        checkLocationPermission();

        // Inicializar el mapa
        initializeMap();

        // Inicializar Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.map_api_key));
        }

        placesController = new PlacesController(this);

        // Inicializar UI
        initializeUI();
        setupLocationButtons();

        setupLocationButtons(); // Configura los botones de origen y destino
        setupMapUseButtons(); // Configura los botones para usar el mapa como origen/destino

        // Inicializar BottomSheet y DrawerLayout
        initializeBottomSheetAndDrawer();
    }

    /**
     * Configura los botones para establecer la ubicación actual como origen o destino.
     */
    private void setupMapUseButtons() {
        Button btnUseMapForOrigin = findViewById(R.id.btnUseMapForOrigin);
        Button btnUseMapForDestination = findViewById(R.id.btnUseMapForDestination);
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

        // Configurar los botones "Usar el mapa"
        btnUseMapForOrigin.setOnClickListener(v -> handleUseMapForLocation(true));
        btnUseMapForDestination.setOnClickListener(v -> handleUseMapForLocation(false));

        // Configurar el botón de confirmación de ubicación
        btnConfirmLocation.setOnClickListener(v -> {
            LatLng centerLatLng = mMap.getCameraPosition().target;
            if (isUseMapForOrigin) {
                addMarkerToMap(centerLatLng, "Origen", getResources().getColor(R.color.primaryColor), true);
                editTextOrigin.setText(getAddressFromLatLng(centerLatLng));
                // Cambiar el color del icon_center_marker para el origen
            } else {
                addMarkerToMap(centerLatLng, "Destino", getResources().getColor(R.color.secondaryColor), false);
                editTextDestination.setText(getAddressFromLatLng(centerLatLng));
                // Cambiar el color del icon_center_marker para el destino
            }

            // Llamar a updateRoute después de establecer origen o destino
            updateRoute();

            if (iconCenterMarker instanceof ImageView) {
                int color = isUseMapForOrigin
                        ? ContextCompat.getColor(this, R.color.primaryColor)
                        : ContextCompat.getColor(this, R.color.secondaryColor);
                ((ImageView) iconCenterMarker).setColorFilter(color);
            } else {
                Log.e("MapActivity", "iconCenterMarker no es un ImageView.");
            }

            // Restaurar la interfaz
            btnConfirmLocation.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            iconShadow.setVisibility(View.GONE);
            iconCenterMarker.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Expandir el BottomSheet
            bottomSheetBehavior.setDraggable(true); // Desbloquear el BottomSheet
            menuButtonLayout.setVisibility(View.VISIBLE);
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
     * Maneja la acción de establecer la ubicación como origen o destino.
     */
    private void handleUseMapForLocation(boolean isForOrigin) {
        isUseMapForOrigin = isForOrigin;
        Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        Button btnBack = findViewById(R.id.btn_back);
        View iconShadow = findViewById(R.id.icon_shadow);
        View iconCenterMarker = findViewById(R.id.icon_center_marker);
        CardView menuButtonLayout = findViewById(R.id.btnOpenSidebar);

        // Bloquear el BottomSheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setDraggable(false);

        // Ocultar el botón del menú
        menuButtonLayout.setVisibility(View.GONE);

        // Mostrar los botones y vistas necesarias
        btnConfirmLocation.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        iconShadow.setVisibility(View.VISIBLE);
        iconCenterMarker.setVisibility(View.VISIBLE);
        btnConfirmLocation.setText(isForOrigin ? "Marcar como Origen" : "Marcar como Destino");
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
     * Obtiene las direcciones favoritas del cliente
     */
    private void fetchAndDisplayFavorites() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");

        new MapController().fetchFavoriteDestinations(this, clienteId, new MapController.FavoriteDestinationsCallback() {
            @Override
            public void onFavoritesReceived(List<MapController.FavoriteDestination> favorites) {
                runOnUiThread(() -> {
                    if (favorites != null && !favorites.isEmpty()) {
                        // Limitar la lista a los primeros 2 favoritos
                        List<MapController.FavoriteDestination> limitedFavorites = favorites.size() > 2
                                ? favorites.subList(0, 2)
                                : favorites;

                        findViewById(R.id.layoutFavorites).setVisibility(View.VISIBLE); // Mostrar sección
                        favoritesAdapter.updateFavorites(limitedFavorites);
                    } else {
                        findViewById(R.id.layoutFavorites).setVisibility(View.GONE); // Ocultar sección
                    }
                });
            }

            @Override
            public void onNoFavoritesFound(String message) {
                runOnUiThread(() -> findViewById(R.id.layoutFavorites).setVisibility(View.GONE));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al obtener favoritos: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }



    /**
     * Inicializa la sección de favoritos
     */
    private void initializeFavoritesSection() {
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);

        // Configurar el adaptador
        favoritesAdapter = new FavoritesAdapter(new ArrayList<>(), favorite -> {
            LatLng favoriteLatLng = new LatLng(favorite.getLatitude(), favorite.getLongitude());
            destinationLatLng = favoriteLatLng; // Asignar como destino
            addMarkerToMap(favoriteLatLng, "Destino", getResources().getColor(R.color.secondaryColor), false);

            editTextDestination.setText(favorite.getAddress()); // Actualizar el input
        });

        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFavorites.setAdapter(favoritesAdapter);
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
                    TemporaryData.getInstance().setDistance(distanceText);
                    TemporaryData.getInstance().setDuration(durationText);
                    TemporaryData.getInstance().setEstimatedCost(String.format(Locale.getDefault(), "%.2f soles", estimatedCost));
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
     * Configura los botones para establecer la ubicación actual como origen o destino.
     */
    private void setupLocationButtons() {
        Button btnSetOriginToCurrentLocation = findViewById(R.id.btnSetOriginToCurrentLocation);
        Button btnSetDestinationToCurrentLocation = findViewById(R.id.btnSetDestinationToCurrentLocation);

        btnSetOriginToCurrentLocation.setOnClickListener(v -> setLocationAsOrigin());
        btnSetDestinationToCurrentLocation.setOnClickListener(v -> setLocationAsDestination());
    }

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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar estilo del mapa
        applyMapStyle();

        // Verificar permisos de ubicación
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);

            // Configurar ClusterManager
            setupClusterManager();

            // Obtener y manejar taxis cercanos
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    fetchNearbyTaxis(currentLocation);
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
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

        // Configurar renderer personalizado
        BitmapDescriptor taxiIcon = getResizedBitmap(R.drawable.ic_car, convertDpToPx(30), convertDpToPx(40));
        TaxiClusterRenderer renderer = new TaxiClusterRenderer(this, mMap, clusterManager, taxiIcon);
        clusterManager.setRenderer(renderer);
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

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    /**
     * Redimensiona el ícono para los marcadores de taxis.
     */
    private BitmapDescriptor getResizedBitmap(int drawableRes, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }


    /**
     * Verifica si el permiso de ubicación está otorgado
     * @return
     */
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    /**
     * Verifica si el GPS está activado
     */
    private void checkIfLocationIsEnabled() {
        // Comprobar si el GPS está activado
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            // Mostrar un cuadro de diálogo para activar la ubicación
            Toast.makeText(this, "La ubicación está desactivada. Por favor actívela.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            getCurrentLocationAndCenterMap();
        }
    }

    /**
     * Obtiene la ubicación actual y centra el mapa.
     */
    private void getCurrentLocationAndCenterMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); // Centrar el mapa
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
            }
        });
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
                checkIfLocationIsEnabled();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Inicializa la interfaz de usuario
     */
    private void initializeUI() {
        editTextOrigin = findViewById(R.id.editTextOrigin);
        editTextDestination = findViewById(R.id.editTextDestination);

        CardView btnMyLocation = findViewById(R.id.btn_my_location);

        ImageButton btnDeleteOrigin = findViewById(R.id.btnDeleteOrigin);
        ImageButton btnDeleteDestination = findViewById(R.id.btnDeleteDestination);

        btnActivateOrigin = findViewById(R.id.btnActivateOrigin);
        btnActivateDestination = findViewById(R.id.btnActivateDestination);

        layoutSearchOrigin = findViewById(R.id.layoutSearchOrigin);
        layoutSearchDestination = findViewById(R.id.layoutSearchDestination);

        recyclerViewSuggestionsOrigin = findViewById(R.id.recyclerViewSuggestionsOrigin);
        recyclerViewSuggestionsDestination = findViewById(R.id.recyclerViewSuggestionsDestination);

        // Inicializar adaptadores de sugerencias
        initializeSuggestionsAdapterOrigin();
        initializeSuggestionsAdapterDestination();

        // Configurar listeners para los inputs
        setupTextWatchers();

        // Configurar listeners para los inputs y botones
        setupInputListeners();

        // Establecer el estado inicial
        activateInput(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true); // Por defecto, el origen está activado

        // botones para eliminar inputs de origen y destino
        DeleteInputsText(btnDeleteOrigin, btnDeleteDestination);

        // Botón para ir a la ubicación actual
        GotoMyLocation(btnMyLocation);
    }

    private void GotoMyLocation(CardView btnMyLocation) {
        btnMyLocation.setOnClickListener(v -> {
            if (mMap != null && checkLocationPermission()) {
                getCurrentLocationAndCenterMap(); // Centrar el mapa en la ubicación actual
            } else {
                Toast.makeText(this, "Permisos de ubicación no otorgados.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Inicializa el adaptador y RecyclerView para las sugerencias de origen.
     */
    private void initializeSuggestionsAdapterOrigin() {
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
            recyclerViewSuggestionsOrigin.setVisibility(View.GONE);

            Log.d("OriginSelection", "Dirección de origen seleccionada: " + originAddress);
        });

        recyclerViewSuggestionsOrigin.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsOrigin.setAdapter(suggestionsAdapterOrigin);

        // Agregar divisor al RecyclerView
        recyclerViewSuggestionsOrigin.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    /**
     * Inicializa el adaptador y RecyclerView para las sugerencias de destino.
     */
    private void initializeSuggestionsAdapterDestination() {
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
            recyclerViewSuggestionsDestination.setVisibility(View.GONE);

            Log.d("DestinationSelection", "Dirección de destino seleccionada: " + destinationAddress);
        });

        recyclerViewSuggestionsDestination.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsDestination.setAdapter(suggestionsAdapterDestination);

        // Agregar divisor al RecyclerView
        recyclerViewSuggestionsDestination.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }


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
        android.graphics.Color.colorToHSV(color, hsv);

        // Eliminar marcador anterior si es necesario
        if (isOrigin) {
            if (originMarker != null) {
                originMarker.remove();
            }
            originMarker = mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0])));
        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            destinationMarker = mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0])));
        }

        // Llamar a updateRoute cuando ambos marcadores estén definidos
        if (originMarker != null && destinationMarker != null) {
            updateRoute();
        }


        // Enfocar el mapa en el marcador con animación
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
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
            Log.d("DeleteInput", "Texto de origen eliminado");
        });

        btnDeleteDestination.setOnClickListener(v -> {
            editTextDestination.setText(""); // Limpia el texto del destino
            Log.d("DeleteInput", "Texto de destino eliminado");
        });
    }

    /**
     * Configura los listeners para los inputs y botones
     */
    private void setupInputListeners() {
        // Configurar el layout y el input de origen
        layoutSearchOrigin.setOnClickListener(v -> handleActivation(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true));
        btnActivateOrigin.setOnClickListener(v -> handleActivation(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true));
        editTextOrigin.setOnTouchListener((v, event) -> {
            return handleEditTextTouch(event, editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true, originTouchCount++);
        });

        // Configurar acción del teclado para "Siguiente" en el origen
        editTextOrigin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                handleActivation(editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false);
                return true; // Consumir la acción
            }
            return false;
        });

        // Configurar el layout y el input de destino
        layoutSearchDestination.setOnClickListener(v -> handleActivation(editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false));
        btnActivateDestination.setOnClickListener(v -> handleActivation(editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false));
        editTextDestination.setOnTouchListener((v, event) -> {
            return handleEditTextTouch(event, editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false, destinationTouchCount++);
        });

        // Configurar acción del teclado para "Done" o finalizar en el destino
        editTextDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Si necesario, agregar lógica adicional para "finalizar"
                closeKeyboard();
                return true; // Consumir la acción
            }
            return false;
        });
    }

    /**
     * Maneja el toque en el input
     * @param event
     * @param editTextToActivate
     * @param buttonToActivate
     * @param buttonToDeactivate
     * @param layoutToActivate
     * @param layoutToDeactivate
     * @param isActivatingOrigin
     * @param touchCount
     * @return
     */
    private boolean handleEditTextTouch(MotionEvent event, EditText editTextToActivate, ImageButton buttonToActivate, ImageButton buttonToDeactivate,
                                        LinearLayout layoutToActivate, LinearLayout layoutToDeactivate, boolean isActivatingOrigin, int touchCount) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (touchCount == 0) {
                // Primera vez que se toca el input: Activar pero no abrir el teclado
                handleActivation(editTextToActivate, buttonToActivate, buttonToDeactivate, layoutToActivate, layoutToDeactivate, isActivatingOrigin);
                closeKeyboard();
                return true; // Prevenir que el teclado se abra
            } else {
                // A partir del segundo toque, dejar que el teclado se abra normalmente
                handleActivation(editTextToActivate, buttonToActivate, buttonToDeactivate, layoutToActivate, layoutToDeactivate, isActivatingOrigin);
                return false; // Permitir el comportamiento normal (teclado)
            }
        }
        return false;
    }

    /**
     * maneja las sugerencias
     * @param targetEditText
     * @param targetImageButton
     * @param otherImageButton
     * @param targetLayout
     * @param otherLayout
     * @param isOrigin
     */
    private void handleActivation(
            EditText targetEditText,
            ImageButton targetImageButton,
            ImageButton otherImageButton,
            LinearLayout targetLayout,
            LinearLayout otherLayout,
            boolean isOrigin
    ) {
        if (isOrigin) {
            isOriginActive = true;
            isDestinationActive = false;
            updateUIForActiveInput();
            recyclerViewSuggestionsDestination.setVisibility(View.GONE); // Ocultar sugerencias de destino
            // Ocultar favoritos si origen está activado
            findViewById(R.id.layoutFavorites).setVisibility(View.GONE);
        } else {
            isDestinationActive = true;
            isOriginActive = false;
            updateUIForActiveInput();
            fetchAndDisplayFavorites(); // Cargar y mostrar favoritos

            recyclerViewSuggestionsOrigin.setVisibility(View.GONE); // Ocultar sugerencias de origen
            // Mostrar favoritos si destino está activado
            if (favoritesAdapter.getItemCount() > 0) {
                findViewById(R.id.layoutFavorites).setVisibility(View.VISIBLE);
            }
        }

        // Cambiar los drawables para los botones
        targetImageButton.setImageResource(R.drawable.ic_my_location_active);
        otherImageButton.setImageResource(R.drawable.ic_my_location);

        // Registrar cambios de estado en el log
        Log.d("Activation", isOrigin ? "Origen activado" : "Destino activado");

        // Cerrar el teclado al cambiar activación
        closeKeyboard();
    }

    /**
     * Activa o desactiva el input
     */
    private void activateInput(EditText editTextToActivate, ImageButton buttonToActivate, ImageButton buttonToDeactivate,
                               LinearLayout layoutToActivate, LinearLayout layoutToDeactivate, boolean isActivatingOrigin) {
        // Cambiar el estado de activación
        isOriginActive = isActivatingOrigin;
        isDestinationActive = !isActivatingOrigin;

        // Cambiar el estado visual de los botones
        buttonToActivate.setImageResource(R.drawable.ic_my_location_active);
        buttonToDeactivate.setImageResource(R.drawable.ic_my_location);

        // Registrar logs
        if (isActivatingOrigin) {
            Log.d("InputActivation", "Input de origen activado.");
            Log.d("InputActivation", "Input de destino desactivado.");
        } else {
            Log.d("InputActivation", "Input de destino activado.");
            Log.d("InputActivation", "Input de origen desactivado.");
        }
    }

    /**
     * Cierra el teclado
     */
    private void closeKeyboard() {
        // Cerrar el teclado
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

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
                if (isOriginActive && s.length() >= 3) {
                    placesController.getPredictions(s.toString(), predictions -> {
                        suggestionsAdapterOrigin.updateSuggestions(predictions);
                        recyclerViewSuggestionsOrigin.setVisibility(View.VISIBLE);
                        recyclerViewSuggestionsDestination.setVisibility(View.GONE); // Ocultar sugerencias de destino
                    });
                } else {
                    recyclerViewSuggestionsOrigin.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // TextWatcher para el input de destino
        editTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isDestinationActive && s.length() >= 3) {
                    placesController.getPredictions(s.toString(), predictions -> {
                        suggestionsAdapterDestination.updateSuggestions(predictions);
                        recyclerViewSuggestionsDestination.setVisibility(View.VISIBLE);
                        recyclerViewSuggestionsOrigin.setVisibility(View.GONE); // Ocultar sugerencias de origen
                    });
                } else {
                    recyclerViewSuggestionsDestination.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

    private void updateUIForActiveInput() {
        // Mostrar/ocultar layout_buttons_origin y layout_buttons_destination
        LinearLayout layoutButtonsOrigin = findViewById(R.id.layout_buttons_origin);
        LinearLayout layoutButtonsDestination = findViewById(R.id.layout_buttons_destination);

        // Mostrar/ocultar btnDeleteOrigin y btnDeleteDestination
        ImageButton btnDeleteOrigin = findViewById(R.id.btnDeleteOrigin);
        ImageButton btnDeleteDestination = findViewById(R.id.btnDeleteDestination);

        if (isOriginActive) {
            layoutButtonsOrigin.setVisibility(View.VISIBLE);
            layoutButtonsDestination.setVisibility(View.GONE);

            btnDeleteOrigin.setVisibility(View.VISIBLE);
            btnDeleteDestination.setVisibility(View.GONE);
        } else if (isDestinationActive) {
            layoutButtonsOrigin.setVisibility(View.GONE);
            layoutButtonsDestination.setVisibility(View.VISIBLE);

            btnDeleteOrigin.setVisibility(View.GONE);
            btnDeleteDestination.setVisibility(View.VISIBLE);
        }
    }

}