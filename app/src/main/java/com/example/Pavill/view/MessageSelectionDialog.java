package com.example.Pavill.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.Pavill.R;
import com.example.Pavill.controller.EnviarMensajeController;

public class MessageSelectionDialog extends DialogFragment {

    private String conductorTelefono;
    private Context context;
    private CheckBox checkProblemasConductor, checkDemoraExcesiva, checkCambiosDestino, checkOtrosMotivos;

    public MessageSelectionDialog(Context context, String conductorTelefono) {
        this.conductorTelefono = conductorTelefono;
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_message_selection, container, false);

        // CheckBoxes
        checkProblemasConductor = view.findViewById(R.id.checkProblemasConductor);
        checkDemoraExcesiva = view.findViewById(R.id.checkDemoraExcesiva);
        checkCambiosDestino = view.findViewById(R.id.checkCambiosDestino);
        checkOtrosMotivos = view.findViewById(R.id.checkOtrosMotivos);

        // Listeners de selección
        view.findViewById(R.id.optionProblemasConductor).setOnClickListener(v -> selectOption(checkProblemasConductor));
        view.findViewById(R.id.optionDemoraExcesiva).setOnClickListener(v -> selectOption(checkDemoraExcesiva));
        view.findViewById(R.id.optionCambiosDestino).setOnClickListener(v -> selectOption(checkCambiosDestino));
        view.findViewById(R.id.optionOtrosMotivos).setOnClickListener(v -> selectOption(checkOtrosMotivos));

        // Botón "Enviar"
        view.findViewById(R.id.btnEnviar).setOnClickListener(v -> enviarMensaje());

        // Botón "Cancelar"
        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        return view;
    }

    private void selectOption(CheckBox selectedCheckBox) {
        // Desmarcar todos los CheckBoxes
        checkProblemasConductor.setChecked(false);
        checkDemoraExcesiva.setChecked(false);
        checkCambiosDestino.setChecked(false);
        checkOtrosMotivos.setChecked(false);

        // Marcar solo el seleccionado
        selectedCheckBox.setChecked(true);
    }

    private void enviarMensaje() {
        String mensaje = null;

        if (checkProblemasConductor.isChecked()) {
            mensaje = "Espere un momento.";
        } else if (checkDemoraExcesiva.isChecked()) {
            mensaje = "Ya estoy afuera.";
        } else if (checkCambiosDestino.isChecked()) {
            mensaje = "Ok.";
        } else if (checkOtrosMotivos.isChecked()) {
            mensaje = "gracias.";
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