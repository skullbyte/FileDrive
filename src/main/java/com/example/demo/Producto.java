package com.example.demo;
public class Producto {
    private int numero;
    private String concepto;
    private float importe;

    public Producto(int numero, String concepto, float importe) {
        this.numero = numero;
        this.concepto = concepto;
        this.importe = importe;
    }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public float getImporte() { return importe; }
    public void setImporte(float importe) { this.importe = importe; }
}