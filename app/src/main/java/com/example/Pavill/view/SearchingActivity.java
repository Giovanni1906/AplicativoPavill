package com.example.Pavill.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.example.Pavill.R;
import com.example.Pavill.components.LoadingDialog;
import com.example.Pavill.components.PedidoCancellationHelper;
import com.example.Pavill.components.PedidoServiceHelper;
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
        startTimer(); // Iniciar el temporizador

        // Iniciar el servicio de seguimiento de pedidos
        PedidoServiceHelper.startPedidoStatusService(this);

        // Solicitar permiso de notificación
        requestNotificationPermission();
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
            Log.d("SearchingActivity", "estado." + status);
            switch (status) {
                case "ACEPTADO": // Pedido aprobado
                    navigateToWaitingActivity();
                    Log.d("SearchingActivity", "Pedido aceptado.");
                    break;

                case "CANCELADO": // Pedido cancelado
                    isCancelled = true;
                    timerHandler.removeCallbacks(timerRunnable);
                    loadingDialog.dismiss();
                    PedidoCancellationHelper.cancelProcess(SearchingActivity.this);
                    Log.d("SearchingActivity", "Pedido cancelado.");
                    break;

                default:
                    Log.d("SearchingActivity", "Desconocido o en espera.");
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
                isCancelled = true;
                timerHandler.removeCallbacks(timerRunnable);
                loadingDialog.dismiss();
                PedidoCancellationHelper.cancelProcess(SearchingActivity.this);

            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(SearchingActivity.this, "Error al cancelar el pedido.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Navega a la actividad WaitingActivity.
     */
    private void navigateToWaitingActivity() {
        if (!isCancelled) {
            isCancelled = true;
            timerHandler.removeCallbacks(timerRunnable);
            Log.d("SearchingActivity", "intentando dirigir a waiting");
            Intent intent = new Intent(SearchingActivity.this, WaitingActivity.class);
            startActivity(intent);
            Log.d("SearchingActivity", "dirigiendose a waiting");
            finish();
        }
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
                        Log.d("SearchingActivity", "Publicidad cargada." + imageUrl);
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

    /**
     * Maneja la respuesta de la solicitud de permisos.
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

        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso de notificaciones otorgado
                        Log.d("Permission", "Permiso POST_NOTIFICATIONS otorgado");
                    } else {
                        // Permiso de notificaciones denegado
                        //Toast.makeText(this, "Permiso para publicar notificaciones denegado.", Toast.LENGTH_SHORT).show();
                    }
                } else if (permission.equals(Manifest.permission.FOREGROUND_SERVICE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso FOREGROUND_SERVICE otorgado
                        Log.d("Permission", "Permiso FOREGROUND_SERVICE otorgado");
                    } else {
                        // Permiso FOREGROUND_SERVICE denegado
                        //Toast.makeText(this, "Permiso para usar servicios en primer plano denegado.", Toast.LENGTH_SHORT).show();
                    }
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // Permiso de ubicación precisa otorgado
                        Log.d("Permission", "Permiso ACCESS_FINE_LOCATION otorgado");
                    } else {
                        // Permiso de ubicación precisa denegado
                        //Toast.makeText(this, "Permiso para acceder a ubicación precisa denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * Solicitar permiso de notificación en versiones superiores a Android 13.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            boolean shouldRequest = false;
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    shouldRequest = true;
                    break;
                }
            }

            if (shouldRequest) {
                requestPermissions(permissions, 1);
            }
        }
    }

}