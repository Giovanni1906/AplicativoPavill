package com.example.Pavill.components;

import android.app.Dialog;
import android.content.Context;

import com.example.Pavill.R;

public class LoadingDialog {

    private Dialog dialog;

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_loading); // Layout personalizado para el indicador de carga
        dialog.setCancelable(false); // No permitir cancelar tocando fuera del dialog

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Fondo transparente
        }
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}
