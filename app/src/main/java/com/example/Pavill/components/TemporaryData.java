package com.example.Pavill.components;

import com.google.android.gms.maps.model.LatLng;

public class TemporaryData {
    private static TemporaryData instance;

    private String pedidoId;
    private LatLng originCoordinates;
    private LatLng destinationCoordinates;

    private TemporaryData() {
        // Constructor privado para el patrón singleton
    }

    public static TemporaryData getInstance() {
        if (instance == null) {
            instance = new TemporaryData();
        }
        return instance;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public LatLng getOriginCoordinates() {
        return originCoordinates;
    }

    public void setOriginCoordinates(LatLng originCoordinates) {
        this.originCoordinates = originCoordinates;
    }

    public LatLng getDestinationCoordinates() {
        return destinationCoordinates;
    }

    public void setDestinationCoordinates(LatLng destinationCoordinates) {
        this.destinationCoordinates = destinationCoordinates;
    }

    public void clearData() {
        pedidoId = null;
        originCoordinates = null;
        destinationCoordinates = null;
    }
}
