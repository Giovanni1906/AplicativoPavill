package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

public class TemporaryData {
    private static TemporaryData instance;
    private static final String PREFS_NAME = "TemporaryDataPrefs";

    // Variables de pedido
    private String pedidoId;
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;
    private String conductorId;
    private String conductorNombre;
    private String conductorTelefono;
    private String conductorFoto;
    private String unidadPlaca;
    private String unidadModelo;
    private String unidadColor;
    private String unidadCalificacion;
    private String distance;
    private String duration;
    private String estimatedCost;
    private String vehiculoUnidad;
    private String originName;
    private String destinationName;
    private String originReference;

    private long requestTime;

    private TemporaryData() { }

    public static TemporaryData getInstance() {
        if (instance == null) {
            instance = new TemporaryData();
        }
        return instance;
    }

    // 📌 Método para guardar datos en SharedPreferences
    public void saveToPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("pedidoId", pedidoId);
        editor.putString("conductorId", conductorId);
        editor.putString("conductorNombre", conductorNombre);
        editor.putString("conductorTelefono", conductorTelefono);
        editor.putString("conductorFoto", conductorFoto);
        editor.putString("unidadPlaca", unidadPlaca);
        editor.putString("unidadModelo", unidadModelo);
        editor.putString("unidadColor", unidadColor);
        editor.putString("unidadCalificacion", unidadCalificacion);
        editor.putString("distance", distance);
        editor.putString("duration", duration);
        editor.putString("estimatedCost", estimatedCost);
        editor.putString("vehiculoUnidad", vehiculoUnidad);
        editor.putString("originName", originName);
        editor.putString("destinationName", destinationName);
        editor.putString("originReference", originReference);
        editor.putLong("requestTime", requestTime);


        // Guardar LatLng como String "lat,lng"
        if (originCoordinates != null) {
            editor.putString("originCoordinates", originCoordinates.latitude + "," + originCoordinates.longitude);
        } else {
            editor.remove("originCoordinates");
        }

        if (destinationCoordinates != null) {
            editor.putString("destinationCoordinates", destinationCoordinates.latitude + "," + destinationCoordinates.longitude);
        } else {
            editor.remove("destinationCoordinates");
        }

