package com.example.Pavill.view;

import androidx.appcompat.app.AppCompatActivity;

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

import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.example.Pavill.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.bumptech.glide.Glide;

public class SearchingActivity extends AppCompatActivity {

    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView textViewTimer;
    private Handler timerHandler = new Handler();
    private long startTime;
    private boolean isCancelled = false; // Variable de control para saber si la búsqueda fue cancelada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        initializeUI();
        initializeBottomSheet();
        startTimer();

        // Inicia la comprobación periódica de aceptación del viaje
        startCheckingForRideAcceptance();
    }

    private void initializeUI() {
        // Configurar el mensaje de actualización
        TextView textViewUpdateMessage = findViewById(R.id.textViewUpdateMessage);
        setTextWithColorSpan(textViewUpdateMessage, "actualiza\nla nueva versión\nde la aplicación", "nueva versión", R.color.primaryColor);

        // Configurar el cronómetro
        textViewTimer = findViewById(R.id.textViewTimer);

        // Botón para cancelar la búsqueda
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);
        btnCancelSearch.setOnClickListener(v -> cancelSearch());

        // Cargar el GIF de carga
        ImageView gifLoader = findViewById(R.id.gifLoader);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .transform(new FitCenter())
                .override(300, 300)
                .into(gifLoader);

        // Obtener las coordenadas de origen y destino
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            originLat = extras.getDouble("origin_lat");
            originLng = extras.getDouble("origin_lng");
            destinationLat = extras.getDouble("destination_lat");
            destinationLng = extras.getDouble("destination_lng");
        }
    }

    private void setTextWithColorSpan(TextView textView, String fullText, String targetText, int colorRes) {
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(targetText);
        int endIndex = startIndex + targetText.length();

        if (startIndex >= 0) {
            int primaryColor = getResources().getColor(colorRes);
            spannableString.setSpan(new ForegroundColorSpan(primaryColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannableString);
    }

    private void initializeBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(100);
        bottomSheetBehavior.setDraggable(true);
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
                timerHandler.postDelayed(this, 1000); // Actualiza cada segundo
            }
        }
    };

    private void updateTimer() {
        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        textViewTimer.setText(String.format("%02d:%02d", minutes, seconds));

        if (millis >= 10000) { // Pasados 10 segundos, simula la transición a WaitingActivity
            navigateToWaitingActivity();
        }
    }

    private void cancelSearch() {
        isCancelled = true;
        timerHandler.removeCallbacks(timerRunnable);
        finish(); // Finalizar la actividad y volver a la anterior
    }

    @Override
    public void onBackPressed() {
        cancelSearch(); // Usa el mismo método de cancelación
        super.onBackPressed();
    }

    private void navigateToWaitingActivity() {
        if (!isCancelled) {
            isCancelled = true; // Marcar como cancelado para detener cualquier ejecución posterior del cronómetro
            timerHandler.removeCallbacks(timerRunnable); // Detener el Runnable del cronómetro

            Intent intent = new Intent(SearchingActivity.this, WaitingActivity.class);
            intent.putExtra("origin_lat", originLat);
            intent.putExtra("origin_lng", originLng);
            intent.putExtra("destination_lat", destinationLat);
            intent.putExtra("destination_lng", destinationLng);
            startActivity(intent);
            finish(); // Finaliza la actividad actual
        }
    }

    // Simulación de comprobación de aceptación del viaje
    private void startCheckingForRideAcceptance() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCancelled) {
                    checkRideAcceptance();
                    handler.postDelayed(this, 3000); // Vuelve a verificar cada 3 segundos
                }
            }
        }, 3000);
    }

    private void checkRideAcceptance() {
        boolean rideAccepted = /* Lógica de verificación en el servidor */ false;

        if (rideAccepted) {
            onRideAccepted();
        }
    }

    private void onRideAccepted() {
//        isCancelled = true;
//        timerHandler.removeCallbacks(timerRunnable);
        navigateToWaitingActivity();
    }
}