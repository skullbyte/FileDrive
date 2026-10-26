package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Práctica 6: lectura y filtrado de un fichero SDMX-CSV descargado de EUROSTAT.
// La primera línea tiene el nombre de las columnas (DATAFLOW, freq, unit, geo, TIME_PERIOD, OBS_VALUE...)
public class SdmxCsv {

    private final List<String> cabecera;
    private final List<List<String>> filas = new ArrayList<>();

    public SdmxCsv(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String linea = br.readLine();
            if (linea == null) throw new IOException("El fichero está vacío");
            // Quitamos el BOM si lo trae
            if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
            cabecera = separar(linea);
            linea = br.readLine();
            while (linea != null) {
                if (!linea.isBlank()) filas.add(separar(linea));
                linea = br.readLine();
            }
        }
    }

    public List<String> getCabecera() { return cabecera; }
    public List<List<String>> getFilas() { return filas; }

    // Separa una línea CSV por comas respetando los valores entre comillas
    // (con etiquetas, EUROSTAT pone cosas como "ES:Spain" o "Euro, per kilogram")
    static List<String> separar(String linea) {
        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (comillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    comillas = !comillas;
                }
            } else if (c == ',' && !comillas) {
                valores.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        valores.add(actual.toString().trim());
        return valores;
    }

    public int columna(String nombre) {
        for (int i = 0; i < cabecera.size(); i++) {
            if (cabecera.get(i).equalsIgnoreCase(nombre)) return i;
        }
        return -1;
    }

    private String valor(List<String> fila, int col) {
        return (col >= 0 && col < fila.size()) ? fila.get(col) : "";
    }

    // A y B: filas del país indicado (columna geo) y/o con la fecha indicada (columna TIME_PERIOD).
    // Si un filtro va vacío no se aplica.
    public List<List<String>> filtrar(String pais, String fecha) {
        int colGeo = columna("geo");
        int colFecha = columna("TIME_PERIOD");
        List<List<String>> resultado = new ArrayList<>();
        for (List<String> fila : filas) {
            if (pais != null && !pais.isBlank()) {
                String geo = valor(fila, colGeo).toLowerCase();
                String buscado = pais.trim().toLowerCase();
                // Vale el código (ES) o el nombre si el fichero trae etiquetas (ES:Spain)
                if (!geo.equals(buscado) && !geo.startsWith(buscado + ":") && !geo.contains(":" + buscado)) {
                    continue;
                }
            }
            if (fecha != null && !fecha.isBlank() && !valor(fila, colFecha).startsWith(fecha.trim())) {
                continue;
            }
            resultado.add(fila);
        }
        return resultado;
    }

    // C: de las filas indicadas, solo la columna pedida
    public List<String> soloColumna(List<List<String>> filas, String nombreColumna) {
        int col = columna(nombreColumna);
        List<String> valores = new ArrayList<>();
        if (col < 0) return valores;
        for (List<String> fila : filas) {
            valores.add(valor(fila, col));
        }
        return valores;
    }

    // D: tabla resumen PAÍS; valor mínimo; valor máximo (de la columna OBS_VALUE)
    public List<String[]> resumen(List<List<String>> filas) {
        int colGeo = columna("geo");
        int colValor = columna("OBS_VALUE");
        Map<String, double[]> minMax = new TreeMap<>();
        for (List<String> fila : filas) {
            String v = valor(fila, colValor);
            if (v.isEmpty()) continue;
            double num;
            try {
                num = Double.parseDouble(v);
            } catch (NumberFormatException e) {
                continue;
            }
            double[] mm = minMax.get(valor(fila, colGeo));
            if (mm == null) {
                minMax.put(valor(fila, colGeo), new double[] { num, num });
            } else {
                mm[0] = Math.min(mm[0], num);
                mm[1] = Math.max(mm[1], num);
            }
        }
        List<String[]> resumen = new ArrayList<>();
        for (Map.Entry<String, double[]> e : minMax.entrySet()) {
            resumen.add(new String[] { e.getKey(), numero(e.getValue()[0]), numero(e.getValue()[1]) });
        }
        return resumen;
    }

    private String numero(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
