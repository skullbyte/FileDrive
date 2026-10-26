package com.example.demo;

// Práctica 2 y 5: crear un fichero (con contenido opcional) o una carpeta
public class CrearModel {
    private String carpeta = "";
    private String nombre;
    private String tipo = "fichero";
    private String contenido;

    public String getCarpeta() { return carpeta; }
    public void setCarpeta(String carpeta) { this.carpeta = carpeta; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}
