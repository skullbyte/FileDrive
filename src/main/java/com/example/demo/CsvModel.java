package com.example.demo;

// Práctica 6: filtros del editor SDMX-CSV
public class CsvModel {
    private String fichero;
    private String pais;
    private String fecha;
    private String columna;

    public String getFichero() { return fichero; }
    public void setFichero(String fichero) { this.fichero = fichero; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getColumna() { return columna; }
    public void setColumna(String columna) { this.columna = columna; }
}
