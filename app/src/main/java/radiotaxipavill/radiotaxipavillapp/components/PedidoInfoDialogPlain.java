package radiotaxipavill.radiotaxipavillapp.components;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import radiotaxipavill.radiotaxipavillapp.R;

public class PedidoInfoDialogPlain extends androidx.fragment.app.DialogFragment {

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View v = inflater.inflate(R.layout.dialog_pedido_details, null, false);

        // Bind views
        ImageButton btnClose = v.findViewById(R.id.btnClose);
        TextView tvDriverName = v.findViewById(R.id.tvDriverName);
        TextView tvDriverPhone = v.findViewById(R.id.tvDriverPhone);
        TextView tvUnidad = v.findViewById(R.id.tvUnidad);
        TextView tvModelo = v.findViewById(R.id.tvModelo);
        TextView tvPlaca  = v.findViewById(R.id.tvPlaca);
        TextView tvColor  = v.findViewById(R.id.tvColor);

        TextView tvOrigin = v.findViewById(R.id.tvOrigin);
        TextView tvRef    = v.findViewById(R.id.tvRef);
        TextView tvDestination = v.findViewById(R.id.tvDestination);
        TextView tvCost   = v.findViewById(R.id.tvCost);

        // Cargar datos
        TemporaryData data = TemporaryData.getInstance();
        data.loadFromPreferences(ctx);

        setOrHideSuffix(tvDriverName, "• Nombre: ", data.getConductorNombre());
        setOrHideSuffix(tvDriverPhone,"• Teléfono: ", data.getConductorTelefono());
        setOrHideSuffix(tvUnidad,     "• Unidad: ", data.getVehiculoUnidad());
        setOrHideSuffix(tvModelo,     "• Modelo: ", data.getUnidadModelo());
        setOrHideSuffix(tvPlaca,      "• Placa: ",  data.getUnidadPlaca());
        setOrHideSuffix(tvColor,      "• Color: ",  data.getUnidadColor());

        setOrHideSuffix(tvOrigin,     "• Lugar de origen: ", data.getOriginName());
        setOrHideSuffix(tvRef,        "• Referencia: ",      data.getOriginReference());
        setOrHideSuffix(tvDestination,"• Lugar de destino: ",data.getDestinationName());
        if (data.getEstimatedCost() != null)
            tvCost.setText("• Costo estimado: S/" + String.format(java.util.Locale.US,"%.2f", data.getEstimatedCost()));
        else
            tvCost.setVisibility(View.GONE);

        btnClose.setOnClickListener(_v -> dismiss());

        // Construir AlertDialog con vista custom
        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setView(v)
                        .create();

        // Fondo transparente + leve oscurecido + animaciones
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            w.setDimAmount(0.35f);
            w.setWindowAnimations(R.style.PavillFloatingDialog_Window);
        }
        setCancelable(true);
        return dialog;
    }

    private void setOrHideSuffix(TextView tv, String prefix, @Nullable String value){
        if (value == null || value.trim().isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(prefix + value);
            tv.setVisibility(View.VISIBLE);
        }
    }

    // Helper para mostrarlo fácil
    public static void show(@NonNull androidx.fragment.app.FragmentManager fm){
        new PedidoInfoDialogPlain().show(fm, "pedido_info_plain");
    }
}
