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
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.Pavill.R;
import com.example.Pavill.components.MyClusterItem;
import com.example.Pavill.components.NavigationHeaderInfo;
import com.example.Pavill.controller.PlacesController;
import com.example.Pavill.components.SuggestionsAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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

        // Inicializar BottomSheet y DrawerLayout
        initializeBottomSheetAndDrawer();
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

        // Verificar si la ubicación está habilitada y centrar en la ubicación actual
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocationAndCenterMap();
        } else {
            Toast.makeText(this, "Permisos de ubicación no otorgados.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Verifica si el permiso de ubicación está otorgado y si el GPS está activado
     *
     * @return
     */
    private boolean checkLocationPermission() {
        // Verificar si el permiso ya está otorgado
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Solicitar el permiso
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Si el permiso está otorgado, verificar si el GPS está activado
            checkIfLocationIsEnabled();
        }
        return false;
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
     * Obtiene la ubicación actual y la centra en el mapa
     */
    private void getCurrentLocationAndCenterMap() {
        // Obtener la última ubicación conocida
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
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
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

        btnActivateOrigin = findViewById(R.id.btnActivateOrigin);
        btnActivateDestination = findViewById(R.id.btnActivateDestination);

        layoutSearchOrigin = findViewById(R.id.layoutSearchOrigin);
        layoutSearchDestination = findViewById(R.id.layoutSearchDestination);

        recyclerViewSuggestionsOrigin = findViewById(R.id.recyclerViewSuggestionsOrigin);
        recyclerViewSuggestionsDestination = findViewById(R.id.recyclerViewSuggestionsDestination);

        // Configurar RecyclerViews
        suggestionsAdapterOrigin = new SuggestionsAdapter(new ArrayList<>(), suggestion -> {
            editTextOrigin.setText(suggestion.getPrimaryText(null));
            recyclerViewSuggestionsOrigin.setVisibility(RecyclerView.GONE);
        });

        recyclerViewSuggestionsOrigin.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsOrigin.setAdapter(suggestionsAdapterOrigin);

        // Agregar divisor al RecyclerView de Origen
        recyclerViewSuggestionsOrigin.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        suggestionsAdapterDestination = new SuggestionsAdapter(new ArrayList<>(), suggestion -> {
            editTextDestination.setText(suggestion.getPrimaryText(null));
            recyclerViewSuggestionsDestination.setVisibility(RecyclerView.GONE);
        });

        recyclerViewSuggestionsDestination.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsDestination.setAdapter(suggestionsAdapterDestination);

        // Agregar divisor al RecyclerView de Destino
        recyclerViewSuggestionsDestination.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Configurar listeners para los inputs
        setupTextWatchers();

        // Configurar listeners para los inputs y botones
        setupInputListeners();

        // Establecer el estado inicial
        activateInput(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true); // Por defecto, el origen está activado
    }

    private void setupInputListeners() {
        // Configurar el layout y el input de origen
        layoutSearchOrigin.setOnClickListener(v -> handleActivation(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true));
        btnActivateOrigin.setOnClickListener(v -> handleActivation(editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true));
        editTextOrigin.setOnTouchListener((v, event) -> {
            return handleEditTextTouch(event, editTextOrigin, btnActivateOrigin, btnActivateDestination, layoutSearchOrigin, layoutSearchDestination, true, originTouchCount++);
        });

        // Configurar el layout y el input de destino
        layoutSearchDestination.setOnClickListener(v -> handleActivation(editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false));
        btnActivateDestination.setOnClickListener(v -> handleActivation(editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false));
        editTextDestination.setOnTouchListener((v, event) -> {
            return handleEditTextTouch(event, editTextDestination, btnActivateDestination, btnActivateOrigin, layoutSearchDestination, layoutSearchOrigin, false, destinationTouchCount++);
        });
    }

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

    private void handleActivation(EditText editTextToActivate, ImageButton buttonToActivate, ImageButton buttonToDeactivate,
                                  LinearLayout layoutToActivate, LinearLayout layoutToDeactivate, boolean isActivatingOrigin) {
        // Activar el input correspondiente
        if ((isActivatingOrigin && !isOriginActive) || (!isActivatingOrigin && !isDestinationActive)) {
            activateInput(editTextToActivate, buttonToActivate, buttonToDeactivate, layoutToActivate, layoutToDeactivate, isActivatingOrigin);
            closeKeyboard();
        }
    }

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
        editTextOrigin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    placesController.getPredictions(s.toString(), predictions -> {
                        // Limitar a las 2 primeras predicciones
                        List<AutocompletePrediction> limitedPredictions = predictions.size() > 2
                                ? predictions.subList(0, 2)
                                : predictions;

                        suggestionsAdapterOrigin.updateSuggestions(predictions);
                        recyclerViewSuggestionsOrigin.setVisibility(RecyclerView.VISIBLE);
                    });
                } else {
                    recyclerViewSuggestionsOrigin.setVisibility(RecyclerView.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    placesController.getPredictions(s.toString(), predictions -> {
                        // Limitar a las 2 primeras predicciones
                        List<AutocompletePrediction> limitedPredictions = predictions.size() > 2
                                ? predictions.subList(0, 2)
                                : predictions;

                        suggestionsAdapterDestination.updateSuggestions(predictions);
                        recyclerViewSuggestionsDestination.setVisibility(RecyclerView.VISIBLE);
                    });
                } else {
                    recyclerViewSuggestionsDestination.setVisibility(RecyclerView.GONE);
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
}
