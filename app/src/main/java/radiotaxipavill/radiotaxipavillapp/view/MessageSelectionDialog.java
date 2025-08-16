package radiotaxipavill.radiotaxipavillapp.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.controller.EnviarMensajeController;

public class MessageSelectionDialog extends DialogFragment {

    private String conductorTelefono;
    private Context context;

    private RadioButton selectedRadioButton = null;
    private LinearLayout selectedLayout = null;

    public MessageSelectionDialog(Context context, String conductorTelefono) {
        this.conductorTelefono = conductorTelefono;
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_message_selection, container, false);

        // Configurar opciones dentro del layout
        setupOption(view, R.id.optionSaldreAhora, R.id.OptionSaldreAhora);
        setupOption(view, R.id.optionEstoyAfuera, R.id.OptionEstoyAfuera);
        setupOption(view, R.id.optionEspereme, R.id.OptionEspereme);

        // Botón "Enviar"
        view.findViewById(R.id.btnEnviar).setOnClickListener(v -> enviarMensaje());

        // Botón "Cancelar"
        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        return view;
    }

    /**
     * Configura la selección y deselección de opciones manualmente.
     */
    private void setupOption(View view, int layoutId, int radioButtonId) {
        LinearLayout layoutOption = view.findViewById(layoutId);
        RadioButton radioButton = view.findViewById(radioButtonId);

        layoutOption.setOnClickListener(v -> {
            if (selectedRadioButton == radioButton) {
                // Si se vuelve a tocar la misma opción, se deselecciona
                selectedRadioButton.setChecked(false);
                selectedLayout.setBackgroundResource(R.drawable.custom_radio_background);
                selectedRadioButton = null;
                selectedLayout = null;
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

            // Guardar selección
            selectedRadioButton = radioButton;
            selectedLayout = layoutOption;
        });

        // También aseguramos que el RadioButton responde al toque
        radioButton.setOnClickListener(v -> layoutOption.performClick());
    }

    /**
     * Envía el mensaje seleccionado
     */
    private void enviarMensaje() {
        if (selectedRadioButton == null) {
            Log.e("MensajeSelectionDialog", "No se seleccionó ninguna opción.");
            return;
        }

        String mensaje = selectedRadioButton.getText().toString(); // Obtiene el texto del RadioButton seleccionado

        EnviarMensajeController enviarMensajeController = new EnviarMensajeController();
        enviarMensajeController.enviarMensaje(context, mensaje, new EnviarMensajeController.MensajeCallback() {
            @Override
            public void onSuccess(String respuesta) {
                Log.d("MensajeSelectionDialog", "Enviado correctamente: " + respuesta);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("MensajeSelectionDialog", "Error al enviar: " + errorMessage);
            }
        });

        dismiss(); // Cerrar la ventana flotante después de enviar
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
}
