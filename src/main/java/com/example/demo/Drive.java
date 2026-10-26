package com.example.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Drive {

    // Carpeta raíz del drive: todo lo que se ve en la web está dentro de ella
    private final File raiz;

    public Drive(@Value("${filedrive.raiz:drive}") String rutaRaiz) throws IOException {
        this.raiz = new File(rutaRaiz).getCanonicalFile();
        if (!raiz.exists()) {
            raiz.mkdirs();
        }
    }

    public File getRaiz() {
        return raiz;
    }

    // Convierte una ruta relativa de la web ("docs/a.txt") en un File dentro de la raíz.
    // Si alguien intenta salir con "../" se lanza una excepción.
    public File resolver(String ruta) throws IOException {
        if (ruta == null) ruta = "";
        File f = new File(raiz, ruta).getCanonicalFile();
        if (!f.toPath().startsWith(raiz.toPath())) {
            throw new IOException("Ruta fuera del drive: " + ruta);
        }
        return f;
    }

    // Ruta relativa a la raíz con "/" como separador (la que usa la web)
    public String relativa(File f) {
        return raiz.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
    }

    // Ejemplo de función para la Práctica 1 (convierte texto a mayúsculas)
    public String procesarPractica1(String texto) {
        if (texto == null) return "";
        return texto.toUpperCase();
    }

    // ===================== PRÁCTICAS 1 y 4: EXPLORADOR =====================

    // Lista el contenido de una carpeta: primero carpetas y luego ficheros, por nombre
    public List<FicheroInfo> listar(String ruta) throws IOException {
        File carpeta = resolver(ruta);
        if (!carpeta.isDirectory()) {
            throw new IOException("No es un directorio: " + ruta);
        }
        File[] ficheros = carpeta.listFiles();
        if (ficheros == null) ficheros = new File[0];
        Arrays.sort(ficheros, Comparator.comparing((File f) -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));

        List<FicheroInfo> lista = new ArrayList<>();
        for (File f : ficheros) {
            FicheroInfo info = new FicheroInfo(f, relativa(f));
            // Práctica 3: conteo de vocales de cada fichero de texto (si no es enorme)
            if (info.isTexto() && f.length() <= 1024 * 1024) {
                info.setVocales(contarVocalesBufferedReader(f));
                info.setVocalesFileReader(contarVocalesFileReader(f));
            }
            lista.add(info);
        }
        return lista;
    }

    // Práctica 1: toda la información de un fichero con los métodos de java.io.File
    public FicheroInfo info(String ruta) throws IOException {
        File f = resolver(ruta);
        if (!f.exists()) {
            throw new FileNotFoundException("No existe el fichero o directorio " + ruta);
        }
        FicheroInfo info = new FicheroInfo(f, relativa(f));
        // Las vocales solo se cuentan en ficheros que no sean enormes
        if (f.isFile() && f.length() <= 10 * 1024 * 1024) {
            info.setVocales(contarVocalesBufferedReader(f));
            info.setVocalesFileReader(contarVocalesFileReader(f));
        }
        return info;
    }

    // Rutas de todos los ficheros del drive que acaban en alguna de las extensiones
    // (sin extensiones devuelve todos). Se usa para los desplegables de la web.
    public List<String> ficherosPorExtension(String... extensiones) {
        List<String> rutas = new ArrayList<>();
        recorrer(raiz, rutas, extensiones);
        rutas.sort(String.CASE_INSENSITIVE_ORDER);
        return rutas;
    }

    private void recorrer(File dir, List<String> rutas, String[] extensiones) {
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            if (f.isDirectory()) {
                recorrer(f, rutas, extensiones);
            } else if (extensiones.length == 0) {
                rutas.add(relativa(f));
            } else {
                for (String ext : extensiones) {
                    if (f.getName().toLowerCase().endsWith(ext)) {
                        rutas.add(relativa(f));
                        break;
                    }
                }
            }
        }
    }

    // ===================== PRÁCTICA 2: CREACIÓN Y BORRADO =====================

    public File crearFichero(String carpeta, String nombre) throws IOException {
        File f = resolver(carpeta + "/" + nombre);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        if (!f.createNewFile()) {
            throw new IOException("Ya existe: " + nombre);
        }
        return f;
    }

    public File crearCarpeta(String carpeta, String nombre) throws IOException {
        File f = resolver(carpeta + "/" + nombre);
        if (f.exists()) {
            throw new IOException("Ya existe: " + nombre);
        }
        if (!f.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + nombre);
        }
        return f;
    }

    // Crea la estructura Abuelo|Abuela / Padre|Madre / HijoN.txt|HijaN.txt mediante bucles
    public List<String> crearEstructuraFamilia(String carpeta) throws IOException {
        String[] abuelos = { "Abuelo", "Abuela" };
        String[] padres = { "Padre", "Madre" };
        List<String> creados = new ArrayList<>();
        int n = 1;
        for (String abuelo : abuelos) {
            for (String padre : padres) {
                File dir = resolver(carpeta + "/" + abuelo + "/" + padre);
                dir.mkdirs();
                for (int i = 0; i < 2; i++) {
                    String nombre = (n % 2 == 1 ? "Hijo" : "Hija") + n + ".txt";
                    File f = new File(dir, nombre);
                    f.createNewFile();
                    creados.add(relativa(f));
                    n++;
                }
            }
        }
        return creados;
    }

    // Borra un fichero o una carpeta con todo su contenido
    public void borrar(String ruta) throws IOException {
        File f = resolver(ruta);
        if (f.equals(raiz)) {
            throw new IOException("No se puede borrar la raíz del drive");
        }
        if (!f.exists()) {
            throw new FileNotFoundException("No existe: " + ruta);
        }
        borrarRecursivo(f);
    }

    private void borrarRecursivo(File f) throws IOException {
        if (f.isDirectory()) {
            File[] hijos = f.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) {
                    borrarRecursivo(hijo);
                }
            }
        }
        if (!f.delete()) {
            throw new IOException("No se pudo borrar " + relativa(f));
        }
    }

    // Recorre recursivamente una carpeta buscando ficheros con ese nombre y los elimina
    public List<String> buscarYBorrar(String carpeta, String nombre) throws IOException {
        List<String> borrados = new ArrayList<>();
        buscarYBorrarRecursivo(resolver(carpeta), nombre, borrados);
        return borrados;
    }

    private void buscarYBorrarRecursivo(File dir, String nombre, List<String> borrados) {
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            if (f.isDirectory()) {
                buscarYBorrarRecursivo(f, nombre, borrados);
            } else if (f.getName().equals(nombre)) {
                String ruta = relativa(f);
                if (f.delete()) {
                    borrados.add(ruta);
                }
            }
        }
    }

    // ===================== PRÁCTICA 3: LECTURA SECUENCIAL =====================

    // Posición de cada vocal (también con tilde) en el array a, e, i, o, u
    private int indiceVocal(int c) {
        switch (Character.toLowerCase(c)) {
            case 'a': case 'á': case 'à': case 'ä': return 0;
            case 'e': case 'é': case 'è': case 'ë': return 1;
            case 'i': case 'í': case 'ì': case 'ï': return 2;
            case 'o': case 'ó': case 'ò': case 'ö': return 3;
            case 'u': case 'ú': case 'ù': case 'ü': return 4;
            default: return -1;
        }
    }

    // Implementación 1: FileReader leyendo carácter a carácter con read()
    public int[] contarVocalesFileReader(File f) throws IOException {
        int[] vocales = new int[5];
        try (FileReader fil = new FileReader(f)) {
            int letra = fil.read();
            while (letra != -1) {
                int i = indiceVocal(letra);
                if (i >= 0) vocales[i]++;
                letra = fil.read();
            }
        }
        return vocales;
    }

    // Implementación 2: BufferedReader leyendo línea a línea con readLine()
    public int[] contarVocalesBufferedReader(File f) throws IOException {
        int[] vocales = new int[5];
        try (BufferedReader brf = new BufferedReader(new FileReader(f))) {
            String line = brf.readLine();
            while (line != null) {
                for (int j = 0; j < line.length(); j++) {
                    int i = indiceVocal(line.charAt(j));
                    if (i >= 0) vocales[i]++;
                }
                line = brf.readLine();
            }
        }
        return vocales;
    }

    // ===================== PRÁCTICA 4: BÚSQUEDA DE TEXTO =====================

    // Busca un texto en los ficheros de una carpeta (y subcarpetas si se pide)
    // y devuelve la línea y columna de cada aparición
    public List<Coincidencia> buscarTexto(String carpeta, String texto, boolean recursivo) throws IOException {
        List<Coincidencia> resultado = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return resultado;
        buscarTextoEn(resolver(carpeta), texto, recursivo, resultado);
        return resultado;
    }

    private void buscarTextoEn(File dir, String texto, boolean recursivo, List<Coincidencia> resultado) throws IOException {
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        Arrays.sort(hijos);
        for (File f : hijos) {
            if (f.isDirectory()) {
                if (recursivo) buscarTextoEn(f, texto, true, resultado);
                continue;
            }
            // Los ficheros binarios (.dat, imágenes...) no se leen como texto
            if (!FicheroInfo.esTexto(f)) continue;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea = br.readLine();
                int numLinea = 1;
                while (linea != null) {
                    int col = linea.indexOf(texto);
                    while (col != -1) {
                        // Columnas empezando en 1, como en un editor
                        resultado.add(new Coincidencia(relativa(f), numLinea, col + 1, linea));
                        col = linea.indexOf(texto, col + 1);
                    }
                    linea = br.readLine();
                    numLinea++;
                }
            }
        }
    }

    // ===================== PRÁCTICA 5: ESCRITURA SECUENCIAL =====================

    public String leerTexto(String ruta) throws IOException {
        File f = resolver(ruta);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea = br.readLine();
            while (linea != null) {
                sb.append(linea).append('\n');
                linea = br.readLine();
            }
        }
        return sb.toString();
    }

    // 1º CREAR / guardar: sobrescribe el fichero con el contenido indicado
    public void guardarTexto(String ruta, String contenido) throws IOException {
        File f = resolver(ruta);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(normalizar(contenido));
        }
    }

    // 2º AÑADIR FINAL: FileWriter en modo append
    public void anadirFinal(String ruta, String texto) throws IOException {
        File f = resolver(ruta);
        if (!f.exists()) throw new FileNotFoundException("No existe: " + ruta);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
            // Si la última línea no acaba en salto de línea, lo ponemos para no pegar el texto a ella
            if (!terminaEnSaltoDeLinea(f)) bw.newLine();
            bw.write(normalizar(texto));
            bw.newLine();
        }
    }

    // 3º AÑADIR PRINCIPIO: se lee todo, se escribe el texto nuevo y después lo anterior
    public void anadirPrincipio(String ruta, String texto) throws IOException {
        File f = resolver(ruta);
        if (!f.exists()) throw new FileNotFoundException("No existe: " + ruta);
        String anterior = leerTexto(ruta);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(normalizar(texto));
            bw.newLine();
            bw.write(anterior);
        }
    }

    private boolean terminaEnSaltoDeLinea(File f) throws IOException {
        if (f.length() == 0) return true;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            raf.seek(f.length() - 1);
            return raf.read() == '\n';
        }
    }

    // Los textarea del navegador envían saltos de línea \r\n
    private String normalizar(String texto) {
        return texto == null ? "" : texto.replace("\r\n", "\n");
    }

    // ===================== PRÁCTICA 9: ENCODING DE FICHEROS =====================

    public static final List<String> ENCODINGS = List.of("ASCII", "UTF-8", "UTF-16", "ISO-8859-1");

    // Lee el fichero con un encoding y lo escribe en otro fichero con otro encoding.
    // Devuelve un mensaje con el resultado (las excepciones se capturan aquí).
    public String convertirEncoding(String entrada, String encEntrada, String salida, String encSalida) {
        try {
            File fEntrada = resolver(entrada);
            File fSalida = resolver(salida);
            // Si fueran el mismo, FileOutputStream lo vaciaría antes de leerlo
            if (fEntrada.equals(fSalida)) {
                return "ERROR: el fichero de salida debe ser distinto del de entrada";
            }
            if (fSalida.getParentFile() != null) fSalida.getParentFile().mkdirs();
            int lineas = 0;
            try (BufferedReader br = new BufferedReader(
                         new InputStreamReader(new FileInputStream(fEntrada), charset(encEntrada)));
                 BufferedWriter bw = new BufferedWriter(
                         new OutputStreamWriter(new FileOutputStream(fSalida), charset(encSalida)))) {
                String linea = br.readLine();
                while (linea != null) {
                    bw.write(linea);
                    bw.newLine();
                    lineas++;
                    linea = br.readLine();
                }
            }
            return "Convertido " + entrada + " (" + encEntrada + ") → " + salida + " (" + encSalida + "): "
                    + lineas + " líneas, " + fEntrada.length() + " → " + fSalida.length() + " bytes";
        } catch (FileNotFoundException e) {
            return "ERROR fichero no encontrado: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR de entrada/salida: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // "ASCII" en Java se llama "US-ASCII"
    private String charset(String enc) {
        if (!ENCODINGS.contains(enc)) throw new IllegalArgumentException("Encoding no permitido: " + enc);
        return enc.equals("ASCII") ? "US-ASCII" : enc;
    }
}
