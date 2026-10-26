package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Pruebas de la lógica de cada práctica sobre una carpeta temporal
class FileDriveTests {

    @TempDir
    Path tmp;

    Drive drive;

    @BeforeEach
    void preparar() throws IOException {
        drive = new Drive(tmp.toString());
    }

    @Test
    void noSePuedeSalirDelDrive() {
        assertThrows(IOException.class, () -> drive.resolver("../fuera.txt"));
    }

    // Práctica 2
    @Test
    void estructuraFamiliaYBorradoRecursivo() throws IOException {
        List<String> creados = drive.crearEstructuraFamilia("");
        assertEquals(8, creados.size());
        assertTrue(creados.contains("Abuelo/Padre/Hijo1.txt"));
        assertTrue(creados.contains("Abuela/Madre/Hija8.txt"));

        assertEquals(List.of("Abuelo/Madre/Hija4.txt"), drive.buscarYBorrar("", "Hija4.txt"));
        assertFalse(new File(tmp.toFile(), "Abuelo/Madre/Hija4.txt").exists());

        drive.borrar("Abuela");
        assertFalse(new File(tmp.toFile(), "Abuela").exists());
        assertThrows(IOException.class, () -> drive.borrar(""));
    }

    // Prácticas 3 y 5
    @Test
    void vocalesYEscrituraSecuencial() throws IOException {
        drive.crearFichero("", "a.txt");
        drive.guardarTexto("a.txt", "canción\r\naeiou");
        drive.anadirFinal("a.txt", "FINAL");
        drive.anadirPrincipio("a.txt", "INICIO");
        assertEquals("INICIO\ncanción\naeiou\nFINAL\n", drive.leerTexto("a.txt"));

        File f = drive.resolver("a.txt");
        int[] esperado = { 3, 1, 6, 3, 1 };   // INICIO canción aeiou FINAL
        assertArrayEquals(esperado, drive.contarVocalesBufferedReader(f));
        assertArrayEquals(esperado, drive.contarVocalesFileReader(f));
    }

    // Práctica 4
    @Test
    void buscarTextoDaLineaYColumna() throws IOException {
        drive.crearFichero("", "b.txt");
        drive.guardarTexto("b.txt", "hola\nuna hola y otra hola\n");
        List<Coincidencia> r = drive.buscarTexto("", "hola", false);
        assertEquals(3, r.size());
        assertEquals(1, r.get(0).getLinea());
        assertEquals(1, r.get(0).getColumna());
        assertEquals(2, r.get(2).getLinea());
        assertEquals(17, r.get(2).getColumna());
    }

    // Prácticas 7 y 8
    @Test
    void ficheroAleatorio() throws IOException {
        FicheroAleatorio faa = FicheroAleatorio.personas(tmp.resolve("p.dat").toFile());
        assertEquals(77, faa.getLongReg());

        faa.insertar(Map.of("DNI", "11111111A", "NOMBRE", "José Núñez", "EDAD", "20", "FECHA_ALTA", "2026-09-15"));
        faa.insertar(Map.of("DNI", "22222222B", "NOMBRE", "Ana"), 3);
        assertEquals(4, faa.getNumReg());
        assertEquals(4 * 77, tmp.resolve("p.dat").toFile().length());

        assertEquals("José Núñez", faa.selectCampo(0, "NOMBRE"));
        assertEquals("2026-09-15", faa.selectCampo(0, "FECHA_ALTA"));
        assertEquals(List.of("11111111A", "", "", "22222222B"), faa.selectColumna("DNI"));
        assertEquals(List.of("11111111A", "José Núñez", "", "20", "2026-09-15"), faa.selectRowList(0));

        faa.update(0, "EDAD", "21");
        faa.update(3, Map.of("CIUDAD", "Granada", "EDAD", "30"));
        assertEquals("21", faa.selectRowMap(0).get("EDAD"));
        assertEquals("Granada", faa.selectCampo(3, "CIUDAD"));
        assertEquals("Ana", faa.selectCampo(3, "NOMBRE"));

        faa.delete(0);
        assertEquals("", faa.selectCampo(0, "DNI"));
        assertEquals(4, faa.getNumReg());

        // Un texto más largo que el campo se recorta sin pasarse de bytes
        faa.update(1, "DNI", "123456789012");
        assertEquals("123456789", faa.selectCampo(1, "DNI"));

        // Caracteres de 4 bytes en UTF-8 (emoji) no se parten por la mitad
        faa.update(1, "CIUDAD", "Granada 😀");
        assertEquals("Granada 😀", faa.selectCampo(1, "CIUDAD"));

        assertThrows(IOException.class, () -> faa.update(1, "EDAD", "abc"));
        assertThrows(IOException.class, () -> faa.selectRowMap(10));
    }

    // Práctica 9
    @Test
    void cambioDeEncoding() throws IOException {
        Files.writeString(tmp.resolve("e.txt"), "Ñandú\nacción\n", StandardCharsets.UTF_8);
        String r = drive.convertirEncoding("e.txt", "UTF-8", "out/e_latin1.txt", "ISO-8859-1");
        assertTrue(r.startsWith("Convertido"), r);
        // newLine() escribe el salto de línea del sistema (\n en Linux, \r\n en Windows)
        String esperado = "Ñandú" + System.lineSeparator() + "acción" + System.lineSeparator();
        assertEquals(esperado, Files.readString(tmp.resolve("out/e_latin1.txt"), StandardCharsets.ISO_8859_1));

        drive.convertirEncoding("out/e_latin1.txt", "ISO-8859-1", "e16.txt", "UTF-16");
        assertEquals(esperado, Files.readString(tmp.resolve("e16.txt"), StandardCharsets.UTF_16));

        assertTrue(drive.convertirEncoding("e.txt", "UTF-8", "e.txt", "ASCII").startsWith("ERROR"));
        assertTrue(drive.convertirEncoding("no.txt", "UTF-8", "x.txt", "ASCII").startsWith("ERROR"));
    }

    // Práctica 6
    @Test
    void sdmxCsv() throws IOException {
        Path csv = tmp.resolve("d.csv");
        Files.writeString(csv, """
                DATAFLOW,LAST UPDATE,freq,unit,geo,TIME_PERIOD,OBS_VALUE,OBS_FLAG
                ESTAT:X(1.0),01/01/26,A,"Euro, per unit",ES,2022,10.5,
                ESTAT:X(1.0),01/01/26,A,"Euro, per unit",ES,2023,12,
                ESTAT:X(1.0),01/01/26,A,"Euro, per unit",FR,2022,8,
                ESTAT:X(1.0),01/01/26,A,"Euro, per unit",FR,2023,,n
                """);
        SdmxCsv datos = new SdmxCsv(csv.toFile());
        assertEquals(4, datos.getFilas().size());
        assertEquals("Euro, per unit", datos.getFilas().get(0).get(3));
        assertEquals(2, datos.filtrar("es", "").size());
        assertEquals(2, datos.filtrar("", "2023").size());
        assertEquals(1, datos.filtrar("FR", "2022").size());
        assertEquals(List.of("ES", "ES", "FR", "FR"), datos.soloColumna(datos.getFilas(), "geo"));

        List<String[]> resumen = datos.resumen(datos.getFilas());
        assertArrayEquals(new String[] { "ES", "10.5", "12" }, resumen.get(0));
        assertArrayEquals(new String[] { "FR", "8", "8" }, resumen.get(1));
    }
}
