package radiotaxipavill.radiotaxipavillapp.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.CircularImageView;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.PedidoCancellationHelper;
import radiotaxipavill.radiotaxipavillapp.components.PedidoServiceHelper;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.components.BitmapUtils;
import radiotaxipavill.radiotaxipavillapp.controller.CancelRequestController;
import radiotaxipavill.radiotaxipavillapp.controller.DriverLocationController;
import radiotaxipavill.radiotaxipavillapp.controller.PedidoController;
import radiotaxipavill.radiotaxipavillapp.controller.PedidoStatusController;
import radiotaxipavill.radiotaxipavillapp.controller.RecibirMensajeController;
import radiotaxipavill.radiotaxipavillapp.controller.RouteController;
import radiotaxipavill.radiotaxipavillapp.receiver.PedidoStatusReceiver;
import radiotaxipavill.radiotaxipavillapp.services.PedidoStatusService;

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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class WaitingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Handler mensajehandler = new Handler();
    private Runnable runnable;
    private static final int INTERVALO_CONSULTA = 5000; // Cada 5 segundos

    private GoogleMap mMap;
    private Marker driverMarker;
    private Handler locationUpdateHandler = new Handler();

    private boolean hasArrivalMessageShown = false; // Bandera para controlar el mensaje flotante

    private String conductorId;
    private String vehiculoUnidad;
    private String conductorNombre;
    private String conductorTelefono;
    private String unidadPlaca;
    private String unidadModelo;
    private String unidadColor;
    private String unidadCalificacion;
    private String conductorFoto;

    private int initialEstimatedTime = -1; // Tiempo inicial de llegada
    private boolean isCountdownStarted = false; // Bandera para controlar el inicio del contador

    private LoadingDialog loadingDialog;

    private PedidoStatusReceiver pedidoStatusReceiver;
    // Variable global para indicar si está en tardanza (1 = tardanza, 0 = no tardanza)
    private int tardanzaFlag = 0;

    private Handler pedidoStatusHandler = new Handler(); // Nuevo handler para verificar el estado del pedido

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TemporaryData temporaryData;

    private static final float MIN_DISTANCE_THRESHOLD_METERS = 5f;
    private static final long DRIVER_UPDATE_INTERVAL = 3000; // cada 3 segundos
    private Runnable driverLocationRunnable;

    private ProgressBar loadingDriverLocation;
    private boolean isFirstLocationReceived = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        loadingDialog = new LoadingDialog(this);

        // Inicializar TemporaryData
        temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);  // 🔹 Restaurar datos guardados

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

        // Iniciar actualizaciones de ubicación del conductor
        startFetchingDriverLocation();

        loadConductorPhoto();

        // Iniciar el servicio de seguimiento de pedidos
        Intent serviceIntent = new Intent(this, PedidoStatusService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent); // Para Android 8+
        } else {
            this.startService(serviceIntent);
        }

    }

    /**
     * Inicializa la UI con los elementos de la actividad.
     */
    private void initializeUI() {
        // Inicializar campos de conductor
        conductorId = temporaryData.getConductorId();
        vehiculoUnidad = temporaryData.getVehiculoUnidad();
        conductorNombre = temporaryData.getConductorNombre();
        conductorTelefono = temporaryData.getConductorTelefono();
        unidadPlaca = temporaryData.getUnidadPlaca();
        unidadModelo = temporaryData.getUnidadModelo();
        unidadColor = temporaryData.getUnidadColor();
        unidadCalificacion = temporaryData.getUnidadCalificacion();
        conductorFoto = temporaryData.getConductorFoto();
        loadingDriverLocation = findViewById(R.id.loadingDriverLocation);
        Log.d("Conductor foto:", "initializeUI: " + conductorFoto);

        // Verificar que unidadCalificacion sea un número válido
        double value;
        try {
            value = Double.parseDouble(unidadCalificacion);
        } catch (NumberFormatException e) {
            // Si no es un número válido, asignar un valor por defecto
            value = 0.0;
            Log.e("WaitingActivity", "UnidadCalificacion no es un número válido: " + unidadCalificacion, e);
        }

        // Configurar BottomSheet
        initializeBottomSheet();
        // Calificación de estrellas
        int roundedValue = (int) Math.round(value);
        updateRatingBar(roundedValue); // Actualiza las estrellas con base en la puntuación

        // Configurar TextViews de la UI
        TextView textViewDriverName = findViewById(R.id.textViewDriverName);
        TextView textViewDriverCode = findViewById(R.id.textViewDriverCode);
        TextView textViewCarDetails = findViewById(R.id.textViewCarDetails);

        textViewDriverName.setText(toCapitalLetter(conductorNombre));
        textViewDriverCode.setText(vehiculoUnidad);
        textViewCarDetails.setText("Modelo: " + unidadModelo+ " - " + unidadColor + " | Placa: " + unidadPlaca);

        // Configurar botones
        initializeBottomsUI();
    }

    /**
     * convierte a tipo de letra capital
     * @param input
     * @return
     */
    private String toCapitalLetter(String input) {
        if (input == null || input.isEmpty()) {
            return "";  // En lugar de devolver null, devolvemos un string vacío
        }

        StringBuilder capitalized = new StringBuilder();
        String[] words = input.toLowerCase().split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0))) // Primera letra en mayúscula
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return capitalized.toString().trim();  // Eliminamos el espacio extra al final
    }


    /**
     * Inicializa los botones de llamada, whatsapp y de cancelar búsqueda
     */
    public void initializeBottomsUI(){
        //Botón de llamada
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> {

            if (conductorTelefono != null && !conductorTelefono.isEmpty()) {
                loadingDialog.show();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + conductorTelefono));
                startActivity(intent);
                loadingDialog.dismiss();
            } else {
                showError("Número de teléfono no disponible.");
            }
        });

        //Botón de mensaje de chat
        findViewById(R.id.btnMessage).setOnClickListener(v -> {
            if (conductorTelefono != null && !conductorTelefono.isEmpty()) {
                MessageSelectionDialog dialog = new MessageSelectionDialog(WaitingActivity.this, conductorTelefono);
                dialog.show(getSupportFragmentManager(), "MessageSelectionDialog");
            } else {
                showError("Número de teléfono no disponible.");
            }
        });

        //Botón de cancelar pedido
        findViewById(R.id.btnCancelSearch).setOnClickListener(v -> {
            loadingDialog.show();
            new CancelRequestController().cancelRequest(this, new CancelRequestController.CancelRequestCallback() {
                @Override
                public void onSuccess(String message) {
                    loadingDialog.dismiss();
                    Toast.makeText(WaitingActivity.this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show();

                    // Crear un intent para ir al cancel reason activity
                    Intent intent = new Intent(WaitingActivity.this, CancelReasonActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                    startActivity(intent);
                    finish(); // Finalizar la WaitingActivity
                }

                @Override
                public void onFailure(String errorMessage) {
                    loadingDialog.dismiss();
                    showError(errorMessage);
                }
            });

        });

        //Botón de continuar
        findViewById(R.id.btnOnBoard).setOnClickListener(v -> checkAndProceedToProgress());
        //Botón de detalles
        findViewById(R.id.btn_details).setOnClickListener(v -> showPedidoDetailsDialog());

    }

    private void showPedidoDetailsDialog() {
        TemporaryData data = TemporaryData.getInstance();
        data.loadFromPreferences(this); // Asegura que los datos estén actualizados

        StringBuilder message = new StringBuilder();

        // Sección 1: Detalles del conductor
        message.append("🚖 Detalles del conductor\n\n");

        if (data.getConductorNombre() != null) message.append("• Nombre: ").append(data.getConductorNombre()).append("\n");
        if (data.getConductorTelefono() != null) message.append("• Teléfono: ").append(data.getConductorTelefono()).append("\n");
        if (data.getVehiculoUnidad() != null) message.append("• Unidad: ").append(data.getVehiculoUnidad()).append("\n");
        if (data.getUnidadModelo() != null) message.append("• Modelo: ").append(data.getUnidadModelo()).append("\n");
        if (data.getUnidadPlaca() != null) message.append("• Placa: ").append(data.getUnidadPlaca()).append("\n");
        if (data.getUnidadColor() != null) message.append("• Color: ").append(data.getUnidadColor()).append("\n");

        message.append("\n"); // Espacio entre secciones

        // Sección 2: Detalles del pedido
        message.append("🚖 Detalles de la carrera\n\n");

        if (data.getOriginName() != null) message.append("• Lugar de origen: ").append(data.getOriginName()).append("\n");
        if (data.getOriginReference() != null) message.append("• Referencia: ").append(data.getOriginReference()).append("\n");
        if (data.getDestinationName() != null) message.append("• Lugar de destino: ").append(data.getDestinationName()).append("\n");
        if (data.getEstimatedCost() != null) message.append("• Costo estimado: S/").append(data.getEstimatedCost()).append("\n");

        // Si está vacío, mostrar mensaje
        if (message.toString().trim().isEmpty()) {
            message.append("No hay detalles disponibles del pedido.");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📋 Información del Pedido")
                .setMessage(message.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        applyMapStyle();

        if (originCoordinates != null) {
            // Agregar marcador de origen
            mMap.addMarker(new MarkerOptions()
                    .position(originCoordinates)
                    .title("Ubicación de origen")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            Log.d("WaitingActivity", "destino: " + destinationCoordinates);
            if (destinationCoordinates.latitude != 0.0 && destinationCoordinates.longitude != 0.0) {
                // Agregar marcador de destino
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

                // Dibujar ruta solo si el destino está definido
                drawRoute(originCoordinates, destinationCoordinates);
            } else {
                // Si no hay destino, solo centrar en el origen con un nivel de zoom adecuado
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originCoordinates, 15f));
            }
        }
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
                    Log.e("WaitingActivity", "Error aplicando estilo oscuro.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("WaitingActivity", "Archivo de estilo oscuro no encontrado.", e);
            }
        } else {
            // Modo normal: no aplicar estilo (usar predeterminado)
            mMap.setMapStyle(null);
        }
    }

    /**
     * Dibuja la ruta entre el origen y el destino en el mapa.
     * @param origin
     * @param destination
     */
    private void drawRoute(LatLng origin, LatLng destination) {
        if (origin == null || destination == null) {
            showError("Origen o destino no definido.");
            return;
        }

        RouteController routeController = new RouteController(this);
        routeController.fetchRoute(origin, destination, null, new RouteController.RouteCallback() {
            @Override
            public void onRouteSuccess(List<LatLng> route, String distanceText, String durationText, double estimatedCost) {
                // Dibujar la polyline en el mapa
                mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .color(getResources().getColor(R.color.secondaryColor))
                        .width(10));
            }

            @Override
            public void onRouteError(String errorMessage) {
                showError("Error al cargar la ruta: " + errorMessage);
            }
        });
    }


    /**
     * Actualizar ubicación del conductor
     */
    private void startFetchingDriverLocation() {
        driverLocationRunnable = new Runnable() {
            @Override
            public void run() {
                new DriverLocationController().fetchDriverLocation(
                        WaitingActivity.this,
                        new DriverLocationController.DriverLocationCallback() {
                            @Override
                            public void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes) {
                                LatLng driverLocation = new LatLng(lat, lng);

                                // Ocultar loader después de la primera ubicación válida
                                if (!isFirstLocationReceived) {
                                    loadingDriverLocation.setVisibility(View.GONE);
                                    isFirstLocationReceived = true;
                                }

                                int iconSize = 32;
                                if (driverMarker == null) {
                                    driverMarker = mMap.addMarker(new MarkerOptions()
                                            .position(driverLocation)
                                            .icon(BitmapUtils.getProportionalBitmap(WaitingActivity.this, R.drawable.ic_car, iconSize))
                                            .title("Conductor en camino"));
                                } else {
                                    animateMarkerTo(driverMarker, driverLocation);
                                }

                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15));

                                if (initialEstimatedTime == -1) {
                                    initialEstimatedTime = calculateEstimatedTimeToOrigin(driverLocation, originCoordinates);
                                    ((TextView) findViewById(R.id.textViewETA)).setText("Tiempo estimado: " + initialEstimatedTime + " minutos");

                                    if (!isCountdownStarted) {
                                        isCountdownStarted = true;
                                        startEstimatedTimeCountdown(initialEstimatedTime);
                                    }
                                }

                                checkProximityToOrigin(driverLocation);

                                locationUpdateHandler.postDelayed(driverLocationRunnable, DRIVER_UPDATE_INTERVAL);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("DriverLocation", "Error al obtener ubicación: " + errorMessage);

                                // Muestra el loader nuevamente si no hay datos
                                if (!isFirstLocationReceived) {
                                    loadingDriverLocation.setVisibility(View.VISIBLE);
                                }

                                locationUpdateHandler.postDelayed(driverLocationRunnable, DRIVER_UPDATE_INTERVAL);
                            }
                        }
                );
            }
        };

        locationUpdateHandler.post(driverLocationRunnable); // Iniciar por primera vez
    }



    /**
     * Calcula el tiempo estimado de llegada al origen.
     */
    private int calculateEstimatedTimeToOrigin(LatLng driverLocation, LatLng originCoordinates) {
        float[] results = new float[1];
        Location.distanceBetween(
                driverLocation.latitude, driverLocation.longitude,
                originCoordinates.latitude, originCoordinates.longitude,
                results
        );
        float distanceInMeters = results[0];
        int estimatedTime = (int) (distanceInMeters / 300); // Ejemplo: Suponiendo 300 metros por minuto
        return Math.max(1, estimatedTime); // Mínimo de 1 minuto
    }

    /**
     * Verifica si el conductor está cerca del origen.
     */
    private void checkProximityToOrigin(LatLng driverLocation) {
        float[] results = new float[1];
        Location.distanceBetween(
                driverLocation.latitude, driverLocation.longitude,
                originCoordinates.latitude, originCoordinates.longitude,
                results
        );

        float distanceToOrigin = results[0]; // Distancia en metros
        if (distanceToOrigin <= 100 && !hasArrivalMessageShown) { // Mostrar solo si no se ha mostrado antes
            showArrivalDialog();
            hasArrivalMessageShown = true; // Actualizar la bandera
        }
    }

    /**
     * Método para iniciar el contador de tiempo estimado
     * @param initialTimeMinutes Tiempo inicial en minutos
     */
    private void startEstimatedTimeCountdown(int initialTimeMinutes) {
        TextView estimatedTimeView = findViewById(R.id.textViewETA);

        // Variable global para marcar si hay tardanza (1 = tardanza, 0 = no tardanza)
        tardanzaFlag = 0;

        // Inicia un Handler para el contador
        Handler countdownHandler = new Handler();
        Runnable countdownRunnable = new Runnable() {
            int remainingTime = initialTimeMinutes; // Tiempo restante

            @Override
            public void run() {
                if (remainingTime > 0) {
                    // Actualiza el texto con el tiempo restante
                    estimatedTimeView.setText("Tiempo estimado: " + remainingTime + " minutos");
                    Log.d("Tiempo estimado:", "Tiempo restante: " + remainingTime + " minutos");
                    remainingTime--;

                    // Repite el Runnable cada minuto (1000ms x 60)
                    countdownHandler.postDelayed(this, 60 * 1000);
                } else {
                    // Cuando el tiempo llega a 0
                    estimatedTimeView.setTextColor(ContextCompat.getColor(WaitingActivity.this, R.color.alertColor));
                    estimatedTimeView.setText("Tiempo estimado: " + remainingTime + " minutos");
                    // Marcar como tardanza
                    tardanzaFlag = 1;
                    Log.d("TardanzaFlag", "Marcado como tardanza (1).");

                    // Detener el contador, no volver a llamar a postDelayed
                }
            }
        };

        // Ejecuta el Runnable inmediatamente
        countdownHandler.post(countdownRunnable);
    }


    /**
     * Muestra un mensaje flotante indicando que el conductor ya llegó.
     */
    private void showArrivalDialog() {
        ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
        String arrivalMessage = temporaryData.getConductorNombre() + ", tu pavill ya llegó";
        String buttonText = "A bordo";

        arrivalMessageDialog.setDriverName(arrivalMessage);
        arrivalMessageDialog.setButtonText(buttonText);

        arrivalMessageDialog.setOnConfirmClickListener(() -> checkAndProceedToProgress());

        arrivalMessageDialog.show(getSupportFragmentManager(), "ArrivalMessageDialog");
    }

    /**
     * Muestra un mensaje de error.
     * @param errorMessage
     */
    private void showError(String errorMessage) {
        Toast.makeText(WaitingActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

    }

    /**
     * Anima el marcador del conductor para que simule avanzar
     * @param marker
     * @param toPosition
     */
    private void animateMarkerTo(final Marker marker, final LatLng toPosition) {
        final LatLng fromPosition = marker.getPosition();

        float[] results = new float[1];
        Location.distanceBetween(
                fromPosition.latitude, fromPosition.longitude,
                toPosition.latitude, toPosition.longitude,
                results
        );
        float distance = results[0];

        if (distance < MIN_DISTANCE_THRESHOLD_METERS) {
            Log.d("animateMarkerTo", "🚫 Movimiento menor a " + MIN_DISTANCE_THRESHOLD_METERS + "m, se omite animación.");
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1500);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            double lat = (toPosition.latitude - fromPosition.latitude) * t + fromPosition.latitude;
            double lng = (toPosition.longitude - fromPosition.longitude) * t + fromPosition.longitude;
            marker.setPosition(new LatLng(lat, lng));
        });

        animator.start();
    }


    /**
     * Actualiza las estrellas del conductor según la calificación
     * @param rating
     */
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

    /**
     * Verifica el estado y procede a la actividad de progreso.
     */
    private void checkAndProceedToProgress() {
        String pedidoId = temporaryData.getPedidoId();
        String conductorId = temporaryData.getConductorId();

        if (pedidoId == null || conductorId == null) {
            showError("Faltan datos para procesar el pedido.");
            return;
        }

        loadingDialog.show();

        // Obtén las coordenadas del vehículo desde el DriverLocationController
        new DriverLocationController().fetchDriverLocation(this, new DriverLocationController.DriverLocationCallback() {
            @Override
            public void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes) {
                // Usa la variable global `tardanzaFlag` para determinar si hubo tardanza
                int tardanza = tardanzaFlag; // 1 = tardanza, 0 = no tardanza

                PedidoServiceHelper.updateSubPedidoState(WaitingActivity.this, "A_BORDO");

                // Preparar y enviar los datos al controlador PedidoController
                new PedidoController().marcarAbordo(WaitingActivity.this, pedidoId, conductorId, lat, lng, tardanza, new PedidoController.AbordoCallback() {
                    @Override
                    public void onSuccess(String message) {
                        loadingDialog.dismiss();
                        // Redirigir a ProgressActivity
                        Intent intent = new Intent(WaitingActivity.this, ProgressActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        loadingDialog.dismiss();
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
                    PedidoCancellationHelper.cancelProcess(WaitingActivity.this);
                    Toast.makeText(WaitingActivity.this, "El pedido ha sido cancelado.", Toast.LENGTH_SHORT).show();
                    break;

                case "EN_ESPERA":
                    Log.d("WaitingActivity", "EN_ESPERAEN_ESPERAEN_ESPERAEN_ESPERAEN_ESPERA");

                    Toast.makeText(WaitingActivity.this, "El conductor canceló el pedido, buscando nuevo Pavill.", Toast.LENGTH_SHORT).show();
                    Intent searchIntent = new Intent(WaitingActivity.this, SearchingActivity.class);
                    searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(searchIntent);
                    finish();
                    break;

                case "ACEPTADO":
                    Log.d("WaitingActivity", "Pedido aceptado. Conductor en camino.");
                    break;

                case "FINALIZADO":
                    PedidoServiceHelper.stopPedidoStatusService(WaitingActivity.this);
                    Toast.makeText(WaitingActivity.this, "Pedido finalizado.", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Log.d("WaitingActivity", "Estado desconocido: " + status);
                    break;
            }
        });

        // Registra el receptor con el intent-filter
        IntentFilter filter = new IntentFilter(PedidoStatusReceiver.ACTION_PEDIDO_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(pedidoStatusReceiver, filter);

        // Iniciar la escucha de mensajes
        iniciarRecepcionMensajes();

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

        // Detener la escucha de mensajes
        detenerRecepcionMensajes();
    }

    private void iniciarRecepcionMensajes() {
        RecibirMensajeController recibirMensajeController = new RecibirMensajeController();

        runnable = new Runnable() {
            @Override
            public void run() {
                recibirMensajeController.recibirMensaje(WaitingActivity.this, new RecibirMensajeController.MensajeCallback() {
                    @Override
                    public void onSuccess(String mensajeCliente) {
                        mostrarDialogoMensaje(mensajeCliente);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("WaitActivity", "Error al recibir mensaje: " + errorMessage);
                    }
                });

                // Volver a ejecutar después de INTERVALO_CONSULTA
                mensajehandler.postDelayed(this, INTERVALO_CONSULTA);
            }
        };

        // Ejecutar la primera vez
        mensajehandler.post(runnable);
    }

    private void detenerRecepcionMensajes() {
        if (mensajehandler != null && runnable != null) {
            mensajehandler.removeCallbacks(runnable);
        }
    }

    private void mostrarDialogoMensaje(String mensaje) {
        ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
        arrivalMessageDialog.setDriverName(mensaje); // Configura el mensaje a mostrar
        arrivalMessageDialog.setButtonText("Aceptar"); // Configura el texto del botón

        arrivalMessageDialog.setOnConfirmClickListener(() -> {
            // Puedes agregar aquí cualquier acción después de cerrar el diálogo
        });

        arrivalMessageDialog.show(getSupportFragmentManager(), "ArrivalMessageDialog");
    }


    /**
     * Obtiene la foto del conductor y la guarda en TemporaryData.
     */
    private void loadConductorPhoto() {
        String conductorId = temporaryData.getConductorId();

        new PedidoStatusController().fetchConductorPhoto(this, conductorId, new PedidoStatusController.FetchConductorPhotoCallback() {
            @Override
            public void onSuccess(String conductorFoto) {
                // Actualiza la imagen solo cuando la respuesta es exitosa
                CircularImageView profileImage = findViewById(R.id.profile_image_conductor);

                if (conductorFoto != null && !conductorFoto.isEmpty()) {
                    // Usar Glide para cargar la imagen
                    Glide.with(WaitingActivity.this)
                            .load(conductorFoto)
                            .placeholder(R.drawable.img_conductor)
                            .error(R.drawable.img_conductor)
                            .into(profileImage);
                    Log.d("Conductor foto:", "Imagen cargada exitosamente");
                    Log.d("Conductor foto:", "Imagen en el tmeporary" + temporaryData.getConductorFoto());
                } else {
                    // Usa la imagen por defecto
                    profileImage.setImageResource(R.drawable.img_conductor);
                    Log.d("Conductor foto:", "Imagen por defecto cargada");
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Maneja el error
                CircularImageView profileImage = findViewById(R.id.profile_image_conductor);
                profileImage.setImageResource(R.drawable.img_conductor);
                Log.e("Conductor foto:", "Error al obtener la foto: " + errorMessage);
            }
        });
    }

    /**
     * Inicializa el BottomSheet para mostrar la información del viaje.
     */
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

        // Detener actualizaciones de ubicación
        if (locationUpdateHandler != null) {
            locationUpdateHandler.removeCallbacksAndMessages(null);
        }

        // Detener verificaciones de estado del pedido
        if (pedidoStatusHandler != null) {
            pedidoStatusHandler.removeCallbacksAndMessages(null);
        }

        // Desregistrar el BroadcastReceiver si está registrado
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pedidoStatusReceiver);
        } catch (IllegalArgumentException e) {
            // Manejar el caso en que el receiver no esté registrado para evitar excepciones
            Log.w("WaitingActivity", "BroadcastReceiver no estaba registrado: " + e.getMessage());
        }
    }
}