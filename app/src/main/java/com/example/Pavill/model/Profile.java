package com.example.Pavill.model;

public class Profile {
    private String photoUrl;
    private String telefono;
    private String correo;
    private boolean recibeNotificaciones;

    public Profile(String photoUrl, String telefono, String correo, boolean recibeNotificaciones) {
        this.photoUrl = photoUrl;
        this.telefono = telefono;
        this.correo = correo;
        this.recibeNotificaciones = recibeNotificaciones;
    }
    // Getters y setters para cada atributo
}