package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.Pavill.R;
import com.example.Pavill.components.CircularImageView;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.CancelRequestController;
import com.example.Pavill.controller.DriverLocationController;
import com.example.Pavill.controller.PedidoController;
import com.example.Pavill.controller.PedidoStatusController;
import com.example.Pavill.controller.RouteController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class WaitingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker driverMarker;
    private Handler locationUpdateHandler = new Handler();

    private Handler pedidoStatusHandler = new Handler(); // Nuevo handler para verificar el estado del pedido
    private Runnable pedidoStatusChecker; // Runnable para manejar las verificaciones

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView errorText;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Inicializar TemporaryData
        temporaryData = TemporaryData.getInstance();

        // Inicializar coordenadas de origen y destino
        originCoordinates = temporaryData.getOriginCoordinates();
        destinationCoordinates = temporaryData.getDestinationCoordinates();

        // Configurar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configurar UI
        initializeUI();
        loadConductorPhoto();

        // Iniciar actualizaciones de ubicación del conductor
        startFetchingDriverLocation();

        // Iniciar verificación del estado del pedido
        startPedidoStatusChecker();
    }

    private void initializeUI() {
        // Inicializar campos de conductor
        String conductorId = temporaryData.getConductorId();
        String conductorNombre = temporaryData.getConductorNombre();
        String conductorTelefono = temporaryData.getConductorTelefono();
        String unidadPlaca = temporaryData.getUnidadPlaca();
        String unidadModelo = temporaryData.getUnidadModelo();
        String unidadColor = temporaryData.getUnidadColor();
        String unidadCalificacion = temporaryData.getUnidadCalificacion();

        // Configurar BottomSheet
        initializeBottomSheet();
        // Otros inicializadores...
        double value = Double.parseDouble(unidadCalificacion);
        int roundedValue = (int) Math.round(value);
        updateRatingBar(roundedValue); // Actualiza las estrellas con base en la puntuación

        // Configurar TextViews de la UI
        TextView textViewDriverName = findViewById(R.id.textViewDriverName);
        TextView textViewDriverCode = findViewById(R.id.textViewDriverCode);
        TextView textViewCarDetails = findViewById(R.id.textViewCarDetails);
        errorText = findViewById(R.id.errorText);

        textViewDriverName.setText(conductorNombre);
        textViewDriverCode.setText("Código: " + conductorId);
        textViewCarDetails.setText("Modelo: " + unidadModelo+ " - " + unidadColor + " | Placa: " + unidadPlaca);

        // Configurar botones
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> {
            if (conductorTelefono != null && !conductorTelefono.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + conductorTelefono));
                startActivity(intent);
            } else {
                showError("Número de teléfono no disponible.");
            }
        });

        findViewById(R.id.btnMessage).setOnClickListener(v -> {
            if (conductorTelefono != null && !conductorTelefono.isEmpty()) {
                String clientName = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .getString("ClienteNombre", "Cliente");
                String message = "Soy " + clientName + ", pedí un pavill. Quisiera saber si hay alguna dificultad.";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + conductorTelefono + "?text=" + Uri.encode(message)));
                startActivity(intent);
            } else {
                showError("Número de teléfono no disponible.");
            }
        });

        findViewById(R.id.btnCancelSearch).setOnClickListener(v -> {
            new CancelRequestController().cancelRequest(this, new CancelRequestController.CancelRequestCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(WaitingActivity.this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show();

                    // limpiar TemporaryData
                    temporaryData = TemporaryData.getInstance();
                    temporaryData.clearData();

                    // Crear un intent para regresar al MapActivity
                    Intent intent = new Intent(WaitingActivity.this, MapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                    startActivity(intent);
                    finish(); // Finalizar la WaitingActivity
                }

                @Override
                public void onFailure(String errorMessage) {
                    showError(errorMessage);
                }
            });

        });

        findViewById(R.id.btnOnBoard).setOnClickListener(v -> checkAndProceedToProgress());


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (originCoordinates != null && destinationCoordinates != null) {
            // Agregar marcadores
            mMap.addMarker(new MarkerOptions()
                    .position(originCoordinates)
                    .title("Ubicación de origen")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

            mMap.addMarker(new MarkerOptions()
                    .position(destinationCoordinates)
                    .title("Ubicación de Destino")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            // Ajustar cámara para que ambos puntos sean visibles
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(originCoordinates)
                    .include(destinationCoordinates)
                    .build();

            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            drawRoute(originCoordinates, destinationCoordinates);
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + getString(R.string.map_api_key);

        new RouteController(this).fetchRoute(url, new RouteController.RouteCallback() {
            @Override
            public void onRouteReceived(List<LatLng> route) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .color(getResources().getColor(R.color.secondaryColor))
                        .width(10));
            }

            @Override
            public void onError(String errorMessage) {
                showError("Error al cargar la ruta: " + errorMessage);
            }
        });
    }

    private void startFetchingDriverLocation() {
        locationUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new DriverLocationController().fetchDriverLocation(WaitingActivity.this, new DriverLocationController.DriverLocationCallback() {
                    @Override
                    public void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes) {
                        LatLng driverLocation = new LatLng(lat, lng);

                        // Redimensionar el icono del taxi
                        int iconSize = convertDpToPx(40);

                        // Actualizar marcador del conductor con animación
                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverLocation)
                                    .icon(resizeIcon(R.drawable.ic_car, iconSize, iconSize))
                                    .title("Conductor en camino"));
                        } else {
                            animateMarkerTo(driverMarker, driverLocation);
                        }

                        // Mostrar tiempo estimado
                        TextView estimatedTimeView = findViewById(R.id.textViewETA);
                        estimatedTimeView.setText("Tiempo estimado: " + estimatedTimeMinutes + " minutos");

                        // Verificar si el taxi está cerca del origen
                        float[] results = new float[1];
                        Location.distanceBetween(
                                driverLocation.latitude, driverLocation.longitude,
                                originCoordinates.latitude, originCoordinates.longitude,
                                results
                        );

                        float distanceToOrigin = results[0]; // Distancia en metros
                        if (distanceToOrigin <= 100) { // Rango de 100 metros
                            showArrivalDialog();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                });

                locationUpdateHandler.postDelayed(this, 2000); // Repetir cada 2 segundos
            }
        }, 5000);
    }

    private void showArrivalDialog() {
        // Crear instancia del diálogo
        ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
        String arrivalMessage = temporaryData.getConductorNombre() + ", tu pavill ya llegó";
        String buttonText = "A bordo";

        // Configurar texto del mensaje y botón
        arrivalMessageDialog.setDriverName(arrivalMessage);
        arrivalMessageDialog.setButtonText(buttonText);

        // Configurar el listener para el botón "A bordo"
        arrivalMessageDialog.setOnConfirmClickListener(() -> checkAndProceedToProgress());



        // Mostrar el diálogo
        arrivalMessageDialog.show(getSupportFragmentManager(), "ArrivalMessageDialog");
    }


    private void showError(String errorMessage) {
        errorText.setText(errorMessage);
        errorText.setVisibility(View.VISIBLE);
    }

    private BitmapDescriptor resizeIcon(int resourceId, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), resourceId);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void animateMarkerTo(final Marker marker, final LatLng toPosition) {
        final LatLng fromPosition = marker.getPosition();
        final long duration = 1000;

        final Handler handler = new Handler();
        final long start = System.currentTimeMillis();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - start;
                float t = Math.min(1, elapsed / (float) duration);
                double lat = (toPosition.latitude - fromPosition.latitude) * t + fromPosition.latitude;
                double lng = (toPosition.longitude - fromPosition.longitude) * t + fromPosition.longitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void updateRatingBar(int rating) {

        // Referencias de las estrellas en el LinearLayout
        ImageView[] stars = new ImageView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        // Recorre las estrellas y asigna el color según la puntuación
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setColorFilter(getResources().getColor(com.google.android.libraries.places.R.color.quantum_yellow)); // Estrellas calificadas
            } else {
                stars[i].setColorFilter(getResources().getColor(com.google.android.libraries.places.R.color.quantum_grey)); // Estrellas no calificadas
            }
        }
    }

    private void checkAndProceedToProgress() {
        String pedidoId = temporaryData.getPedidoId();
        String conductorId = temporaryData.getConductorId();

        if (pedidoId == null || conductorId == null) {
            showError("Faltan datos para procesar el pedido.");
            return;
        }

        // Obtén las coordenadas del vehículo desde el DriverLocationController
        new DriverLocationController().fetchDriverLocation(this, new DriverLocationController.DriverLocationCallback() {
            @Override
            public void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes) {
                // Calcular si hubo tardanza (mayor a 5 minutos)
                long timeSinceRequest = System.currentTimeMillis() - temporaryData.getRequestTime();
                int tardanza = (timeSinceRequest > 5 * 60 * 1000) ? 1 : 0; // 1 = tardanza, 0 = no tardanza

                // Preparar y enviar los datos al controlador PedidoController
                new PedidoController().marcarAbordo(WaitingActivity.this, pedidoId, conductorId, lat, lng, tardanza, new PedidoController.AbordoCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Redirigir a ProgressActivity
                        Intent intent = new Intent(WaitingActivity.this, ProgressActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showError(errorMessage); // Mostrar error si no se pudo marcar como "A bordo"
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showError("Error al obtener ubicación del vehículo: " + errorMessage);
            }
        });
    }

    private void startPedidoStatusChecker() {
        String pedidoId = temporaryData.getPedidoId();

        if (pedidoId == null) {
            showError("ID de pedido no disponible.");
            return;
        }

        pedidoStatusChecker = new Runnable() {
            @Override
            public void run() {
                new PedidoStatusController().checkPedidoStatus(WaitingActivity.this, new PedidoStatusController.PedidoStatusCallback() {
                    @Override
                    public void onStatusReceived(String status, String message) {
                        if ("CANCELADO".equalsIgnoreCase(status)) {
                            // Detener verificaciones
                            pedidoStatusHandler.removeCallbacks(pedidoStatusChecker);

                            // Limpiar TemporaryData
                            temporaryData = TemporaryData.getInstance();
                            temporaryData.clearData();

                            // Redirigir a MapActivity
                            Intent intent = new Intent(WaitingActivity.this, MapActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                            startActivity(intent);
                            finish(); // Finalizar la WaitingActivity

                            // Mostrar un mensaje de cancelación
                            Toast.makeText(WaitingActivity.this, "El pedido ha sido cancelado.", Toast.LENGTH_SHORT).show();

                        } else if ("EN_ESPERA".equalsIgnoreCase(status)) {
                            // Detener verificaciones
                            pedidoStatusHandler.removeCallbacks(pedidoStatusChecker);

                            // Mostrar un mensaje indicando la búsqueda de un nuevo conductor
                            Toast.makeText(WaitingActivity.this, "El conductor canceló el pedido, buscando nuevo Pavill.", Toast.LENGTH_SHORT).show();

                            // Redirigir a SearchingActivity
                            Intent intent = new Intent(WaitingActivity.this, SearchingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                            startActivity(intent);
                            finish(); // Finalizar la WaitingActivity
                        } else {
                            // Continuar verificando si no está cancelado o en espera
                            pedidoStatusHandler.postDelayed(pedidoStatusChecker, 3000); // Reintentar después de 3 segundos
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // En caso de error, continuar verificando
                        pedidoStatusHandler.postDelayed(pedidoStatusChecker, 3000); // Reintentar después de 3 segundos
                    }
                });
            }
        };

        // Ejecutar la verificación inicial
        pedidoStatusHandler.post(pedidoStatusChecker);
    }

    //para la foto del conductor
    private void loadConductorPhoto() {
        String conductorFoto = TemporaryData.getInstance().getConductorFoto();
        CircularImageView profileImage = findViewById(R.id.profile_image);

        if (conductorFoto != null && !conductorFoto.isEmpty()) {
            // Usar Glide para cargar la imagen
            Glide.with(this)
                    .load(conductorFoto)
                    .placeholder(R.drawable.img_conductor) // Imagen por defecto mientras se carga
                    .error(R.drawable.img_conductor) // Imagen por defecto si hay un error
                    .into(profileImage);
        } else {
            // Usa la imagen por defecto
            profileImage.setImageResource(R.drawable.img_conductor);
        }
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void initializeBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(100);
        bottomSheetBehavior.setDraggable(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationUpdateHandler.removeCallbacksAndMessages(null);
        pedidoStatusHandler.removeCallbacksAndMessages(null); // Detener el handler al destruir la actividad
    }
}