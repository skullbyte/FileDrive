package com.example.demo;

// Práctica 4: buscar un texto en los ficheros de una carpeta
public class BuscarModel {
    private String carpeta = "";
    private String texto;
    private boolean recursivo;

    public String getCarpeta() { return carpeta; }
    public void setCarpeta(String carpeta) { this.carpeta = carpeta; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public boolean isRecursivo() { return recursivo; }
    public void setRecursivo(boolean recursivo) { this.recursivo = recursivo; }
}
