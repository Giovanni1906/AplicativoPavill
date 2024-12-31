package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.example.Pavill.R;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.CancelRequestController;
import com.example.Pavill.controller.PedidoStatusController;
import com.example.Pavill.controller.PublicidadController;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.bumptech.glide.Glide;

public class SearchingActivity extends AppCompatActivity {

    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;
    private TextView textViewTimer;
    private Handler timerHandler = new Handler();
    private long startTime;
    private boolean isCancelled = false;
    private LoadingDialog loadingDialog;
    private Runnable pedidoStatusChecker;

    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        loadingDialog = new LoadingDialog(this);
        initializeUI();
        startTimer();
        startCheckingPedidoStatus();

        // Cargar la publicidad
        loadPublicidad();
    }

    private void initializeUI() {
        textViewTimer = findViewById(R.id.textViewTimer);

        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);
        btnCancelSearch.setOnClickListener(v -> cancelSearch());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
             originLat = extras.getDouble("origin_lat", 0.0);
             originLng = extras.getDouble("origin_lng", 0.0);
             destinationLat = extras.getDouble("destination_lat", 0.0);
             destinationLng = extras.getDouble("destination_lng", 0.0);
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCancelled) {
                updateTimer();
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimer() {
        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        textViewTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void startCheckingPedidoStatus() {
        final Handler handler = new Handler();

        // Define el Runnable
        pedidoStatusChecker = new Runnable() {
            @Override
            public void run() {
                if (!isCancelled) {
                    new PedidoStatusController().checkPedidoStatus(SearchingActivity.this, new PedidoStatusController.PedidoStatusCallback() {
                        @Override
                        public void onStatusReceived(String status, String message) {
                            switch (status) {
                                case "P005": // Pedido aprobado
                                    navigateToWaitingActivity();
                                    handler.removeCallbacksAndMessages(null); // Detiene el ciclo de verificación
                                    break;

                                case "CANCELADO": // Pedido cancelado
                                    isCancelled = true;
                                    timerHandler.removeCallbacks(timerRunnable); // Detiene el cronómetro
                                    handler.removeCallbacksAndMessages(null); // Detiene el ciclo de verificación

                                    // limpiar TemporaryData
                                    temporaryData = TemporaryData.getInstance();
                                    temporaryData.clearData();

                                    // Crear un intent para regresar al MapActivity
                                    Intent intent = new Intent(SearchingActivity.this, MapActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                                    startActivity(intent);
                                    finish(); // Finalizar

                                    showCancelledMessage(); // Muestra un mensaje de cancelación
                                    break;

                                case "EN_ESPERA": // Pedido en espera
                                    showSearchingMessage();
                                    handler.postDelayed(pedidoStatusChecker, 3000); // Reintentar después de 3 segundos
                                    break;

                                default: // Respuesta inesperada
                                    isCancelled = true;
                                    timerHandler.removeCallbacks(timerRunnable); // Detiene el cronómetro
                                    handler.removeCallbacksAndMessages(null); // Detiene el ciclo de verificación
                                    showCancelledMessage(); // Muestra un mensaje de cancelación
                                    break;
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Maneja errores pero continúa intentando
                            handler.postDelayed(pedidoStatusChecker, 3000);
                        }
                    });
                }
            }
        };

        // Inicia el primer chequeo después de 3 segundos
        handler.postDelayed(pedidoStatusChecker, 3000);
    }


    private void cancelSearch() {
        isCancelled = true;
        timerHandler.removeCallbacks(timerRunnable);

        loadingDialog.show();
        new CancelRequestController().cancelRequest(this, new CancelRequestController.CancelRequestCallback() {
            @Override
            public void onSuccess(String message) {
                loadingDialog.dismiss();
                Toast.makeText(SearchingActivity.this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show();

                // limpiar TemporaryData
                temporaryData = TemporaryData.getInstance();
                temporaryData.clearData();

                // Crear un intent para regresar al MapActivity
                Intent intent = new Intent(SearchingActivity.this, MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
                startActivity(intent);
                finish(); // Finalizar
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(SearchingActivity.this, "Error al cancelar el pedido.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToWaitingActivity() {
        if (!isCancelled) {
            isCancelled = true;
            timerHandler.removeCallbacks(timerRunnable);

            Intent intent = new Intent(SearchingActivity.this, WaitingActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showCancelledMessage() {
        runOnUiThread(() -> {
            Toast.makeText(this, "El pedido fue cancelado", Toast.LENGTH_LONG).show();
        });
    }

    private void showSearchingMessage() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Seguimos buscando...", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar búsqueda")
                .setMessage("¿Estás seguro de que deseas cancelar la búsqueda?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    cancelSearch();
                    super.onBackPressed(); // Llama al comportamiento predeterminado
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void loadPublicidad() {
        ImageView adImageView = findViewById(R.id.ad_image);

        new PublicidadController().fetchPublicidad(this, new PublicidadController.PublicidadCallback() {
            @Override
            public void onPublicidadReceived(String imageUrl) {
                Glide.with(SearchingActivity.this)
                        .load(imageUrl)
                        .transform(new FitCenter()) // Ajustar la imagen
                        .into(adImageView);
            }

            @Override
            public void onNoPublicidadFound(String message) {
                adImageView.setImageResource(R.drawable.sample_ad_image); // Imagen predeterminada si no hay publicidad
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(SearchingActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                adImageView.setImageResource(R.drawable.sample_ad_image); // Imagen predeterminada en caso de error
            }
        });
    }


}