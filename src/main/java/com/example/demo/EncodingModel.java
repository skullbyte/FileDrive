package com.example.demo;

// Práctica 9: conversión de encoding (entradas y resultado)
public class EncodingModel {
    private String entrada;
    private String encEntrada = "UTF-8";
    private String salida;
    private String encSalida = "ISO-8859-1";
    private String resultado;

    public String getEntrada() { return entrada; }
    public void setEntrada(String entrada) { this.entrada = entrada; }
    public String getEncEntrada() { return encEntrada; }
    public void setEncEntrada(String encEntrada) { this.encEntrada = encEntrada; }
    public String getSalida() { return salida; }
    public void setSalida(String salida) { this.salida = salida; }
    public String getEncSalida() { return encSalida; }
    public void setEncSalida(String encSalida) { this.encSalida = encSalida; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
}
