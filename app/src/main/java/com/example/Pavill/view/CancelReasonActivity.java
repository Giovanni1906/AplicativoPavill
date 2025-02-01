package com.example.Pavill.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pavill.R;
import com.example.Pavill.components.TemporaryData;
import com.example.Pavill.controller.CancelPedidoController;

public class CancelReasonActivity extends AppCompatActivity {

    private EditText editMotivo;
    private RadioGroup radioGroupOpciones;
    private RadioButton optionProblemasConductor, optionDemoraExcesiva, optionOtrosMotivos;
    private TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel);

        // Inicializar TemporaryData
        temporaryData = TemporaryData.getInstance();

        // Referencias a las vistas
        editMotivo = findViewById(R.id.edit_motivo);
        radioGroupOpciones = findViewById(R.id.radioGroupOpciones);
        optionProblemasConductor = findViewById(R.id.optionProblemasConductor);
        optionDemoraExcesiva = findViewById(R.id.optionDemoraExcesiva);
        optionOtrosMotivos = findViewById(R.id.optionOtrosMotivos);

        // Configurar los listeners para los radio buttons
//        setupRadioListeners();

        // Botón "Enviar"
        findViewById(R.id.edit_button).setOnClickListener(v -> enviarMotivo());
    }

//    private void setupRadioListeners() {
//        optionProblemasConductor.setOnClickListener(v -> editMotivo.setText("Problemas con el conductor"));
//        optionDemoraExcesiva.setOnClickListener(v -> editMotivo.setText("Demora excesiva"));
//        optionOtrosMotivos.setOnClickListener(v -> editMotivo.setText("Otros motivos"));
//    }

    private void enviarMotivo() {
        String motivoSeleccionado = "";
        String comentarioAdicional = editMotivo.getText().toString().trim();

        int selectedId = radioGroupOpciones.getCheckedRadioButtonId();
        if (selectedId == R.id.optionProblemasConductor) {
            motivoSeleccionado = "Problemas con el conductor";
        } else if (selectedId == R.id.optionDemoraExcesiva) {
            motivoSeleccionado = "Demora excesiva";
        } else if (selectedId == R.id.optionOtrosMotivos) {
            motivoSeleccionado = "Otros motivos";
        }

        if (motivoSeleccionado.isEmpty()) {
            Toast.makeText(this, "Por favor seleccione un motivo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el PedidoId desde TemporaryData
        String pedidoId = TemporaryData.getInstance().getPedidoId();
        if (pedidoId == null || pedidoId.isEmpty()) {
            Toast.makeText(this, "Error interno, intente nuevamente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Llamar al controlador
        new CancelPedidoController().cancelarPedido(
                this,
                pedidoId,
                motivoSeleccionado,
                comentarioAdicional,
                new CancelPedidoController.CancelPedidoCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(CancelReasonActivity.this, "Cancelación exitosa", Toast.LENGTH_SHORT).show();

                        // limpiar TemporaryData
                        temporaryData = TemporaryData.getInstance();
                        temporaryData.clearData();

                        // Redirigir al MapActivity
                        Intent intent = new Intent(CancelReasonActivity.this, MapActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(CancelReasonActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}