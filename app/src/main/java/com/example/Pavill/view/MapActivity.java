package com.example.Pavill.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import com.example.Pavill.R;
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
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.location.Location;
import android.Manifest;
import android.util.Log;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.example.Pavill.components.PlacesAutoCompleteAdapter;  // Asegúrate de que este sea el paquete correcto.


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Variables del mapa y navegación
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private GoogleMap mMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Estado de selección
    private boolean isOriginSelectionActive = false;
    private boolean isDestinationSelectionActive = false;

    // Marcadores de origen y destino
    private Marker originMarker;
    private Marker destinationMarker;
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private PlacesClient placesClient;
    private EditText editTextOrigin, editTextDestination;
    private RecyclerView recyclerViewSuggestionsOrigin, recyclerViewSuggestionsDestination;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Inicializar el cliente de Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCiO3MUfI0sejMPz0v0IowvKmnc3706iJs");
        }
        placesClient = Places.createClient(this);  // Asegúrate de asignar placesClient aquí correctamente

        // Referencias a los EditText y RecyclerView
        editTextOrigin = findViewById(R.id.editTextOrigin);
        editTextDestination = findViewById(R.id.editTextDestination);
        recyclerViewSuggestionsOrigin = findViewById(R.id.recyclerViewSuggestionsOrigin);
        recyclerViewSuggestionsDestination = findViewById(R.id.recyclerViewSuggestionsDestination);

        // Configurar LayoutManager para los RecyclerView
        recyclerViewSuggestionsOrigin.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestionsDestination.setLayoutManager(new LinearLayoutManager(this));

        // Agregar TextWatcher para el autocompletado
        setupAutoComplete(editTextOrigin, recyclerViewSuggestionsOrigin, true);  // Para origen
        setupAutoComplete(editTextDestination, recyclerViewSuggestionsDestination, false);  // Para destino

        // Inicializar el FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar si los permisos de ubicación están otorgados, si no, solicitarlos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // Si ya tiene los permisos, continuar con la inicialización del mapa
            initializeMapAndLocationServices();
        }

        // Inicializar botones, mapa y otras configuraciones
        initializeButtons();
        initializeBottomSheetAndDrawer();

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Listener para seleccionar el input de origen
        editTextOrigin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // Activar el botón de origen cuando el campo de texto recibe el foco
                    findViewById(R.id.btnActivateLocation).performClick();
                }
            }
        });

        // Listener para seleccionar el input de destino
        editTextDestination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // Activar el botón de destino cuando el campo de texto recibe el foco
                    findViewById(R.id.btnActivateDestination).performClick();
                }
            }
        });

        // boton para marcar origen en ubicación actual

        // En tu onCreate o en el método donde inicializas los botones
        Button btnSetOriginToCurrentLocation = findViewById(R.id.btnSetOriginToCurrentLocation);
        btnSetOriginToCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si el permiso de ubicación ha sido concedido
                if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Obtener la última ubicación conocida
                    fusedLocationClient.getLastLocation().addOnSuccessListener(MapActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Si la ubicación es válida, usarla para el origen
                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                // Marcar la ubicación actual como origen
                                placeMarkerAndShowAddress(currentLocation, true);

                                // Opcionalmente, mover la cámara a la ubicación actual
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            } else {
                                Toast.makeText(MapActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // Si no hay permisos, solicitarlos
                    ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
        });


    }

    private void initializeButtons() {
        final ImageButton btnActivateOrigin = findViewById(R.id.btnActivateLocation);
        final ImageButton btnActivateDestination = findViewById(R.id.btnActivateDestination);

        // Activar/Desactivar selección de origen
        btnActivateOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOriginSelectionActive) {
                    // Activar origen y desactivar destino
                    activateSelection(btnActivateOrigin, btnActivateDestination, true);
                } else {
                    deactivateSelection(btnActivateOrigin);
                }
            }
        });

        // Activar/Desactivar selección de destino
        btnActivateDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDestinationSelectionActive) {
                    // Activar destino y desactivar origen
                    activateSelection(btnActivateDestination, btnActivateOrigin, false);
                } else {
                    deactivateSelection(btnActivateDestination);
                }
            }
        });
    }

    // Inicializa el BottomSheet y el DrawerLayout
    private void initializeBottomSheetAndDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Botón del menú lateral
        ImageButton btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
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

        // Verificar si el permiso de ubicación ha sido concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Obtener la última ubicación conocida
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // Si la ubicación es válida, centrar la cámara en la ubicación del usuario
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));

                        // Mostrar carros simulados cerca de la ubicación actual
                        addNearbyCars(myLocation);
                    } else {
                        // Si no se puede obtener la ubicación, centrar en Tacna, Perú
                        LatLng tacnaLocation = new LatLng(-18.0146, -70.2536);  // Tacna, Perú
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tacnaLocation, 15));
                    }
                }
            });
        } else {
            // Si no hay permisos, centrar en Tacna, Perú
            LatLng tacnaLocation = new LatLng(-18.0146, -70.2536);  // Tacna, Perú
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tacnaLocation, 15));
        }
    }

    private void addNearbyCars(LatLng userLocation) {
        // Generar ubicaciones cercanas al usuario
        LatLng car1 = new LatLng(userLocation.latitude + 0.001, userLocation.longitude + 0.001);
        LatLng car2 = new LatLng(userLocation.latitude - 0.001, userLocation.longitude - 0.001);
        LatLng car3 = new LatLng(userLocation.latitude + 0.0015, userLocation.longitude - 0.0015);

        // Obtener el ícono redimensionado
        BitmapDescriptor smallCarIcon = getResizedBitmap(R.drawable.ic_car, 30, 50);  // Redimensionar a 80x80 px

        // Añadir marcadores para los carros en las ubicaciones simuladas
        mMap.addMarker(new MarkerOptions()
                .position(car1)
                .title("Taxi cercano")
                .icon(smallCarIcon));

        mMap.addMarker(new MarkerOptions()
                .position(car2)
                .title("Taxi cercano")
                .icon(smallCarIcon));

        mMap.addMarker(new MarkerOptions()
                .position(car3)
                .title("Taxi cercano")
                .icon(smallCarIcon));
    }

    // Método para redimensionar el ícono
    private BitmapDescriptor getResizedBitmap(int drawableRes, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    // Activa la selección de origen o destino
    private void activateSelection(ImageButton activeButton, ImageButton inactiveButton, boolean isForOrigin) {
        isOriginSelectionActive = isForOrigin;
        isDestinationSelectionActive = !isForOrigin;

        // Actualizar los íconos
        activeButton.setImageResource(isForOrigin ? R.drawable.ic_my_location_active : R.drawable.ic_my_location_active);
        inactiveButton.setImageResource(isForOrigin ? R.drawable.ic_my_location : R.drawable.ic_my_location);

        // Activar selección en el mapa
        activateLocationSelection(isForOrigin);
    }

    // Desactiva la selección
    private void deactivateSelection(ImageButton button) {
        isOriginSelectionActive = false;
        isDestinationSelectionActive = false;

        // Actualizar ícono
        button.setImageResource(button == findViewById(R.id.btnActivateLocation) ? R.drawable.ic_my_location : R.drawable.ic_my_location);

        deactivateLocationSelection();
    }

    // Configura el clic en el mapa para seleccionar origen o destino
    private void activateLocationSelection(final boolean isForOrigin) {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                placeMarkerAndShowAddress(latLng, isForOrigin);
            }
        });
    }

    // Coloca el marcador y muestra la dirección
    private void placeMarkerAndShowAddress(LatLng latLng, boolean isForOrigin) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(isForOrigin ? "Ubicación de origen" : "Ubicación de destino")
                .icon(BitmapDescriptorFactory.defaultMarker(isForOrigin ? BitmapDescriptorFactory.HUE_VIOLET : BitmapDescriptorFactory.HUE_ORANGE));

        // Actualiza el marcador correcto
        if (isForOrigin) {
            if (originMarker != null) originMarker.remove();
            originMarker = mMap.addMarker(markerOptions);
            originCoordinates = latLng;
        } else {
            if (destinationMarker != null) destinationMarker.remove();
            destinationMarker = mMap.addMarker(markerOptions);
            destinationCoordinates = latLng;
        }

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

    // Desactiva el clic en el mapa
    private void deactivateLocationSelection() {
        mMap.setOnMapClickListener(null);
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
}


