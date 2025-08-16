package radiotaxipavill.radiotaxipavillapp.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.PedidoCancellationHelper;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.CancelRequestController;
import radiotaxipavill.radiotaxipavillapp.controller.PublicidadController;
import radiotaxipavill.radiotaxipavillapp.receiver.PedidoStatusReceiver;
import radiotaxipavill.radiotaxipavillapp.services.PedidoStatusService;

import com.bumptech.glide.Glide;

public class SearchingActivity extends AppCompatActivity {

    private TextView textViewTimer;
    private Handler timerHandler = new Handler();
    private long startTime;
    private boolean isCancelled = false;
    private LoadingDialog loadingDialog;
    private TemporaryData temporaryData;
    private PedidoStatusReceiver pedidoStatusReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        loadingDialog = new LoadingDialog(this);
        initializeUI();
        startTimer(); // Iniciar el temporizador

        // Guardar el estado del pedido en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("PedidoPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("pedido_activo", true).apply();

        // Iniciar el servicio de seguimiento de pedidos
        Intent serviceIntent = new Intent(this, PedidoStatusService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent); // Para Android 8+
        } else {
            this.startService(serviceIntent);
        }

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
     * Registra el receptor de cambios de estado del pedido.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Inicializa el PedidoStatusReceiver con un callback para manejar los cambios de estado
        pedidoStatusReceiver = new PedidoStatusReceiver(status -> {
            Log.d("SearchingActivity", "Estado del pedido recibido: " + status);

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
                    Log.d("SearchingActivity", "Estado desconocido o en espera: " + status);
                    break;
            }
        });

        // Registra el receptor con el intent-filter
        IntentFilter filter = new IntentFilter(PedidoStatusReceiver.ACTION_PEDIDO_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(pedidoStatusReceiver, filter);
    }

    /**
     * Desregistra el receptor de cambios de estado del pedido.
     */
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pedidoStatusReceiver);
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
                Toast.makeText(SearchingActivity.this, "Pedido cancelado con exito.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(SearchingActivity.this, "Error al cancelar el pedido, intente nuevamente.", Toast.LENGTH_SHORT).show();
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
                        .fitCenter() // Asegura que la imagen se ajuste al tamaño del ImageView respetando sus proporciones
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
}