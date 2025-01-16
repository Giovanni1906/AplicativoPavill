package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.example.Pavill.services.PedidoStatusService;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.bumptech.glide.Glide;

public class SearchingActivity extends AppCompatActivity {

    private TextView textViewTimer;
    private Handler timerHandler = new Handler();
    private long startTime;
    private boolean isCancelled = false;
    private LoadingDialog loadingDialog;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        loadingDialog = new LoadingDialog(this);
        initializeUI();
        startTimer();

        startPedidoStatusService();
    }

    /**
     * Inicia el servicio PedidoStatusService.
     */
    private void startPedidoStatusService() {
        Intent serviceIntent = new Intent(this, PedidoStatusService.class);
        startService(serviceIntent);
    }

    /**
     * Inicializa la interfaz de usuario.
     */
    private void initializeUI() {
        textViewTimer = findViewById(R.id.textViewTimer);
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);
        btnCancelSearch.setOnClickListener(v -> cancelSearch());
        loadGifLoader();
        // Cargar la publicidad
        loadPublicidad();
    }

    /**
     * Inicia el temporizador.
     */
    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
    }

    /**
     * Runnable para actualizar el temporizador.
     */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCancelled) {
                updateTimer();
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Actualiza el temporizador en la interfaz de usuario.
     */
    private void updateTimer() {
        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        textViewTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    /**
     * Receptor de cambios de estado del pedido.
     */
    private BroadcastReceiver pedidoStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");

            switch (status) {
                case "ACEPTADO": // Pedido aprobado
                    navigateToWaitingActivity();
                    break;

                case "CANCELADO": // Pedido cancelado
                    cancelProcess();
                    break;

                default:
                    Toast.makeText(SearchingActivity.this, "Estado desconocido", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * Registra el receptor de cambios de estado del pedido.
     */
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.Pavill.PEDIDO_STATUS_UPDATE");
        registerReceiver(pedidoStatusReceiver, filter);
    }

    /**
     * Desregistra el receptor de cambios de estado del pedido.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(pedidoStatusReceiver);
    }

    /**
     * Operación de cancelar pedido manual.
     */
    private void cancelSearch() {
        loadingDialog.show();
        new CancelRequestController().cancelRequest(this, new CancelRequestController.CancelRequestCallback() {
            @Override
            public void onSuccess(String message) {
                cancelProcess();
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(SearchingActivity.this, "Error al cancelar el pedido.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelProcess() {
        isCancelled = true;
        timerHandler.removeCallbacks(timerRunnable);
        loadingDialog.dismiss();
        showCancelledMessage();
        // limpiar TemporaryData
        temporaryData = TemporaryData.getInstance();
        temporaryData.clearData();

        // Crear un intent para regresar al MapActivity
        Intent intent = new Intent(SearchingActivity.this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpia la pila de actividades
        startActivity(intent);
        finish(); // Finalizar
    }

    /**
     * Navega a la actividad WaitingActivity.
     */
    private void navigateToWaitingActivity() {
        if (!isCancelled) {
            isCancelled = true;
            timerHandler.removeCallbacks(timerRunnable);

            Intent intent = new Intent(SearchingActivity.this, WaitingActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Muestra un mensaje de cancelación.
     */
    private void showCancelledMessage() {
        runOnUiThread(() -> {
            Toast.makeText(this, "El pedido fue cancelado", Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Maneja el botón de retroceso.
     */
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

    /**
     * Carga la publicidad de la aplicación.
     */
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

    /**
     * Carga un GIF como un loader.
     */
    private void loadGifLoader() {
        ImageView gifLoader = findViewById(R.id.gifLoader);
        Glide.with(this)
                .asGif() // Especifica que quieres cargar un GIF
                .load(R.drawable.loading) // Reemplaza con el recurso de tu GIF
                .into(gifLoader);
    }

}