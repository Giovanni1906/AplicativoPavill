package com.example.Pavill.controller;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.Pavill.view.MainActivity;
import com.example.Pavill.view.RatingActivity;

public class ProgressController {

    private Context context;

    public ProgressController(Context context) {
        this.context = context;
    }

    // Método para finalizar el viaje
    public void finishTravel() {
        // Aquí podrías agregar la lógica necesaria para finalizar el viaje.
        // Por ejemplo, enviar datos al servidor, finalizar la actividad en el backend, etc.

        // Mostrar mensaje de viaje finalizado
        Toast.makeText(context, "El viaje ha finalizado", Toast.LENGTH_SHORT).show();

        // Después de finalizar el viaje, redirigir a la RatingActivity para calificar al conductor
        Intent intent = new Intent(context, RatingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
