package radiotaxipavill.radiotaxipavillapp.controller;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import radiotaxipavill.radiotaxipavillapp.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteController {

    public interface RouteCallback {
        void onRouteSuccess(List<LatLng> polylinePoints, String distanceText, String durationText, double estimatedCost);
        void onRouteError(String errorMessage);
    }

    private Polyline currentPolyline; // Referencia a la polyline actual
    private final Context context;

    public RouteController(Context context) {
        this.context = context;
    }

    /**
     * Solicita una ruta entre origen y destino utilizando la API Directions de Google.
     */
    public void fetchRoute(LatLng origin, LatLng destination, GoogleMap mMap, RouteCallback callback) {
        if (origin == null || destination == null) {
            callback.onRouteError("Origen o destino no definido.");
            return;
        }

        String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + context.getString(R.string.map_api_key);

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, directionsUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray routes = jsonResponse.getJSONArray("routes");

                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String points = overviewPolyline.getString("points");

                            // Decodificar polyline
                            List<LatLng> polylinePoints = decodePoly(points);

                            // Extraer distancia y duración del primer "leg"
                            JSONArray legs = route.getJSONArray("legs");
                            JSONObject leg = legs.getJSONObject(0);

                            String distanceText = leg.getJSONObject("distance").getString("text");
                            String durationText = leg.getJSONObject("duration").getString("text");
                            Log.d("RouteController", "distancia: " + distanceText);
                            Log.d("RouteController", "kilometraje: " + durationText);

                            // Calcular costo estimado
                            double estimatedCost = calculateEstimatedCostKm(distanceText);

                            // Callback con los resultados
                            callback.onRouteSuccess(polylinePoints, distanceText, durationText, estimatedCost);

                            // Dibujar la polyline en el mapa
                            drawPolyline(mMap, polylinePoints);
                        } else {
                            callback.onRouteError("No se encontró una ruta.");
                        }
                    } catch (Exception e) {
                        Log.e("RouteController", "Error procesando la respuesta de la API Directions.", e);
                        callback.onRouteError("Error procesando la ruta.");
                    }
                },
                error -> {
                    Log.e("RouteController", "Error al obtener la ruta: " + error.getMessage());
                    callback.onRouteError("Error al obtener la ruta.");
                });

        queue.add(request);
    }


    /**
     * Decodifica una polyline codificada en una lista de puntos LatLng.
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    /**
     * Calcula el costo estimado basado en la duración.
     */
    private double calculateEstimatedCost(String durationText) {
        double durationMinutes = Double.parseDouble(durationText.replace(" mins", "").replace(" min", ""));
        double estimatedCost;

        if (durationMinutes <= 10) {
            estimatedCost = (10.0 / 10) * durationMinutes; // Tarifa base de 10 soles por 10 minutos
        } else {
            estimatedCost = (13.0 / 18) * durationMinutes; // Tarifa de 13 soles por 18 minutos
        }

        // Ajustar costo entre 6 y 20 soles
        if (estimatedCost < 6.0) estimatedCost = 6.0;
        if (estimatedCost > 20.0) estimatedCost = 20.0;

        return Math.round(estimatedCost);
    }

    /**
     * Calcula el costo estimado basado en el kilometraje.
     */
    private double calculateEstimatedCostKm(String durationText) {
        double durationKm = Double.parseDouble(durationText.replace(" km", "").replace(" m", ""));
        double estimatedCost;

        if (durationKm <= 3) {
            estimatedCost = (2) * durationKm; // Tarifa base 1.6 soles por km
        } else {
            estimatedCost = (1.5) * durationKm; // Tarifa de 1.5 soles por km
        }

        // Ajustar costo entre 6 y 20 soles
        if (estimatedCost < 6.0) estimatedCost = 6.0;
        if (estimatedCost > 25.0) estimatedCost = 20.0;

        return Math.round(estimatedCost);
    }

    /**
     * Dibuja una polyline en el mapa.
     */
    private void drawPolyline(GoogleMap mMap, List<LatLng> polylinePoints) {
        if (mMap != null) {
            // Eliminar la polyline anterior si existe
            if (currentPolyline != null) {
                currentPolyline.remove();
            }

            // Dibujar la nueva polyline
            currentPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(polylinePoints)
                    .width(10)
                    .color(ContextCompat.getColor(context, R.color.secondaryColor))
                    .geodesic(true));
        }
    }

    /**
     * Borra la polyline actual del mapa.
     */
    public void clearRoute() {
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
            Log.d("RouteController", "Ruta eliminada del mapa.");
        }
    }
}

