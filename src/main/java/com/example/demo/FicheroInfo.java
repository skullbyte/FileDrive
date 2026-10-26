package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Modelo con la información de un fichero sacada de java.io.File (Práctica 1)
public class FicheroInfo {
    private final File f;
    private final String ruta;
    private int[] vocales;            // Práctica 3 con BufferedReader
    private int[] vocalesFileReader;  // Práctica 3 con FileReader

    public FicheroInfo(File f, String ruta) {
        this.f = f;
        this.ruta = ruta;
    }

    public String getNombre() { return f.getName(); }
    public String getRuta() { return ruta; }
    public String getPadre() {
        int i = ruta.lastIndexOf('/');
        return i < 0 ? "" : ruta.substring(0, i);
    }
    public boolean isDirectorio() { return f.isDirectory(); }
    public boolean isFichero() { return f.isFile(); }
    public boolean isOculto() { return f.isHidden(); }
    public boolean isLectura() { return f.canRead(); }
    public boolean isEscritura() { return f.canWrite(); }
    public boolean isEjecucion() { return f.canExecute(); }
    public boolean isAbsoluto() { return f.isAbsolute(); }
    public long getTamano() { return f.length(); }
    public String getRutaAbsoluta() { return f.getAbsolutePath(); }
    public String getPath() { return f.getPath(); }
    public String getParent() { return f.getParent(); }
    public String getUri() { return f.toURI().toString(); }
    public long getEspacioTotal() { return f.getTotalSpace(); }
    public long getEspacioLibre() { return f.getFreeSpace(); }
    public long getEspacioUsable() { return f.getUsableSpace(); }
    public String getEspacioTotalLegible() { return legible(f.getTotalSpace()); }
    public String getEspacioLibreLegible() { return legible(f.getFreeSpace()); }
    public String getEspacioUsableLegible() { return legible(f.getUsableSpace()); }

    public String getRutaCanonica() {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return "?";
        }
    }

    public String getUltimaModificacion() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(f.lastModified()));
    }

    // Igual que en el ejemplo de la práctica: "/" carpeta, "_" fichero, "?" otro
    public String getTipo() {
        return f.isDirectory() ? "/" : f.isFile() ? "_" : "?";
    }

    public String getPermisos() {
        return (f.canRead() ? "r" : "-") + (f.canWrite() ? "w" : "-") + (f.canExecute() ? "x" : "-");
    }

    public String getExtension() {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return (i > 0 && f.isFile()) ? n.substring(i + 1).toLowerCase() : "";
    }

    // Ficheros que se pueden abrir en el editor de texto
    public boolean isTexto() {
        return f.isFile() && java.util.List.of("txt", "csv", "md", "xml", "json", "html", "css", "js", "java",
                "properties", "log", "ini", "yml", "yaml", "sql", "").contains(getExtension());
    }

    public int getNumHijos() {
        String[] hijos = f.list();
        return hijos == null ? 0 : hijos.length;
    }

    public String getTamanoLegible() {
        if (f.isDirectory()) return getNumHijos() + " elementos";
        return legible(f.length());
    }

    public static String legible(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public int[] getVocales() { return vocales; }
    public void setVocales(int[] vocales) { this.vocales = vocales; }
    public int[] getVocalesFileReader() { return vocalesFileReader; }
    public void setVocalesFileReader(int[] vocalesFileReader) { this.vocalesFileReader = vocalesFileReader; }

    public int getTotalVocales() {
        if (vocales == null) return 0;
        int total = 0;
        for (int v : vocales) total += v;
        return total;
    }
}
