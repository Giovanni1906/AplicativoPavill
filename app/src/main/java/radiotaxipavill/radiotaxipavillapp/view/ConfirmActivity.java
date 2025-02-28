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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private Button btnRequestTaxi;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

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


        if (temporaryData.getOriginCoordinates() != null && temporaryData.getDestinationCoordinates() != null) {
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

            new RequestTaxiController().requestTaxi(
                    this,
                    reference, // el "reference" vendria a ser la dirección exacta
                    destinationAddress,
                    originCoordinates.latitude,
                    originCoordinates.longitude,
                    destinationCoordinates.latitude,
                    destinationCoordinates.longitude,
                    originAddress, // el "originAddress" se desplazaría a ser la referencia
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
        LatLng destinationCoordinates = temporaryData.getDestinationCoordinates();
        String destinationAddress = this.destinationAddress != null ? this.destinationAddress : "Destino no disponible";

        ImageView heartButton = findViewById(R.id.favoriteButton);
        heartButton.setOnClickListener(v -> {
            if (destinationCoordinates != null && destinationAddress != null) {
                new FavoriteController().addFavoriteDestination(
                        this,
                        clienteId,
                        destinationAddress,
                        destinationCoordinates.latitude,
                        destinationCoordinates.longitude
                );
                Toast.makeText(this, "Espere un momento...", Toast.LENGTH_SHORT).show();
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

        originTextView.setText(originAddress);
        destinationTextView.setText(destinationAddress);

        // Mostrar el costo estimado desde TemporaryData
        String estimatedCost = temporaryData.getEstimatedCost();
        if (estimatedCost != null && !estimatedCost.isEmpty() && !estimatedCost.equals("N/A")  ) {
            Log.d("ConfirmActivity", "Estimated Cost: " + estimatedCost);
            float estimatedCostFloat = Float.parseFloat(estimatedCost);
            if (estimatedCostFloat > 0.0f) {
                Log.d("ConfirmActivity", "Estimated Cost: " + estimatedCostFloat);
                estimatedCostTextView.setText("s/" + estimatedCost);
                estimatedCostTextView.setVisibility(View.VISIBLE);
                withoutCostTextView.setVisibility(View.GONE);
            }else {
                estimatedCostTextView.setVisibility(View.GONE);
                withoutCostTextView.setVisibility(View.VISIBLE);
            }

        } else {
            estimatedCostTextView.setVisibility(View.GONE);
            withoutCostTextView.setVisibility(View.VISIBLE);
        }
    }
}

