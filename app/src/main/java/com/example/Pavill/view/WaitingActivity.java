package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.Pavill.R;
import com.example.Pavill.components.CircularImageView;
import com.example.Pavill.components.DistanceUtils;
import com.example.Pavill.components.PedidoCancellationHelper;
import com.example.Pavill.components.PedidoServiceHelper;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.components.BitmapUtils;
import com.example.Pavill.controller.CancelRequestController;
import com.example.Pavill.controller.DriverLocationController;
import com.example.Pavill.controller.PedidoController;
import com.example.Pavill.controller.PedidoStatusController;
import com.example.Pavill.controller.RecibirMensajeController;
import com.example.Pavill.controller.RouteController;
import com.example.Pavill.receiver.PedidoStatusReceiver;
import com.example.Pavill.services.PedidoStatusService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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


    private PedidoStatusReceiver pedidoStatusReceiver;
    // Variable global para indicar si está en tardanza (1 = tardanza, 0 = no tardanza)
    private int tardanzaFlag = 0;

    private Handler pedidoStatusHandler = new Handler(); // Nuevo handler para verificar el estado del pedido

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

        // Iniciar actualizaciones de ubicación del conductor
        startFetchingDriverLocation();

        loadConductorPhoto();

        // Inicia el servicio para verificar el estado del pedido
        PedidoServiceHelper.startPedidoStatusService(this);

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
        errorText = findViewById(R.id.errorText);

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
            return input;
        }

        StringBuilder capitalized = new StringBuilder();
        String[] words = input.toLowerCase().split("\\s+"); // Convertir todo a minúscula y dividir por espacios

        for (String word : words) {
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0))) // Primera letra en mayúscula
                        .append(word.substring(1)) // Resto en minúscula
                        .append(" "); // Agregar espacio
            }
        }

        return capitalized.toString().trim(); // Eliminar el espacio extra al final
    }


    /**
     * Inicializa los botones de llamada, whatsapp y de cancelar búsqueda
     */
    public void initializeBottomsUI(){
        //Botón de llamada
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> {
            if (conductorTelefono != null && !conductorTelefono.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + conductorTelefono));
                startActivity(intent);
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
            new CancelRequestController().cancelRequest(this, new CancelRequestController.CancelRequestCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(WaitingActivity.this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show();

                    // limpiar TemporaryData
                    temporaryData = TemporaryData.getInstance();
                    temporaryData.clearData();

                    // Crear un intent para regresar al MapActivity
                    Intent intent = new Intent(WaitingActivity.this, CancelReasonActivity.class);
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
        //Botón de continuar
        findViewById(R.id.btnOnBoard).setOnClickListener(v -> checkAndProceedToProgress());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        applyMapStyle();


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
        locationUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Llamar al controlador para obtener la ubicación del conductor
                new DriverLocationController().fetchDriverLocation(WaitingActivity.this, new DriverLocationController.DriverLocationCallback() {
                    @Override
                    public void onLocationReceived(double lat, double lng, String orientation, int estimatedTimeMinutes) {
                        LatLng driverLocation = new LatLng(lat, lng);

                        // Actualizar el marcador del conductor
                        int iconSize = 32; // Tamaño deseado para el ícono
                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverLocation)
                                    .icon(BitmapUtils.getProportionalBitmap(WaitingActivity.this, R.drawable.ic_car, iconSize))
                                    .title("Conductor en camino"));
                        } else {
                            animateMarkerTo(driverMarker, driverLocation);
                        }

                        // Calcular el tiempo estimado de llegada solo la primera vez
                        if (initialEstimatedTime == -1) {
                            initialEstimatedTime = calculateEstimatedTimeToOrigin(driverLocation, originCoordinates);
                            Log.d("Tiempo inicial:", "Tiempo estimado inicial: " + initialEstimatedTime + " minutos");

                            // Mostrar el tiempo inicial
                            TextView estimatedTimeView = findViewById(R.id.textViewETA);
                            estimatedTimeView.setText("Tiempo estimado: " + initialEstimatedTime + " minutos");

                            // Iniciar el contador solo una vez
                            if (!isCountdownStarted) {
                                isCountdownStarted = true;
                                startEstimatedTimeCountdown(initialEstimatedTime);
                            }
                        }
                        // Verificar si el conductor está cerca del origen
                        checkProximityToOrigin(driverLocation);

                    }

                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                        // Reprogramar el ciclo incluso en caso de error
                    }
                });
                // Reprogramar el siguiente ciclo
                locationUpdateHandler.postDelayed(this, 2000);
            }
        }, 5000);
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
                    // Actualiza el texto con el tiempo restante
                    estimatedTimeView.setText("Tiempo estimado: " + remainingTime + " minutos");
                    Log.d("Tiempo estimado:", "Tiempo restante: " + remainingTime + " minutos");
                    remainingTime--;

                    // Repite el Runnable cada minuto (1000ms x 60)
                    countdownHandler.postDelayed(this, 60 * 1000);
                } else {
                    // Cuando el tiempo llega a 0
                    estimatedTimeView.setTextColor(ContextCompat.getColor(WaitingActivity.this, R.color.alertColor));

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
        errorText.setText(errorMessage);
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Anima el marcador del conductor para que simule avanzar
     * @param marker
     * @param toPosition
     */
    private void animateMarkerTo(final Marker marker, final LatLng toPosition) {
        final LatLng fromPosition = marker.getPosition();
        final long duration = 2000;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo mensaje")
                .setMessage(mensaje)
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Obtiene la foto del conductor y la guarda en TemporaryData.
     */
    private void loadConductorPhoto() {
        String conductorId = TemporaryData.getInstance().getConductorId();

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