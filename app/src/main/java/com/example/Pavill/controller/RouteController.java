package com.example.Pavill.controller;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;

public class RouteController {

    private final Context context;

    public RouteController(Context context) {
        this.context = context;
    }

    public interface RouteCallback {
        void onRouteReceived(List<LatLng> route);
        void onError(String errorMessage);
    }

    public void fetchRoute(String url, RouteCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray routes = jsonResponse.getJSONArray("routes");

                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONArray legs = route.getJSONArray("legs");
                            JSONObject leg = legs.getJSONObject(0);

                            JSONArray steps = leg.getJSONArray("steps");
                            List<LatLng> path = new ArrayList<>();
                            for (int i = 0; i < steps.length(); i++) {
                                JSONObject step = steps.getJSONObject(i);
                                JSONObject startLocation = step.getJSONObject("start_location");
                                path.add(new LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng")));
                            }
                            callback.onRouteReceived(path);
                        } else {
                            callback.onError("No se encontró ruta.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Error al procesar la respuesta.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onError("Error de conexión.");
                });

        queue.add(request);
    }
}
