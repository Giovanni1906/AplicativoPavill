package com.example.Pavill.model;

public class User {
    private String dni;
    private String name;
    private String phone;
    private String email;
    private String password;

    // Constructor
    public User(String dni, String name, String phone, String email, String password) {
        this.dni = dni;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}