        editor.apply();
    }

    // 📌 Método para restaurar datos de SharedPreferences
    public void loadFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        pedidoId = prefs.getString("pedidoId", null);
        conductorId = prefs.getString("conductorId", null);
        conductorNombre = prefs.getString("conductorNombre", null);
        conductorTelefono = prefs.getString("conductorTelefono", null);
        conductorFoto = prefs.getString("conductorFoto", null);
        unidadPlaca = prefs.getString("unidadPlaca", null);
        unidadModelo = prefs.getString("unidadModelo", null);
        unidadColor = prefs.getString("unidadColor", null);
        unidadCalificacion = prefs.getString("unidadCalificacion", null);
        distance = prefs.getString("distance", null);
        duration = prefs.getString("duration", null);
        estimatedCost = prefs.getString("estimatedCost", null);
        vehiculoUnidad = prefs.getString("vehiculoUnidad", null);
        originName = prefs.getString("originName", null);
        destinationName = prefs.getString("destinationName", null);
        originReference = prefs.getString("originReference", null);
        requestTime = prefs.getLong("requestTime", 0);

        // Recuperar LatLng desde String "lat,lng"
        String originString = prefs.getString("originCoordinates", null);
        if (originString != null) {
            String[] parts = originString.split(",");
            if (parts.length == 2) {
                originCoordinates = new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            }
        }

        String destinationString = prefs.getString("destinationCoordinates", null);
        if (destinationString != null) {
            String[] parts = destinationString.split(",");
            if (parts.length == 2) {
                destinationCoordinates = new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            }
        }

    }

    // metodos para los setters

    public void setPedidoId(String pedidoId, Context context) {
        this.pedidoId = pedidoId;
        saveToPreferences(context);  // 🔹 Guardar automáticamente
    }
    public void setConductorId(String conductorId, Context context) {
        this.conductorId = conductorId;
        saveToPreferences(context);  // 🔹 Guardar automáticamente
    }
    public void setConductorNombre(String conductorNombre, Context context) {
        this.conductorNombre = conductorNombre;
        saveToPreferences(context);
    }
    public void setConductorTelefono(String conductorTelefono, Context context) {
        this.conductorTelefono = conductorTelefono;
        saveToPreferences(context);
    }
    public void setConductorFoto(String conductorFoto, Context context) {
        this.conductorFoto = conductorFoto;
        saveToPreferences(context);
    }
    public void setUnidadPlaca(String unidadPlaca, Context context) {
        this.unidadPlaca = unidadPlaca;
        saveToPreferences(context);
    }
    public void setUnidadModelo(String unidadModelo, Context context) {
        this.unidadModelo = unidadModelo;
        saveToPreferences(context);
    }
    public void setUnidadColor(String unidadColor, Context context) {
        this.unidadColor = unidadColor;
        saveToPreferences(context);
    }
    public void setUnidadCalificacion(String unidadCalificacion, Context context) {
        this.unidadCalificacion = unidadCalificacion;
        saveToPreferences(context);
    }
    public void setDistance(String distance, Context context) {
        this.distance = distance;
        saveToPreferences(context);
    }
    public void setDuration(String duration, Context context) {
        this.duration = duration;
        saveToPreferences(context);
    }
    public void setEstimatedCost(String estimatedCost, Context context) {
        this.estimatedCost = estimatedCost;
        saveToPreferences(context);
    }
    public void setVehiculoUnidad(String vehiculoUnidad, Context context) {
        this.vehiculoUnidad = vehiculoUnidad;
        saveToPreferences(context);
    }
    public void setOriginName(String originName, Context context) {
        this.originName = originName;
        saveToPreferences(context);
    }
    public void setDestinationName(String destinationName, Context context) {
        this.destinationName = destinationName;
        saveToPreferences(context);
    }
    public void setOriginReference(String originReference, Context context) {
        this.originReference = originReference;
        saveToPreferences(context);
    }
    public void setRequestTime(Long requestTime, Context context) {
        this.requestTime = requestTime;
        saveToPreferences(context);
    }
    public void setOriginCoordinates(LatLng originCoordinates, Context context) {
        this.originCoordinates = originCoordinates;
        saveToPreferences(context);
    }

    public void setDestinationCoordinates(LatLng destinationCoordinates, Context context) {
        this.destinationCoordinates = destinationCoordinates;
        saveToPreferences(context);
    }

    // Getters para obtener los datos
    public String getPedidoId() { return pedidoId; }
    public String getConductorId() { return conductorId; }
    public String getConductorNombre() { return conductorNombre; }
    public String getConductorTelefono() { return conductorTelefono; }
    public String getConductorFoto() { return conductorFoto; }
    public String getUnidadPlaca() { return unidadPlaca; }
    public String getUnidadModelo() { return unidadModelo; }
    public String getUnidadColor() { return unidadColor; }
    public String getUnidadCalificacion() { return unidadCalificacion; }
    public String getDistance() { return distance; }
    public String getDuration() { return duration; }
    public String getEstimatedCost() { return estimatedCost; }
    public String getVehiculoUnidad() { return vehiculoUnidad; }
    public String getOriginName() { return originName; }
    public String getDestinationName() { return destinationName; }
    public String getOriginReference() { return originReference; }
    public long getRequestTime() { return requestTime; }

    // 🔹 Para obtener LatLng
    public LatLng getOriginCoordinates() { return originCoordinates; }
    public LatLng getDestinationCoordinates() { return destinationCoordinates; }


    // 📌 Método para limpiar los datos (cuando el pedido finaliza o se cancela)
    public void clearData(Context context) {
        pedidoId = null;
        originCoordinates = null;
        destinationCoordinates = null;
        conductorId = null;
        conductorNombre = null;
        conductorTelefono = null;
        conductorFoto = null;
        unidadPlaca = null;
        unidadModelo = null;
        unidadColor = null;
        unidadCalificacion = null;
        distance = null;
        duration = null;
        estimatedCost = null;
        vehiculoUnidad = null;
        originName = null;
        destinationName = null;
        originReference = null;


        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
