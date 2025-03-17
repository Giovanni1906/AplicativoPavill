package radiotaxipavill.radiotaxipavillapp.view;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.FriendlyAddressHelper;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.FavoriteController;
import radiotaxipavill.radiotaxipavillapp.controller.RequestTaxiController;

import com.google.android.gms.maps.model.LatLng;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ConfirmActivity extends AppCompatActivity {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private String originAddress;
    private String destinationAddress;
    private LoadingDialog loadingDialog;
    private TextView errorText;
    private TextView favText;
    private Button btnRequestTaxi;
    private TemporaryData temporaryData;
    private boolean saveFavorite = false; // Controla si se debe guardar la dirección
    private ImageButton heartButton;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        // dirección traida desde favorito de origen
        EditText editReference = findViewById(R.id.editReference);
        LinearLayout layoutFeedback = findViewById(R.id.LayoutFeedback);
        heartButton = findViewById(R.id.favoriteButton);
        ImageButton btnDeleteOrigin = findViewById(R.id.btnDeleteOrigin);
        favText = findViewById(R.id.favText);

        // Recibir la dirección del origen si está disponible
        if (getIntent().hasExtra("OriginAddress")) {
            String originAddress = getIntent().getStringExtra("OriginAddress");
            if (originAddress != null && !originAddress.isEmpty()) {
                editReference.setText(originAddress);
                layoutFeedback.setBackgroundResource(R.drawable.favorites_box_background); // Restablece el fondo eliminando el color amarillo
                heartButton.setVisibility(View.GONE);
                favText.setVisibility(View.VISIBLE); // Mostrar el texto de favorito (si existe)
            }
        }

        // Configurar el botón para borrar la referencia
        btnDeleteOrigin.setOnClickListener(v -> {
            editReference.setText(""); // Borra el texto del EditText
            layoutFeedback.setBackgroundResource(R.drawable.search_box_background); // Restablece el fondo eliminando el color amarillo
            favText.setVisibility(View.GONE);

        });

        // Acceso a SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", null);

        loadingDialog = new LoadingDialog(this);
        errorText = findViewById(R.id.errorText);
        btnRequestTaxi = findViewById(R.id.btnConfirm);


        // Botón de cancelar
        findViewById(R.id.btnCancel).setOnClickListener(v -> onBackPressed());

        // Obtener las coordenadas directamente desde TemporaryData
        temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);  // 🔹 Restaurar datos guardados


        if (temporaryData.getOriginCoordinates() != null) {
            originCoordinates = temporaryData.getOriginCoordinates();
            destinationCoordinates = temporaryData.getDestinationCoordinates();

            // Usar Geocoder para obtener direcciones amigables
            FriendlyAddressHelper.getFriendlyAddress(this, originCoordinates.latitude, originCoordinates.longitude, new FriendlyAddressHelper.AddressCallback() {
                @Override
                public void onAddressRetrieved(String friendlyAddress) {
                    originAddress = friendlyAddress;
                    updateUI();
                }

                @Override
                public void onError(String errorMessage) {
                    originAddress = "Dirección de origen no disponible";
                    updateUI();
                }
            });

            if (temporaryData.getDestinationCoordinates() != null){
                FriendlyAddressHelper.getFriendlyAddress(this, destinationCoordinates.latitude, destinationCoordinates.longitude, new FriendlyAddressHelper.AddressCallback() {
                    @Override
                    public void onAddressRetrieved(String friendlyAddress) {
                        destinationAddress = friendlyAddress;
                        updateUI();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        destinationAddress = "Dirección de destino no disponible";
                        updateUI();
                    }
                });
            } else{
                // Si el destino es null, se asigna directamente
                destinationAddress = "Destino no marcado en el mapa";
                updateUI();
            }

        } else {
            Toast.makeText(this, "Error: Coordenadas de origen y destino no disponibles.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnRequestTaxi.setOnClickListener(v -> {

            String reference = ((TextView) findViewById(R.id.editReference)).getText().toString().trim();
            if (reference.isEmpty()) {
                Toast.makeText(this, "Ingrese su dirección exacta antes de continuar.", Toast.LENGTH_SHORT).show();
                Log.d("ConfirmActivity", " Error: El campo de referencia está vacío.");
                return;
            }

            if (!btnRequestTaxi.isEnabled()) {
                Log.d("ConfirmActivity", "Botón de solicitud de taxi deshabilitado. Ignorando clic.");
                return;
            }

            btnRequestTaxi.setEnabled(false); // Deshabilita el botón inmediatamente para evitar doble clic
            Log.d("ConfirmActivity", "🟢 Botón de solicitud deshabilitado para evitar dobles clics.");

            loadingDialog.show();

            double destinationLat;
            double destinationLng;

            if (destinationCoordinates != null){
                destinationLat = destinationCoordinates.latitude;
                destinationLng = destinationCoordinates.longitude;
            }
            else{
                destinationLat = 00.00;
                destinationLng = 00.00;
            }

            new RequestTaxiController().requestTaxi(
                    this,
                    reference, // el "reference" vendria a ser la dirección exacta
                    destinationAddress,
                    originCoordinates.latitude,
                    originCoordinates.longitude,
                    destinationLat,
                    destinationLng,
                    originAddress, // el "originAddress" se desplazaría a ser la referencia
                    saveFavorite,
                    new RequestTaxiController.RequestTaxiCallback() {
                        @Override
                        public void onSuccess(String message) {
                            loadingDialog.dismiss();

                            // Registrar el tiempo del pedido
                            long requestTime = System.currentTimeMillis();
                            temporaryData.setRequestTime(requestTime, ConfirmActivity.this);

                            // Redirigir a SearchingActivity
                            Intent intent = new Intent(ConfirmActivity.this, SearchingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            loadingDialog.dismiss();
                            if (isDestroyed() || isFinishing()) {
                                Log.e("ConfirmActivity", "La actividad ya no está activa");
                                return;
                            }

                            if (errorText != null) {
                                errorText.setVisibility(View.VISIBLE);
                                errorText.setText(errorMessage);
                            } else {
                                Log.e("ConfirmActivity", "TextView errorText es null");
                            }

                            habilitarBotonConRetraso(); // 🔹 Rehabilita el botón después del error

                        }
                    }
            );
        });

        // Validar que clienteId esté disponible
        if (clienteId == null) {
            Toast.makeText(this, "Problemas internos con su cuenta, vuelva a iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configurar el botón de favoritos
        setupFavoriteButton(clienteId);
    }

    /**
     * Método para reactivar el botón después de un tiempo prudente
     */
    private void habilitarBotonConRetraso() {
        new android.os.Handler().postDelayed(() -> {
            btnRequestTaxi.setEnabled(true);
            Log.d("ConfirmActivity", "🔄 Botón de solicitud de taxi reactivado.");
        }, 5000); // Espera 5 segundos antes de reactivar el botón
    }


    /**
     * Ingresa la coordenada de destino a favoritos
     * @param clienteId
     */

    private void setupFavoriteButton(String clienteId) {
        LatLng originCoordinates = temporaryData.getOriginCoordinates();

        heartButton = findViewById(R.id.favoriteButton);
        heartButton.setOnClickListener(v -> {
            if (originCoordinates != null) {
                // Activar el booleano para indicar que se guardará al finalizar el recorrido
                saveFavorite = true;

                // Mostrar el mensaje de confirmación
                Toast.makeText(this, "La dirección se guardará al finalizar el recorrido del taxi", Toast.LENGTH_SHORT).show();

                // Aquí puedes hacer que la lógica de guardado real se ejecute solo al finalizar el recorrido
            } else {
                Toast.makeText(this, "No se pudo añadir el destino a favoritos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Actualiza los text view y demás comnponentes UI
     */
    @SuppressLint("SetTextI18n")
    private void updateUI() {
        TextView originTextView = findViewById(R.id.textOrigin);
        TextView destinationTextView = findViewById(R.id.textDestination);
        TextView estimatedCostTextView = findViewById(R.id.estimatedCost);
        TextView withoutCostTextView = findViewById(R.id.withoutCost);
        TextView withoutDestTextView = findViewById(R.id.withoutDest);


        originTextView.setText(originAddress);
        destinationTextView.setText(destinationAddress);

        // Mostrar el costo estimado desde TemporaryData
        String estimatedCost = temporaryData.getEstimatedCost();
        Log.d("ConfirmActivity", "Estimated Cost: " + estimatedCost);
        if (estimatedCost != null && !estimatedCost.isEmpty() && !estimatedCost.equals("N/A")  ) {
            Log.d("ConfirmActivity", "Estimated Cost: " + estimatedCost);
            float estimatedCostFloat = Float.parseFloat(estimatedCost);
            if (estimatedCostFloat > 0.0f) {
                Log.d("ConfirmActivity", "Estimated Cost: " + estimatedCostFloat);
                estimatedCostTextView.setText("s/" + estimatedCost);
                estimatedCostTextView.setVisibility(View.VISIBLE);
                withoutCostTextView.setVisibility(View.GONE);
                withoutDestTextView.setVisibility(View.GONE);
            }else {
                Log.d("ConfirmActivity", "Destino marcado: " + destinationCoordinates);
                if(destinationCoordinates != null) {
                    estimatedCostTextView.setVisibility(View.GONE);
                    withoutCostTextView.setVisibility(View.VISIBLE);
                    withoutDestTextView.setVisibility(View.GONE);
                }else{
                    estimatedCostTextView.setVisibility(View.GONE);
                    withoutCostTextView.setVisibility(View.GONE);
                    withoutDestTextView.setVisibility(View.VISIBLE);
                }
            }
        } else {
            estimatedCostTextView.setVisibility(View.GONE);
            withoutCostTextView.setVisibility(View.GONE);
            withoutDestTextView.setVisibility(View.VISIBLE);
        }
    }
}

