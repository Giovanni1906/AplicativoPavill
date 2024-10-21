package com.example.Pavill.controller;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;
import java.util.ArrayList;

public class MapController {

    private GoogleMap mMap;

    // Método para inicializar el mapa
    public void initializeMap(GoogleMap googleMap) {
        this.mMap = googleMap;
        // Configurar el mapa: tipo, zoom, etc.
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    // Método para agregar taxis en el mapa
    public void addTaxiMarkers(List<LatLng> taxiLocations) {
        for (LatLng location : taxiLocations) {
            mMap.addMarker(new MarkerOptions().position(location).title("Taxi Disponible"));
        }
    }

    // Método para centrar la cámara en una ubicación
    public void moveCameraToLocation(LatLng location, float zoomLevel) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
    }

    // Método para obtener taxis cercanos (simulado)
    public List<LatLng> getNearbyTaxis() {
        List<LatLng> taxis = new ArrayList<>();
        taxis.add(new LatLng(-12.046374, -77.0427934));  // Ejemplo en Lima, Perú
        taxis.add(new LatLng(-12.045, -77.03));  // Otro taxi
        return taxis;
    }
}