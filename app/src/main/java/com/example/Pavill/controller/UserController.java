package com.example.Pavill.controller;
import com.example.Pavill.model.User;

public class UserController {
    public User createUser(String dni, String nombre, String celular, String correo, String contraseña) {
        // Lógica para crear un nuevo usuario
        return new User(dni, nombre, celular, correo, contraseña);
    }
}