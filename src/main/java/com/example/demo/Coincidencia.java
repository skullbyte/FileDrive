package com.example.demo;

// Una aparición del texto buscado (Práctica 4)
public class Coincidencia {
    private final String fichero;
    private final int linea;
    private final int columna;
    private final String textoLinea;

    public Coincidencia(String fichero, int linea, int columna, String textoLinea) {
        this.fichero = fichero;
        this.linea = linea;
        this.columna = columna;
        this.textoLinea = textoLinea;
    }

    public String getFichero() { return fichero; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
    public String getTextoLinea() { return textoLinea; }
}
