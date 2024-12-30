package com.example.Pavill.components;

import com.google.android.gms.maps.model.LatLng;

public class TemporaryData {
    private static TemporaryData instance;

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


    private long requestTime;

    private TemporaryData() {
        // Constructor privado para el patrón singleton
    }

    public static TemporaryData getInstance() {
        if (instance == null) {
            instance = new TemporaryData();
        }
        return instance;
    }

    // Getters y setters para todos los campos
    
    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }
    public LatLng getOriginCoordinates() { return originCoordinates; }
    public void setOriginCoordinates(LatLng originCoordinates) { this.originCoordinates = originCoordinates; }
    public LatLng getDestinationCoordinates() { return destinationCoordinates; }
    public void setDestinationCoordinates(LatLng destinationCoordinates) { this.destinationCoordinates = destinationCoordinates; }
    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }
    public String getConductorNombre() { return conductorNombre; }
    public void setConductorNombre(String conductorNombre) { this.conductorNombre = conductorNombre; }
    public String getConductorTelefono() { return conductorTelefono; }
    public void setConductorTelefono(String conductorTelefono) { this.conductorTelefono = conductorTelefono; }
    public String getConductorFoto() { return conductorFoto; }
    public void setConductorFoto(String conductorFoto) { this.conductorFoto = conductorFoto; }
    public String getUnidadPlaca() { return unidadPlaca; }
    public void setUnidadPlaca(String unidadPlaca) { this.unidadPlaca = unidadPlaca; }
    public String getUnidadModelo() { return unidadModelo; }
    public void setUnidadModelo(String unidadModelo) { this.unidadModelo = unidadModelo; }
    public String getUnidadColor() { return unidadColor; }
    public void setUnidadColor(String unidadColor) { this.unidadColor = unidadColor; }
    public String getUnidadCalificacion() { return unidadCalificacion; }
    public void setUnidadCalificacion(String unidadCalificacion) { this.unidadCalificacion = unidadCalificacion; }
    public String getDistanceD() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getEstimatedCost() {return estimatedCost; }
    public void setEstimatedCost(String estimatedCost) {this.estimatedCost = estimatedCost; }

    // Método para establecer el tiempo del pedido
    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }

    // Método para obtener el tiempo del pedido
    public long getRequestTime() {
        return requestTime;
    }

    //verificadores
    public boolean isPedidoIdAvailable() {
        return pedidoId != null && !pedidoId.isEmpty();
    }

    public boolean isConductorDataAvailable() {
        return conductorNombre != null && !conductorNombre.isEmpty();
    }

    public void clearData() {
        pedidoId = null;
        originCoordinates = null;
        destinationCoordinates = null;
        conductorNombre = null;
        conductorTelefono = null;
        conductorFoto = null;
        unidadPlaca = null;
        unidadModelo = null;
        unidadColor = null;
    }
}
