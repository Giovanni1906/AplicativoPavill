package com.example.Pavill.view;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.Pavill.R;

public class MessageSelectionDialog extends DialogFragment {

    private String conductorTelefono;
    private CheckBox checkProblemasConductor, checkDemoraExcesiva, checkCambiosDestino, checkOtrosMotivos;

    public MessageSelectionDialog(String conductorTelefono) {
        this.conductorTelefono = conductorTelefono;
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
            mensaje = "Hola, tengo problemas con el conductor.";
        } else if (checkDemoraExcesiva.isChecked()) {
            mensaje = "Hola, el servicio está tardando demasiado.";
        } else if (checkCambiosDestino.isChecked()) {
            mensaje = "Hola, quiero cambiar mi destino.";
        } else if (checkOtrosMotivos.isChecked()) {
            mensaje = "Hola, tengo otra consulta sobre el servicio.";
        }

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