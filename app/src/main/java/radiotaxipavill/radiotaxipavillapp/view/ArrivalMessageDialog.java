package radiotaxipavill.radiotaxipavillapp.view;

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
import radiotaxipavill.radiotaxipavillapp.R;

public class ArrivalMessageDialog extends DialogFragment {

    private OnConfirmClickListener listener;
    private String textArrivalMessageDialog;
    private String buttonText; //texto del botón

    public interface OnConfirmClickListener {
        void onConfirmClick();
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.listener = listener;
    }

    public void setDriverName(String textArrivalMessageDialog) {
        this.textArrivalMessageDialog = textArrivalMessageDialog;
    }

    // Nuevo método para configurar el texto del botón
    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_arrival_message, container, false);

        // Configurar el texto del mensaje
        TextView tvArrivalMessage = view.findViewById(R.id.tvArrivalMessage);
        tvArrivalMessage.setText(textArrivalMessageDialog);

        // Configurar el texto del botón "OK"
        Button btnOk = view.findViewById(R.id.btnOk);
        if (buttonText != null) {
            btnOk.setText(buttonText);
        }
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
}