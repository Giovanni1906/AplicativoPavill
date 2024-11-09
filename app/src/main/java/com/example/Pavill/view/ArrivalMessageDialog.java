package com.example.Pavill.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.widget.Button;
import android.widget.TextView;
import com.example.Pavill.R;

public class ArrivalMessageDialog extends DialogFragment {

    private OnConfirmClickListener listener;
    private String driverName;

    public interface OnConfirmClickListener {
        void onConfirmClick();
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.listener = listener;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_arrival_message, container, false);

        // Configurar el texto del nombre del conductor
        TextView tvArrivalMessage = view.findViewById(R.id.tvArrivalMessage);
        tvArrivalMessage.setText(driverName + ", tu pavill ya llegó");

        // Configurar el botón "OK"
        Button btnOk = view.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirmClick();
            }
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Hacer que el fondo sea transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
}
