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

        // Obtener el TextView
        TextView textViewUpdateMessage = findViewById(R.id.textViewUpdateMessage);

        // Texto completo
        String fullText = "actualiza\nla nueva versión\nde la aplicación";

        // Crear SpannableString con el texto completo
        SpannableString spannableString = new SpannableString(fullText);

        // Encontrar la parte de "nueva versión"
        String targetText = "nueva versión";
        int startIndex = fullText.indexOf(targetText);
        int endIndex = startIndex + targetText.length();

        // Aplicar el color primario al texto "nueva versión"
        if (startIndex >= 0) {
            int primaryColor = getResources().getColor(R.color.primaryColor);
            spannableString.setSpan(new ForegroundColorSpan(primaryColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Establecer el SpannableString en el TextView
        textViewUpdateMessage.setText(spannableString);

        // Obtener las coordenadas de origen y destino desde el Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            originLat = extras.getDouble("origin_lat");
            originLng = extras.getDouble("origin_lng");
            destinationLat = extras.getDouble("destination_lat");
            destinationLng = extras.getDouble("destination_lng");
        }

        // Inicializar el BottomSheet
        initializeBottomSheet();

        // Iniciar el cronómetro
        textViewTimer = findViewById(R.id.textViewTimer);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        // Botón para cancelar la búsqueda
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);
        btnCancelSearch.setOnClickListener(v -> {
            isCancelled = true; // Establecer el estado como cancelado
            timerHandler.removeCallbacks(timerRunnable); // Detener el cronómetro
            finish(); // Finalizar la actividad y volver a la anterior
        });

        // Cargar el GIF con Glide
        ImageView gifLoader = findViewById(R.id.gifLoader);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .transform(new FitCenter()) // Para mantener las proporciones y ajustar al ImageView
                .override(300, 300) // Ancho y alto deseados
                .into(gifLoader);
    }

    /**
     * Inicializa el BottomSheet para que se comporte como en MapActivity.
     */
    private void initializeBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(100); // Altura mínima visible del BottomSheet
        bottomSheetBehavior.setDraggable(true); // Permitir arrastrar el BottomSheet
    }

    /**
     * Cronómetro para mostrar el tiempo transcurrido y manejar la transición.
     */
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCancelled) { // Verificar si la búsqueda no ha sido cancelada
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                textViewTimer.setText(String.format("%02d:%02d", minutes, seconds));

                // Verificar si han pasado 3 segundos para iniciar la actividad de WaitingActivity
                if (millis >= 10000) {
                    // Luego del retardo, pasar a la actividad de progreso del viaje
                    Intent intent = new Intent(SearchingActivity.this, WaitingActivity.class);
                    intent.putExtra("origin_lat", originLat);
                    intent.putExtra("origin_lng", originLng);
                    intent.putExtra("destination_lat", destinationLat);
                    intent.putExtra("destination_lng", destinationLng);
                    startActivity(intent);
                    finish(); // Finaliza la actividad para que el usuario no pueda regresar
                } else {
                    timerHandler.postDelayed(this, 1000); // Actualiza cada segundo
                }
            }
        }
    };
}
