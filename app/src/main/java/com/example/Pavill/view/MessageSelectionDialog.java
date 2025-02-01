package com.example.Pavill.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.Pavill.R;
import com.example.Pavill.controller.EnviarMensajeController;

public class MessageSelectionDialog extends DialogFragment {

    private String conductorTelefono;
    private Context context;
    private RadioGroup radioGroupOpciones;

    public MessageSelectionDialog(Context context, String conductorTelefono) {
        this.conductorTelefono = conductorTelefono;
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_message_selection, container, false);

        // Referencia al RadioGroup
        radioGroupOpciones = view.findViewById(R.id.radioGroupOpciones);

        // Botón "Enviar"
        view.findViewById(R.id.btnEnviar).setOnClickListener(v -> enviarMensaje());

        // Botón "Cancelar"
        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        return view;
    }

    private void enviarMensaje() {
        String mensaje = null;

        // Verificar qué opción está seleccionada
        int selectedId = radioGroupOpciones.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Log.e("Mensaje", "No se seleccionó ninguna opción.");
            return;
        }

        RadioButton selectedRadioButton = radioGroupOpciones.findViewById(selectedId);
        if (selectedRadioButton != null) {
            mensaje = selectedRadioButton.getText().toString(); // Obtiene el texto del RadioButton seleccionado
        }

        EnviarMensajeController enviarMensajeController = new EnviarMensajeController();
        enviarMensajeController.enviarMensaje(context, mensaje, new EnviarMensajeController.MensajeCallback() {
            @Override
            public void onSuccess(String respuesta) {
                Log.d("Mensaje", "Enviado correctamente: " + respuesta);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("Mensaje", "Error al enviar: " + errorMessage);
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