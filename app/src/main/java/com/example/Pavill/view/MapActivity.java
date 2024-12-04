package com.example.Pavill.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.Pavill.R;
import com.example.Pavill.components.TaxiClusterRenderer;
import com.example.Pavill.controller.MapController;
import com.example.Pavill.controller.NearbyTaxisController;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.clustering.ClusterManager;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.location.Location;
import android.Manifest;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.example.Pavill.components.PlacesAutoCompleteAdapter;  // Asegúrate de que este sea el paquete correcto.
import com.example.Pavill.components.MyClusterItem;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    //controlador
    private MapController mapController;
    // Variables del mapa y navegación
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private GoogleMap mMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ClusterManager<MyClusterItem> clusterManager;

    // Marcadores de origen y destino
    private Marker originMarker;
    private Marker destinationMarker;
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private PlacesClient placesClient;
    private EditText editTextOrigin, editTextDestination;
    private RecyclerView recyclerViewSuggestionsOrigin, recyclerViewSuggestionsDestination;

    private Polyline routePolyline; // Para almacenar y manipular la ruta


    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeUIComponents();

        // Inicializar el botón de ubicación y agregar el listener
        CardView btnMyLocation = findViewById(R.id.btn_my_location);
        btnMyLocation.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    } else {
                        Toast.makeText(MapActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Inicializar el MapController
        mapController = new MapController();

        // Configurar los listeners para los inputs de origen y destino
        setupInputFocusListener(editTextOrigin, R.id.btnActivateLocation);
        setupInputFocusListener(editTextDestination, R.id.btnActivateDestination);

        // Limpiar el foco de los inputs al iniciar
        editTextOrigin.clearFocus();
        editTextDestination.clearFocus();

        // Configurar los listeners de los botones para marcar origen y destino en la ubicación actual
        setupLocationButton(R.id.btnSetOriginToCurrentLocation, true);
        setupLocationButton(R.id.btnSetDestinationToCurrentLocation, false);
    }

    /**
     * Configura los listeners para los inputs de origen y destino.
     */
    private void setupInputFocusListener(EditText editText, int buttonId) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Expandir el BottomSheet completamente cuando el input tenga el foco
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                // Activar el botón de origen o destino cuando el campo de texto recibe el foco
                findViewById(buttonId).performClick();
            }
        });
    }

    /**
     * Configura los listeners para los botones que marcan la ubicación actual.
     */
    private void setupLocationButton(int buttonId, boolean isForOrigin) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            setLocationToCurrent(isForOrigin);

            // Si se marca el origen, activar automáticamente el destino
            if (isForOrigin) {
                activateDestination();
            }
        });
    }

    private void setLocationToCurrent(boolean isForOrigin) {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        // Marcar la ubicación actual como origen o destino según el botón presionado
                        placeMarkerAndShowAddress(currentLocation, isForOrigin);
                        // Opcionalmente, mover la cámara a la ubicación actual
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    } else {
                        Toast.makeText(MapActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void initializeUIComponents() {
        // Referencias a los EditText y RecyclerView
        editTextOrigin = findViewById(R.id.editTextOrigin);
        editTextDestination = findViewById(R.id.editTextDestination);
        recyclerViewSuggestionsOrigin = findViewById(R.id.recyclerViewSuggestionsOrigin);
        recyclerViewSuggestionsDestination = findViewById(R.id.recyclerViewSuggestionsDestination);
        // Accede al TextView
        TextView textViewName = findViewById(R.id.textViewName);

        // Accede a SharedPreferences para obtener el nombre del cliente
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteNombre = sharedPreferences.getString("ClienteNombre", ""); // "Cliente" es el valor por defecto

        // Establece el texto en el TextView
        textViewName.setText("Hola, " + clienteNombre);

        // Configurar LayoutManager para los RecyclerView
        recyclerViewSuggestionsOrigin.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsDestination.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar el FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar botones y otras configuraciones
        initializeButtons();
        initializeBottomSheetAndDrawer();

        // Configurar el BottomSheet para que esté completamente expandido al iniciar
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Agregar TextWatcher para el autocompletado
        setupAutoComplete(editTextOrigin, recyclerViewSuggestionsOrigin, true);  // Para origen
        setupAutoComplete(editTextDestination, recyclerViewSuggestionsDestination, false);  // Para destino

        // Inicializar el cliente de Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), String.valueOf(R.string.map_api_key));
        }
        placesClient = Places.createClient(this);  // Asegúrate de asignar placesClient aquí correctamente

        // Verificar si los permisos de ubicación están otorgados, si no, solicitarlos
        if (checkLocationPermission()) {
            // Si ya tiene los permisos, continuar con la inicialización del mapa
            initializeMapAndLocationServices();
        }

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initializeButtons() {
        final ImageButton btnActivateOrigin = findViewById(R.id.btnActivateLocation);
        final ImageButton btnActivateDestination = findViewById(R.id.btnActivateDestination);
        final LinearLayout layoutButtonsOrigin = findViewById(R.id.layout_buttons_origin);
        final LinearLayout layoutButtonsDestination = findViewById(R.id.layout_buttons_destination);
        final Button btnUseMapForOrigin = findViewById(R.id.btnUseMapForOrigin);
        final Button btnUseMapForDestination = findViewById(R.id.btnUseMapForDestination);
        final ImageView iconCenterMarker = findViewById(R.id.icon_center_marker);
        final ImageView waveView = findViewById(R.id.icon_shadow);
        final Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        final Button btnBack = findViewById(R.id.btn_back);
        final CardView btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        final Button btnRequestTaxi = findViewById(R.id.btnRequestTaxi);


        // Inicialmente, origen y destino ocultos
        layoutButtonsOrigin.setVisibility(View.GONE);
        layoutButtonsDestination.setVisibility(View.GONE);

        // Configurar los listeners de los botones para activar origen/destino
        View.OnClickListener toggleButtonLayouts = v -> {
            boolean isOriginButton = (v == btnActivateOrigin);
            toggleSelection(isOriginButton, btnActivateOrigin, btnActivateDestination, layoutButtonsOrigin, layoutButtonsDestination);
        };

        //para mostrar los botones de los inputs
        btnActivateOrigin.setOnClickListener(toggleButtonLayouts);
        btnActivateDestination.setOnClickListener(toggleButtonLayouts);

        btnRequestTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (originCoordinates != null && destinationCoordinates != null) {
                    Intent intent = new Intent(MapActivity.this, ConfirmActivity.class);
                    // Pasar las coordenadas al ConfirmActivity
                    intent.putExtra("origin_lat", originCoordinates.latitude);
                    intent.putExtra("origin_lng", originCoordinates.longitude);
                    intent.putExtra("destination_lat", destinationCoordinates.latitude);
                    intent.putExtra("destination_lng", destinationCoordinates.longitude);
                    startActivity(intent);
                } else {
                    Toast.makeText(MapActivity.this, "Por favor, seleccione una ubicación de origen y destino.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Listener para "Usar el mapa" para origen y destino
        btnUseMapForOrigin.setOnClickListener(v -> handleMapUseButton(true));
        btnUseMapForDestination.setOnClickListener(v -> handleMapUseButton(false));

        // Listener para el botón de "Volver"
        btnBack.setOnClickListener(v -> handleBackButton(waveView, btnOpenSidebar, iconCenterMarker, btnConfirmLocation, btnBack));
    }

    /**
     * Alterna la selección entre origen y destino.
     */
    private void toggleSelection(boolean isOriginButton, ImageButton btnActivateOrigin, ImageButton btnActivateDestination,
                                 LinearLayout layoutButtonsOrigin, LinearLayout layoutButtonsDestination) {
        // Llama al método del controlador para alternar la selección
        mapController.toggleSelection(isOriginButton);

        // Verificar si la selección (origen o destino) está activa después de alternar
        if (mapController.isActiveSelection(isOriginButton)) {
            // Si la selección está activa, activar visualmente el botón correspondiente
            activateButtonSelection(
                    isOriginButton ? btnActivateOrigin : btnActivateDestination, // Si es origen, activa el botón de origen; si es destino, activa el de destino
                    isOriginButton ? btnActivateDestination : btnActivateOrigin, // Si es origen, desactiva el botón de destino; y viceversa
                    isOriginButton // Pasar si es para origen o no
            );

            if (isOriginButton) {
                // Si el botón clicado es el de origen:
                // Mostrar el layout de botones adicionales del origen y ocultar el del destino
                layoutButtonsOrigin.setVisibility(View.VISIBLE);
                layoutButtonsDestination.setVisibility(View.GONE);
            } else {
                // Si el botón clicado es el de destino:
                // Mostrar el layout de botones adicionales del destino y ocultar el del origen
                layoutButtonsDestination.setVisibility(View.VISIBLE);
                layoutButtonsOrigin.setVisibility(View.GONE);
            }
        } else {
            // Si la selección no está activa, desactivar visualmente el botón correspondiente
            if (isOriginButton) {
                // Si es el botón de origen:
                // Desactivar la selección visual del botón de origen y ocultar su layout de botones adicionales
                desactivateButtonSelection(btnActivateOrigin);
                layoutButtonsOrigin.setVisibility(View.GONE);
            } else {
                // Si es el botón de destino:
                // Desactivar la selección visual del botón de destino y ocultar su layout de botones adicionales
                desactivateButtonSelection(btnActivateDestination);
                layoutButtonsDestination.setVisibility(View.GONE);
            }
        }
    }


    /**
     * Maneja la acción al presionar el botón "Usar el mapa" para origen o destino.
     */
    private void handleMapUseButton(boolean isForOrigin) {
        // Colapsar el BottomSheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setDraggable(false); // Desactivar el arrastre

        // Mostrar el icono central en el mapa
        ImageView iconCenterMarker = findViewById(R.id.icon_center_marker);
        iconCenterMarker.setVisibility(View.VISIBLE);

        // Cambiar el color del icono central según el uso (origen o destino)
        int color = isForOrigin ? getResources().getColor(R.color.primaryColor) : getResources().getColor(R.color.secondaryColor);
        iconCenterMarker.setColorFilter(color);

        // Añadir una animación de onda debajo del ícono central
        View waveView = findViewById(R.id.icon_shadow);
        waveView.setVisibility(View.VISIBLE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(waveView, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(waveView, "scaleY", 1f, 1.5f);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();

        // Mostrar el botón de confirmación y el botón de volver
        Button btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        Button btnBack = findViewById(R.id.btn_back);
        btnConfirmLocation.setVisibility(View.VISIBLE);
        btnConfirmLocation.setText(isForOrigin ? "Marcar como origen" : "Marcar como destino");

        // Cambiar el color del botón según sea origen o destino
        int buttonColor = isForOrigin ? R.color.primaryColor : R.color.secondaryColor;
        btnConfirmLocation.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonColor));

        btnBack.setVisibility(View.VISIBLE);

        // Ocultar el menú lateral
        CardView btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        btnOpenSidebar.setVisibility(View.GONE);

        // Configurar la acción del botón de confirmación
        btnConfirmLocation.setOnClickListener(v -> {
            LatLng centerLatLng = mMap.getCameraPosition().target;
            placeMarkerAndShowAddress(centerLatLng, isForOrigin); // Marcar la ubicación seleccionada

            // Ocultar el icono y los botones de confirmación y volver
            iconCenterMarker.setVisibility(View.GONE);
            waveView.setVisibility(View.GONE);
            btnConfirmLocation.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);

            // Expandir el BottomSheet y reactivar el arrastre
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetBehavior.setDraggable(true);

            // Mostrar el menú lateral
            btnOpenSidebar.setVisibility(View.VISIBLE);

            // Si se marcó el origen, activar automáticamente el destino
            if (isForOrigin) {
                activateDestination();
            }

        });
    }

    /**
     * Maneja la acción del botón de "Volver".
     */
    private void handleBackButton(ImageView waveView ,CardView btnOpenSidebar, ImageView iconCenterMarker, Button btnConfirmLocation, Button btnBack) {
        // Expandir el BottomSheet
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Ocultar el icono central, el botón de confirmación y el botón de volver
        iconCenterMarker.setVisibility(View.GONE);
        waveView.setVisibility(View.GONE);
        btnConfirmLocation.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);

        // Activar el menú lateral
        btnOpenSidebar.setVisibility(View.VISIBLE);
    }

    // Inicializa el BottomSheet y el DrawerLayout
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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(100);

        setupNavigationView();
    }

    // Manejo de navegación
    private void setupNavigationView() {
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

    // Método que se ejecuta cuando el mapa está listo
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Inicializar el ClusterManager
            if (clusterManager == null) {
                clusterManager = new ClusterManager<>(this, mMap);
                mMap.setOnCameraIdleListener(clusterManager);
                mMap.setOnMarkerClickListener(clusterManager);

                // Configurar un renderer personalizado si es necesario
                int widthInPx = convertDpToPx(30); // Tamaño deseado
                int heightInPx = convertDpToPx(40);
                BitmapDescriptor taxiIcon = getResizedBitmap(R.drawable.ic_car, widthInPx, heightInPx);
                TaxiClusterRenderer renderer = new TaxiClusterRenderer(this, mMap, clusterManager, taxiIcon);
                clusterManager.setRenderer(renderer);
            }

            // Obtener la última ubicación conocida
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                    // Llamar al controlador para obtener taxis cercanos
                    fetchNearbyTaxis(currentLocation);
                } else {
                    Toast.makeText(MapActivity.this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Permisos de ubicación no otorgados.", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para redimensionar el ícono
    private BitmapDescriptor getResizedBitmap(int drawableRes, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void activateButtonSelection(ImageButton activeButton, ImageButton inactiveButton, boolean isForOrigin) {
        // Actualizar los íconos en la UI
        activeButton.setImageResource(isForOrigin ? R.drawable.ic_my_location_active : R.drawable.ic_my_location_active);
        inactiveButton.setImageResource(isForOrigin ? R.drawable.ic_my_location : R.drawable.ic_my_location);
    }

    private void desactivateButtonSelection(ImageButton button) {
        // Actualizar ícono en la UI
        button.setImageResource(R.drawable.ic_my_location);
    }
        // Coloca el marcador y muestra la dirección
    private void placeMarkerAndShowAddress(LatLng latLng, boolean isForOrigin) {
        if (isForOrigin) {
            if (originMarker != null) {
                // Si el marcador ya existe, simplemente actualiza su posición
                originMarker.setPosition(latLng);
            } else {
                // Si no existe, crea un nuevo marcador
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("Ubicación de origen")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                originMarker = mMap.addMarker(markerOptions);
            }
            originCoordinates = latLng;
        } else {
            if (destinationMarker != null) {
                // Si el marcador ya existe, simplemente actualiza su posición
                destinationMarker.setPosition(latLng);
            } else {
                // Si no existe, crea un nuevo marcador
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("Ubicación de destino")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                destinationMarker = mMap.addMarker(markerOptions);
            }
            destinationCoordinates = latLng;

            // Llamar a drawRoute para trazar la ruta entre origen y destino
            drawRoute(originCoordinates, destinationCoordinates);
        }

        // Actualizar la ruta siempre que se cambie origen o destino
        updateRoute();

        // Mostrar la dirección en el campo correspondiente
        getAddressFromCoordinates(latLng, isForOrigin);
    }

    // Obtiene la dirección de las coordenadas y la coloca en el campo correcto
    private void getAddressFromCoordinates(LatLng latLng, boolean isForOrigin) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String addressText = addresses.get(0).getAddressLine(0);
                if (isForOrigin) {
                    EditText editTextOrigin = findViewById(R.id.editTextOrigin);
                    editTextOrigin.setText(addressText);
                    originCoordinates = latLng;
                } else {
                    EditText editTextDestination = findViewById(R.id.editTextDestination);
                    editTextDestination.setText(addressText);
                    destinationCoordinates = latLng;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeMapAndLocationServices() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario concedió los permisos, inicializar el mapa y los servicios de ubicación
                initializeMapAndLocationServices();
            } else {
                // Si el usuario no concedió los permisos, muestra un mensaje
                Toast.makeText(this, "Permiso de ubicación es necesario para mostrar el mapa.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupAutoComplete(final EditText editText, final RecyclerView recyclerView, final boolean isForOrigin) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {  // Iniciar búsqueda tras 3 caracteres
                    AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

                    // LatLngBounds para limitar las sugerencias a Tacna
                    LatLngBounds tacnaBounds = new LatLngBounds(
                            new LatLng(-18.0400, -70.2900),  // Suroeste
                            new LatLng(-17.9800, -70.2000)   // Noreste
                    );

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(s.toString())
                            .setSessionToken(token)
                            .setLocationBias(RectangularBounds.newInstance(tacnaBounds))  // Limitar a Tacna
                            .build();

                    placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
                        // Adaptador para las sugerencias
                        List<AutocompletePrediction> predictionList = response.getAutocompletePredictions();
                        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(predictionList, new PlacesAutoCompleteAdapter.OnPlaceClickListener() {
                            @Override
                            public void onPlaceClick(AutocompletePrediction prediction) {
                                // Cuando el usuario selecciona una sugerencia
                                String placeId = prediction.getPlaceId();
                                fetchPlaceDetails(placeId, isForOrigin);
                            }
                        });

                        recyclerView.setAdapter(adapter);
                        recyclerView.setVisibility(View.VISIBLE);  // Mostrar las sugerencias
                    }).addOnFailureListener((exception) -> {
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            Log.e("Places", "Error: " + apiException.getStatusCode());
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchPlaceDetails(String placeId, final boolean isForOrigin) {
        // Definir los campos de lugar que queremos obtener
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            LatLng latLng = place.getLatLng();

            if (latLng != null) {
                // Colocar el marcador en la ubicación seleccionada
                placeMarkerAndShowAddress(latLng, isForOrigin);

                // Mover la cámara hacia la ubicación seleccionada
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e("Places", "Error al obtener detalles del lugar: " + apiException.getStatusCode());
            }
        });
    }

    private void fetchNearbyTaxis(LatLng currentLocation) {
        new NearbyTaxisController().fetchNearbyTaxis(this, currentLocation, new NearbyTaxisController.NearbyTaxisCallback() {
            @Override
            public void onTaxisReceived(List<MyClusterItem> taxis) {
                if (clusterManager != null) {
                    clusterManager.clearItems(); // Limpiar taxis previos
                    clusterManager.addItems(taxis); // Agregar taxis reales
                    clusterManager.cluster(); // Actualizar el ClusterManager
                } else {
                    Log.e("MapActivity", "ClusterManager no inicializado.");
                }
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

    private void activateDestination() {
        // Referencias necesarias
        ImageButton btnActivateOrigin = findViewById(R.id.btnActivateLocation);
        ImageButton btnActivateDestination = findViewById(R.id.btnActivateDestination);
        LinearLayout layoutButtonsOrigin = findViewById(R.id.layout_buttons_origin);
        LinearLayout layoutButtonsDestination = findViewById(R.id.layout_buttons_destination);
        EditText editTextDestination = findViewById(R.id.editTextDestination);

        loadFavoriteDestinations();

        // Llama a toggleSelection para activar el destino
        toggleSelection(false, btnActivateOrigin, btnActivateDestination, layoutButtonsOrigin, layoutButtonsDestination);

        // Posicionar el cursor en el campo de destino sin activar el teclado
        if (editTextDestination != null) {
            editTextDestination.requestFocus();
            editTextDestination.setShowSoftInputOnFocus(true); // No mostrar el teclado
        }
    }

    // Verificación de permisos del mapa
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return true; // Permiso otorgado
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false; // Permiso aún no otorgado
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        if (origin == null || destination == null) {
            Toast.makeText(this, "Origen o destino no definido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usa la API Directions para obtener la ruta
        String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + getString(R.string.map_api_key);

        // Hacer una solicitud HTTP para obtener la ruta
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, directionsUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray routes = jsonResponse.getJSONArray("routes");

                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String points = overviewPolyline.getString("points");

                            // Decodificar la polyline
                            List<LatLng> polylinePoints = decodePoly(points);

                            // Dibujar la polyline en el mapa
                            if (routePolyline != null) {
                                routePolyline.remove(); // Eliminar la ruta anterior si existe
                            }
                            routePolyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(polylinePoints)
                                    .width(10)
                                    .color(ContextCompat.getColor(this, R.color.secondaryColor))
                                    .geodesic(true));
                        } else {
                            Toast.makeText(this, "No se encontró una ruta.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar la ruta.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error al obtener la ruta.", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    // Método para decodificar los puntos de la polyline
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    private void loadFavoriteDestinations() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");

        new MapController().fetchFavoriteDestinations(this, clienteId, new MapController.FavoriteDestinationsCallback() {
            @Override
            public void onFavoritesReceived(List<MapController.FavoriteDestination> favorites) {
                LinearLayout layoutFavorites = findViewById(R.id.layout_favorite_destinations);
                layoutFavorites.setVisibility(View.VISIBLE);
                layoutFavorites.removeAllViews();

                int limit = Math.min(favorites.size(), 3); // Mostrar máximo 3 favoritos
                for (int i = 0; i < limit; i++) {
                    MapController.FavoriteDestination favorite = favorites.get(i);

                    TextView textView = new TextView(MapActivity.this);
                    textView.setText(favorite.getAddress());
                    textView.setTextSize(16);
                    textView.setPadding(8, 8, 8, 8);
                    textView.setOnClickListener(v -> {
                        LatLng favoriteLocation = new LatLng(favorite.getLatitude(), favorite.getLongitude());
                        placeMarkerAndShowAddress(favoriteLocation, false); // Marcar como destino
                    });

                    layoutFavorites.addView(textView);
                }
            }

            @Override
            public void onNoFavoritesFound(String message) {
                LinearLayout layoutFavorites = findViewById(R.id.layout_favorite_destinations);
                layoutFavorites.setVisibility(View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateRoute() {
        if (originCoordinates != null && destinationCoordinates != null) {
            drawRoute(originCoordinates, destinationCoordinates);
        }
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}