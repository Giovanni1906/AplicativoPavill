package com.example.Pavill.view;

import com.example.Pavill.R;
import com.example.Pavill.components.FriendlyAddressHelper;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.FavoriteController;
import com.example.Pavill.controller.RequestTaxiController;
import com.google.android.gms.maps.model.LatLng;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmActivity extends AppCompatActivity {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private String originAddress;
    private String destinationAddress;
    private LoadingDialog loadingDialog;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        // Acceso a SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", null);

        loadingDialog = new LoadingDialog(this);
        errorText = findViewById(R.id.errorText);

        // Botón de cancelar
        findViewById(R.id.btnCancel).setOnClickListener(v -> onBackPressed());

        // Obtener las coordenadas directamente desde TemporaryData
        TemporaryData tempData = TemporaryData.getInstance();

        if (tempData.getOriginCoordinates() != null && tempData.getDestinationCoordinates() != null) {
            originCoordinates = tempData.getOriginCoordinates();
            destinationCoordinates = tempData.getDestinationCoordinates();

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

        Button btnRequestTaxi = findViewById(R.id.btnConfirm);
        btnRequestTaxi.setOnClickListener(v -> {
            String reference = ((TextView) findViewById(R.id.editReference)).getText().toString().trim();

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
                            TemporaryData.getInstance().setRequestTime(requestTime);

                            // Redirigir a SearchingActivity sin enviar datos
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
                        }
                    }
            );
        });

        // Validar que clienteId esté disponible
        if (clienteId == null) {
            Toast.makeText(this, "ClienteId no encontrado en SharedPreferences", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configurar el botón de favoritos
        setupFavoriteButton(clienteId);
    }

    private void setupFavoriteButton(String clienteId) {
        TemporaryData tempData = TemporaryData.getInstance();
        LatLng destinationCoordinates = tempData.getDestinationCoordinates();
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

    private void updateUI() {
        TextView originTextView = findViewById(R.id.textOrigin);
        TextView destinationTextView = findViewById(R.id.textDestination);
        TextView estimatedCostTextView = findViewById(R.id.estimatedCost);

        originTextView.setText(originAddress);
        destinationTextView.setText(destinationAddress);

        // Mostrar el costo estimado desde TemporaryData
        TemporaryData temporaryData = TemporaryData.getInstance();
        String estimatedCost = temporaryData.getEstimatedCost();
        if (estimatedCost != null && !estimatedCost.isEmpty()) {
            estimatedCostTextView.setText(estimatedCost);
        } else {
            estimatedCostTextView.setText("s/ xx.xx");
        }
    }
}

