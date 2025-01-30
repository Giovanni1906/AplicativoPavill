package com.example.Pavill.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pavill.R;

public class CancelReasonActivity extends AppCompatActivity {

    private EditText editMotivo;
    private TextView tvProblemasConductor, tvDemoraExcesiva, tvCambiosDestino, tvOtrosMotivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel);

        // Referencias a las vistas
        editMotivo = findViewById(R.id.edit_phone);
        tvProblemasConductor = findViewById(R.id.tvProblemasConductor);
        tvDemoraExcesiva = findViewById(R.id.tvDemoraExcesiva);
        tvCambiosDestino = findViewById(R.id.tvCambiosDestino);
        tvOtrosMotivos = findViewById(R.id.tvOtrosMotivos);

        // Configurar los listeners para las viñetas
        setupBulletListeners();

        // Listener para el botón "Enviar"
        findViewById(R.id.edit_button).setOnClickListener(v -> enviarMotivo());
    }

    private void setupBulletListeners() {
        tvProblemasConductor.setOnClickListener(v -> editMotivo.setText("Problemas con el conductor"));
        tvDemoraExcesiva.setOnClickListener(v -> editMotivo.setText("Demora excesiva"));
        tvCambiosDestino.setOnClickListener(v -> editMotivo.setText("Cambios en el destino"));
        tvOtrosMotivos.setOnClickListener(v -> editMotivo.setText("Otros motivos"));
    }

    private void enviarMotivo() {
        String motivo = editMotivo.getText().toString().trim();

        if (motivo.isEmpty()) {
            Toast.makeText(this, "Por favor seleccione o escriba un motivo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aquí puedes enviar el motivo a un servidor si es necesario

        // Mostrar mensaje de éxito
        Toast.makeText(this, "Registrado correctamente", Toast.LENGTH_SHORT).show();

        // Redirigir al MapActivity
        Intent intent = new Intent(CancelReasonActivity.this, MapActivity.class);
        startActivity(intent);
        finish();
    }
}
