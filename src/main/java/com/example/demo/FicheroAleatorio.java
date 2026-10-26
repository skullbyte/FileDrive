package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Prácticas 7 y 8: fichero binario de registros de longitud FIJA con RandomAccessFile.
// Como cada registro ocupa siempre longReg bytes, el registro n empieza en n * longReg.
public class FicheroAleatorio {

    // Tipos de campo: texto (bytes UTF-8 rellenos con espacios), entero (int, 4 bytes)
    // y fecha (long con los días desde 1970, 8 bytes)
    public enum Tipo { TEXTO, ENTERO, FECHA }

    private final File f;
    private final List<String> campos;
    private final List<Integer> camposLength;
    private final List<Tipo> camposTipo;
    private final long longReg;   // Bytes por registro

    // Estructura de los registros que usa la web
    public static FicheroAleatorio personas(File f) {
        List<String> campos = List.of("DNI", "NOMBRE", "CIUDAD", "EDAD", "FECHA_ALTA");
        List<Integer> camposLength = List.of(9, 32, 24, 4, 8);
        List<Tipo> camposTipo = List.of(Tipo.TEXTO, Tipo.TEXTO, Tipo.TEXTO, Tipo.ENTERO, Tipo.FECHA);
        return new FicheroAleatorio(f, campos, camposLength, camposTipo);
    }

    public FicheroAleatorio(File f, List<String> campos, List<Integer> camposLength, List<Tipo> camposTipo) {
        this.f = f;
        this.campos = campos;
        this.camposLength = camposLength;
        this.camposTipo = camposTipo;
        long total = 0;
        for (Integer longitud : camposLength) {
            total += longitud;
        }
        this.longReg = total;
    }

    public List<String> getCampos() { return campos; }
    public long getLongReg() { return longReg; }

    // Número de registros que caben en el fichero según su tamaño
    public long getNumReg() {
        return f.exists() ? f.length() / longReg : 0;
    }

    // ---------------------------------------------------------------- INSERTAR

    // Añade el registro al final
    public long insertar(Map<String, String> reg) throws IOException {
        long pos = getNumReg();
        insertar(reg, pos);
        return pos;
    }

    // Escribe el registro completo en la posición indicada (si es mayor que el número
    // de registros el fichero crece y los huecos quedan como registros vacíos)
    public void insertar(Map<String, String> reg, long pos) throws IOException {
        if (pos < 0) throw new IOException("Posición negativa: " + pos);
        // Primero se codifica todo el registro, así si un campo es incorrecto no se escribe nada
        ByteBuffer registro = ByteBuffer.allocate((int) longReg);
        for (int i = 0; i < campos.size(); i++) {
            String valor = reg.get(campos.get(i));
            registro.put(codificar(i, valor == null ? "" : valor));
        }
        try (RandomAccessFile rndFile = new RandomAccessFile(f, "rws")) {
            rndFile.seek(pos * longReg);
            rndFile.write(registro.array());
        }
    }

    // ---------------------------------------------------------------- SELECT

    // 1) Valor de una columna de un registro
    public String selectCampo(int numRegistro, String nomColumna) throws IOException {
        int i = indiceCampo(nomColumna);
        comprobarFila(numRegistro);
        try (RandomAccessFile rndFile = new RandomAccessFile(f, "r")) {
            if (registroVacio(rndFile, numRegistro)) return "";
            rndFile.seek(numRegistro * longReg + offset(i));
            byte[] bytes = new byte[camposLength.get(i)];
            rndFile.readFully(bytes);
            return decodificar(i, bytes);
        }
    }

    // 2) SELECT columna FROM fichero
    public List<String> selectColumna(String nomColumna) throws IOException {
        List<String> valores = new ArrayList<>();
        for (int fila = 0; fila < getNumReg(); fila++) {
            valores.add(selectCampo(fila, nomColumna));
        }
        return valores;
    }

    // 3) Datos de un registro en una lista (en el orden de los campos)
    public List<String> selectRowList(int numRegistro) throws IOException {
        return new ArrayList<>(selectRowMap(numRegistro).values());
    }

    // 4) Datos de un registro en un Map campo → valor
    public Map<String, String> selectRowMap(int numRegistro) throws IOException {
        comprobarFila(numRegistro);
        Map<String, String> reg = new LinkedHashMap<>();
        try (RandomAccessFile rndFile = new RandomAccessFile(f, "r")) {
            boolean vacio = registroVacio(rndFile, numRegistro);
            rndFile.seek(numRegistro * longReg);
            for (int i = 0; i < campos.size(); i++) {
                byte[] bytes = new byte[camposLength.get(i)];
                rndFile.readFully(bytes);
                reg.put(campos.get(i), vacio ? "" : decodificar(i, bytes));
            }
        }
        return reg;
    }

