package com.example.Pavill.view;

import com.example.Pavill.R;
import com.example.Pavill.components.FriendlyAddressHelper;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.RequestTaxiController;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ConfirmActivity extends AppCompatActivity {

    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private String originAddress;
    private String  destinationAddress;
    private LoadingDialog loadingDialog;
    private TextView errorText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        loadingDialog = new LoadingDialog(this);

        // Inicializar errorText después de setContentView
        errorText = findViewById(R.id.errorText);

        // Botón de cancelar
        findViewById(R.id.btnCancel).setOnClickListener(v -> onBackPressed());

        // Obtener las coordenadas de origen y destino desde el Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Obtener las coordenadas pasadas desde MapActivity
            double originLat = extras.getDouble("origin_lat", 0.0);
            double originLng = extras.getDouble("origin_lng", 0.0);
            double destinationLat = extras.getDouble("destination_lat", 0.0);
            double destinationLng = extras.getDouble("destination_lng", 0.0);

            // Crear los objetos LatLng para las coordenadas
            originCoordinates = new LatLng(originLat, originLng);
            destinationCoordinates = new LatLng(destinationLat, destinationLng);

            // Usar Geocoder para obtener direcciones amigables
            FriendlyAddressHelper.getFriendlyAddress(this, originLat, originLng, new FriendlyAddressHelper.AddressCallback() {
                @Override
                public void onAddressRetrieved(String friendlyAddress) {
                    // Asigna la dirección de origen cuando esté disponible
                    originAddress = friendlyAddress;
                    updateUI(); // Método para actualizar la interfaz si necesitas mostrar ambas direcciones
                }

                @Override
                public void onError(String errorMessage) {
                    // Manejo de error para la dirección de origen
                    originAddress = "Dirección de origen no disponible";
                    updateUI(); // Método para manejar errores y continuar con la interfaz
                }
            });

            FriendlyAddressHelper.getFriendlyAddress(this, destinationLat, destinationLng, new FriendlyAddressHelper.AddressCallback() {
                @Override
                public void onAddressRetrieved(String friendlyAddress) {
                    // Asigna la dirección de destino cuando esté disponible
                    destinationAddress = friendlyAddress;
                    updateUI(); // Método para actualizar la interfaz si necesitas mostrar ambas direcciones
                }

                @Override
                public void onError(String errorMessage) {
                    // Manejo de error para la dirección de destino
                    destinationAddress = "Dirección de destino no disponible";
                    updateUI(); // Método para manejar errores y continuar con la interfaz
                }
            });
        }

        Button btnRequestTaxi = findViewById(R.id.btnConfirm);
        btnRequestTaxi.setOnClickListener(v -> {
            String reference = ((TextView) findViewById(R.id.editReference)).getText().toString().trim();

            loadingDialog.show();

            new RequestTaxiController().requestTaxi(
                    this,
                    originAddress,
                    destinationAddress,
                    originCoordinates.latitude,
                    originCoordinates.longitude,
                    destinationCoordinates.latitude,
                    destinationCoordinates.longitude,
                    reference,
                    new RequestTaxiController.RequestTaxiCallback() {
                        @Override
                        public void onSuccess(String message) {
                            loadingDialog.dismiss();

                            // Acceder a los datos temporales
                            TemporaryData temporaryData = TemporaryData.getInstance();
                            String pedidoId = temporaryData.getPedidoId();

                            // Registrar el tiempo del pedido
                            long requestTime = System.currentTimeMillis(); // Guardar la hora actual en milisegundos
                            temporaryData.setRequestTime(requestTime); // Establecer el tiempo en TemporaryData

                            // Muestra el ID del pedido (opcional, para pruebas)
                            System.out.println("Pedido registrado con ID: " + pedidoId);

                            // Redirigir a SearchingActivity
                            Intent intent = new Intent(ConfirmActivity.this, SearchingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("origin_lat", originCoordinates.latitude);
                            intent.putExtra("origin_lng", originCoordinates.longitude);
                            intent.putExtra("destination_lat", destinationCoordinates.latitude);
                            intent.putExtra("destination_lng", destinationCoordinates.longitude);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
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


    }

    private void updateUI() {
        TextView originTextView = findViewById(R.id.textOrigin);
        TextView destinationTextView = findViewById(R.id.textDestination);

        originTextView.setText(originAddress);
        destinationTextView.setText(destinationAddress);
    }
}
