package radiotaxipavill.radiotaxipavillapp.components;

import android.app.Dialog;
import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import radiotaxipavill.radiotaxipavillapp.R;

public class LoadingDialog {

    private Dialog dialog;

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_loading); // Layout personalizado para el indicador de carga
        dialog.setCancelable(false); // No permitir cancelar tocando fuera del dialog

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Fondo transparente
        }

        // Cargar el GIF usando Glide
        ImageView loadingImage = dialog.findViewById(R.id.loadingImage);
        Glide.with(context)
                .asGif()
                .load(R.drawable.loading) // Tu archivo loading.gif en el drawable
                .into(loadingImage);

    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}