    // ---------------------------------------------------------------- UPDATE / DELETE

    // 5.1) Modifica todos los campos del registro que vengan en el Map
    public void update(int row, Map<String, String> valores) throws IOException {
        for (Map.Entry<String, String> e : valores.entrySet()) {
            update(row, e.getKey(), e.getValue());
        }
    }

    // 5.2) Modifica SOLO un campo: nos posicionamos en el inicio de ese campo y lo escribimos
    public void update(int row, String campo, String valor) throws IOException {
        int i = indiceCampo(campo);
        comprobarFila(row);
        byte[] bytes = codificar(i, valor == null ? "" : valor);
        try (RandomAccessFile rndFile = new RandomAccessFile(f, "rws")) {
            if (registroVacio(rndFile, row)) {
                // Un registro borrado tiene todo a 0: primero lo inicializamos vacío
                rndFile.seek(row * longReg);
                for (int j = 0; j < campos.size(); j++) {
                    rndFile.write(codificar(j, ""));
                }
            }
            rndFile.seek(row * longReg + offset(i));
            rndFile.write(bytes);
        }
    }

    // 6) Limpia el registro (todo a ceros); el resto de registros no se mueve
    public void delete(int row) throws IOException {
        comprobarFila(row);
        try (RandomAccessFile rndFile = new RandomAccessFile(f, "rws")) {
            rndFile.seek(row * longReg);
            rndFile.write(new byte[(int) longReg]);
        }
    }

    // ---------------------------------------------------------------- AUXILIARES

    private int indiceCampo(String nombre) throws IOException {
        int i = campos.indexOf(nombre);
        if (i < 0) throw new IOException("No existe la columna " + nombre);
        return i;
    }

    private void comprobarFila(long fila) throws IOException {
        if (fila < 0 || fila >= getNumReg()) {
            throw new IOException("No existe el registro " + fila + " (hay " + getNumReg() + ")");
        }
    }

    // Bytes desde el inicio del registro hasta el campo i
    private long offset(int i) {
        long off = 0;
        for (int j = 0; j < i; j++) off += camposLength.get(j);
        return off;
    }

    private boolean registroVacio(RandomAccessFile rndFile, long fila) throws IOException {
        byte[] bytes = new byte[(int) longReg];
        rndFile.seek(fila * longReg);
        rndFile.readFully(bytes);
        for (byte b : bytes) {
            if (b != 0) return false;
        }
        return true;
    }

    // Pasa el valor (String) a los bytes que ocupa el campo i en el fichero
    private byte[] codificar(int i, String valor) throws IOException {
        int longitud = camposLength.get(i);
        ByteBuffer buf = ByteBuffer.allocate(longitud);
        valor = valor.trim();
        try {
            switch (camposTipo.get(i)) {
                case ENTERO:
                    buf.putInt(valor.isEmpty() ? 0 : Integer.parseInt(valor));
                    break;
                case FECHA:
                    buf.putLong(valor.isEmpty() ? Long.MIN_VALUE : LocalDate.parse(valor).toEpochDay());
                    break;
                default:
                    // Se copian caracteres mientras quepan (sin partir una letra UTF-8 por la mitad)
                    for (int c = 0; c < valor.length(); c++) {
                        byte[] letra = String.valueOf(valor.charAt(c)).getBytes(StandardCharsets.UTF_8);
                        if (buf.remaining() < letra.length) break;
                        buf.put(letra);
                    }
                    while (buf.hasRemaining()) buf.put((byte) ' ');
            }
        } catch (NumberFormatException e) {
            throw new IOException(campos.get(i) + " debe ser un número entero: " + valor);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IOException(campos.get(i) + " debe ser una fecha AAAA-MM-DD: " + valor);
        }
        return buf.array();
    }

    private String decodificar(int i, byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        switch (camposTipo.get(i)) {
            case ENTERO:
                return String.valueOf(buf.getInt());
            case FECHA:
                long dias = buf.getLong();
                return dias == Long.MIN_VALUE ? "" : LocalDate.ofEpochDay(dias).toString();
            default:
                return new String(bytes, StandardCharsets.UTF_8).replace("\0", "").trim();
        }
    }
}
