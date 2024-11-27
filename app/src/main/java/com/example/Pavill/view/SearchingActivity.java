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
import com.example.Pavill.controller.CancelRequestController;
import com.example.Pavill.controller.PedidoStatusController;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        loadingDialog = new LoadingDialog(this);
        initializeUI();
        startTimer();
        startCheckingPedidoStatus();
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
        pedidoStatusChecker = new Runnable() {
            @Override
            public void run() {
                if (!isCancelled) {
                    new PedidoStatusController().checkPedidoStatus(SearchingActivity.this, new PedidoStatusController.PedidoStatusCallback() {
                        @Override
                        public void onStatusReceived(String status, String message) {
                            switch (status) {
                                case "P005": // Pedido aceptado
                                    navigateToWaitingActivity();
                                    handler.removeCallbacksAndMessages(null);
                                    break;

                                case "P006": // Pedido cancelado
                                    isCancelled = true;
                                    timerHandler.removeCallbacks(timerRunnable);
                                    handler.removeCallbacksAndMessages(null);
                                    showCancelledMessage();
                                    break;

                                default: // Continuar verificando
                                    handler.postDelayed(pedidoStatusChecker, 3000);
                                    break;
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            handler.postDelayed(pedidoStatusChecker, 3000);
                        }
                    });
                }
            }
        };
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
            intent.putExtra("origin_lat", originLat);
            intent.putExtra("origin_lng", originLng);
            intent.putExtra("destination_lat", destinationLat);
            intent.putExtra("destination_lng", destinationLng);
            startActivity(intent);
            finish();
        }
    }

    private void showCancelledMessage() {
        runOnUiThread(() -> {
            Toast.makeText(this, "El pedido fue cancelado.", Toast.LENGTH_LONG).show();
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

}
