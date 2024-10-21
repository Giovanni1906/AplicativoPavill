package com.example.Pavill.controller;

public class AuthController {
    public boolean login(String email, String password) {
        // Lógica temporal para verificar usuario y contraseña
        return email.equals("admin@example.com") && password.equals("123456");
    }
}