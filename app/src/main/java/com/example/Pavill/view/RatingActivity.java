package com.example.Pavill.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.Pavill.R;
import com.example.Pavill.components.CircularImageView;
import com.example.Pavill.components.DeviceIdentifier;
import com.example.Pavill.components.PedidoCancellationHelper;
import com.example.Pavill.components.PedidoServiceHelper;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.CalificarFinalizarPedidoController;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

public class RatingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "RatingActivity";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private int currentRating = 0; // Variable global para la calificación
    private double currentLatitude = 0.0; // Variable global para la latitud
    private double currentLongitude = 0.0; // Variable global para la longitud

    private String conductorFoto;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        // Inicializar TemporaryData
        temporaryData = TemporaryData.getInstance();

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        circularImage();

        // Configuración de las estrellas para la calificación
        setupStarRating();

        // Configurar el botón de envío de calificación
        setupSubmitButton();
    }

    private void circularImage(){
        // Actualiza la imagen solo cuando la respuesta es exitosa
        CircularImageView profileImage = findViewById(R.id.driverImage);

        conductorFoto = temporaryData.getConductorFoto();

        if (conductorFoto != null && !conductorFoto.isEmpty()) {
            // Usar Glide para cargar la imagen
            Glide.with(this)
                    .load(conductorFoto)
                    .placeholder(R.drawable.img_conductor)
                    .error(R.drawable.img_conductor)
                    .into(profileImage);
            Log.d("Conductor foto:", "Imagen cargada exitosamente");
        } else {
            // Usa la imagen por defecto
            profileImage.setImageResource(R.drawable.img_conductor);
            Log.d("Conductor foto:", "Imagen por defecto cargada");
        }
    }

    /**
     * Configurar las estrellas para la calificación
     */
    private void setupStarRating() {
        ImageView[] stars = new ImageView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                currentRating = index; // Actualizar la calificación global


                // Cambiar el color de las estrellas seleccionadas a amarillo
                for (int j = 0; j <= index; j++) {
                    stars[j].setColorFilter(ContextCompat.getColor(this, com.google.android.libraries.places.R.color.quantum_yellow));
                }

                // Cambiar el color de las estrellas no seleccionadas a gris
                for (int j = index + 1; j < stars.length; j++) {
                    stars[j].setColorFilter(ContextCompat.getColor(this, com.google.android.libraries.places.R.color.quantum_grey));
                }
            });
        }
    }

    /**
     * Configurar el botón de envío de calificación.
     */
    private void setupSubmitButton() {
        findViewById(R.id.btn_submit_rating).setOnClickListener(v -> {
            // Obtener el comentario ingresado por el usuario
            EditText commentEditText = findViewById(R.id.editTextFeedback);
            String comment = commentEditText.getText().toString();

            if (currentRating == 0) {
                Toast.makeText(this, "Por favor selecciona una calificación.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (comment.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa un comentario.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el identificador del dispositivo
            String deviceIdentifier = DeviceIdentifier.getUniqueIdentifier(this);

            // Obtener el PedidoId desde TemporaryData
            String pedidoId = TemporaryData.getInstance().getPedidoId();

            if (pedidoId == null || pedidoId.isEmpty()) {
                Toast.makeText(this, "No se encontró el PedidoId.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamar a la función para enviar los datos
            sendCalification(pedidoId, currentRating, comment, currentLatitude, currentLongitude);
        });
    }

    /**
     * Enviar la calificación y manejar las respuestas del servidor.
     */
    private void sendCalification(String pedidoId, int calificacion, String comentario, double lat, double lng) {
        new CalificarFinalizarPedidoController().calificarFinalizarPedido(
                this,
                pedidoId,
                calificacion,
                comentario,
                lat,
                lng,
                new CalificarFinalizarPedidoController.CalificarPedidoCallback() {
                    @Override
                    public void onSuccess(String message, JSONObject response) {
                        Log.d("RatingActivity", "Calificación enviada exitosamente: " + message);
                        Toast.makeText(RatingActivity.this, "Gracias por tu calificación.", Toast.LENGTH_SHORT).show();
                        calificationProcess();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("RatingActivity", "Error al calificar: " + errorMessage);
                        Toast.makeText(RatingActivity.this, "Error al enviar la calificación: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Proceso para finalizar e ir a MapActivity
     */
    private void calificationProcess(){
            //Marcar viaje finalizado
            PedidoServiceHelper.updateSubPedidoState(this, "Finalizado");
            TemporaryData.getInstance().clearData();
            // Detener el servicio PedidoStatusService
            PedidoServiceHelper.stopPedidoStatusService(this);

            // Después de finalizar el viaje, redirigir a la MainActivity
            Log.d(TAG, "going to mapactivity");
            Intent intent = new Intent(this, MainActivity.class);
            // Establecer flags para limpiar la pila de actividades y evitar retroceso
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.startActivity(intent);
            // Finalizar la actividad actual para asegurar que no se pueda volver atrás
            finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        applyMapStyle();

        // Verificar permisos de ubicación y habilitar la funcionalidad del mapa
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            // Solicitar permisos de ubicación
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
     * Obtener la ubicación actual del usuario
     */
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Mover la cámara a la ubicación actual
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    Log.d(TAG, "Ubicación obtenida: Lat=" + currentLatitude + ", Lng=" + currentLongitude);
                } else {
                    Toast.makeText(RatingActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Manejo del resultado de la solicitud de permisos
     * @param requestCode The request code passed in
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}