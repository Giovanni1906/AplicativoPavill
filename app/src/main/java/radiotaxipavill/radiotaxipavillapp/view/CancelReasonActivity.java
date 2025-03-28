package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.CancelPedidoController;

public class CancelReasonActivity extends AppCompatActivity {

    private EditText editMotivo;
    private LinearLayout optionProblemasConductor, optionDemoraExcesiva, optionOtrosMotivos;
    private RadioButton radioProblemasConductor, radioDemoraExcesiva, radioOtrosMotivos;
    private TemporaryData temporaryData;
    private RadioButton selectedRadioButton = null;
    private LinearLayout selectedLayout = null;
    private String motivoSeleccionado = "";

    private LoadingDialog loadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel);

        loadingDialog = new LoadingDialog(this);

        // Inicializar TemporaryData
        temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);  // 🔹 Restaurar datos guardados

        // Referencias a las vistas
        editMotivo = findViewById(R.id.edit_motivo);
        optionProblemasConductor = findViewById(R.id.optionProblemasConductor);
        optionDemoraExcesiva = findViewById(R.id.optionDemoraExcesiva);
        optionOtrosMotivos = findViewById(R.id.optionOtrosMotivos);

        radioProblemasConductor = findViewById(R.id.OptionProblemasConductor);
        radioDemoraExcesiva = findViewById(R.id.OptionDemoraExcesiva);
        radioOtrosMotivos = findViewById(R.id.OptionOtrosMotivos);

        // Configurar selección manual de opciones
        setupOption(optionProblemasConductor, radioProblemasConductor, "Problemas con el conductor");
        setupOption(optionDemoraExcesiva, radioDemoraExcesiva, "Demora excesiva");
        setupOption(optionOtrosMotivos, radioOtrosMotivos, "Otros motivos");

        // Botón "Enviar"
        findViewById(R.id.edit_button).setOnClickListener(v -> enviarMotivo());
    }

    /**
     * Configura la selección y deselección de opciones manualmente.
     */
    private void setupOption(LinearLayout layoutOption, RadioButton radioButton, String motivo) {
        layoutOption.setOnClickListener(v -> {
            if (selectedRadioButton == radioButton) {
                // Si se vuelve a tocar la misma opción, se deselecciona
                selectedRadioButton.setChecked(false);
                selectedLayout.setBackgroundResource(R.drawable.custom_radio_background);
                selectedRadioButton = null;
                selectedLayout = null;
                motivoSeleccionado = "";
                editMotivo.setText("");
                return;
            }

            if (selectedRadioButton != null) {
                // Deseleccionar la opción anterior
                selectedRadioButton.setChecked(false);
                selectedLayout.setBackgroundResource(R.drawable.custom_radio_background);
            }

            // Seleccionar la nueva opción
            radioButton.setChecked(true);
            layoutOption.setBackgroundResource(R.drawable.custom_radio_background_selected);
            motivoSeleccionado = motivo;
            editMotivo.setText(motivo);

            // Guardar selección
            selectedRadioButton = radioButton;
            selectedLayout = layoutOption;
        });

        // También aseguramos que el RadioButton responde al toque
        radioButton.setOnClickListener(v -> layoutOption.performClick());
    }

    private void enviarMotivo() {
        String comentarioAdicional = editMotivo.getText().toString().trim();

        loadingDialog.show();

        if (motivoSeleccionado.isEmpty()) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Por favor seleccione un motivo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el PedidoId desde TemporaryData
        String pedidoId = temporaryData.getPedidoId();
        if (pedidoId == null || pedidoId.isEmpty()) {
            loadingDialog.dismiss();
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
                        loadingDialog.dismiss();
                        Toast.makeText(CancelReasonActivity.this, "Cancelación exitosa", Toast.LENGTH_SHORT).show();

                        // limpiar TemporaryData
                        temporaryData.clearData(CancelReasonActivity.this);

                        // Redirigir al MapActivity
                        Intent intent = new Intent(CancelReasonActivity.this, MapActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        loadingDialog.dismiss();
                        Toast.makeText(CancelReasonActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}