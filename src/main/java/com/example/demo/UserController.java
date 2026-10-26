package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    // 4) Se inyecta la clase Drive que tiene la lógica de ejecución
    @Autowired
    private Drive drive;

    @GetMapping("/pruebas")
    public String pruebas( Model model )
    {
        // LE PASAMOS UN MODELO ( DATOS ) AL TEMPLATE
        model.addAttribute("title", "Título");
        model.addAttribute("nombre", "Pepe");
        model.addAttribute("apellido", "Gomez");

        return "pruebas";
    }

    // 1) Endpoints para la Práctica 1 (GET para mostrar el formulario)
    @GetMapping("/practica1")
    public String mostrarFormularioPractica1(Model model) {
        // 2) Pasamos la Clase Modelo vacía al HTML
        model.addAttribute("datos", new Practica1Model());

        // 3) GET muestra el FORM
        return "practica1_form";
    }

    // 1) Endpoints para la Práctica 1 (POST para procesar y mostrar el resultado)
    @PostMapping("/practica1")
    public String procesarPractica1(@ModelAttribute("datos") Practica1Model datos, Model model) {

        // 4) Desde los métodos POST se usan las funciones de Drive.java
        String resultado = drive.procesarPractica1(datos.getTextoEntrada());

        // Guardamos el resultado en nuestro objeto modelo
        datos.setTextoSalida(resultado);
        model.addAttribute("datos", datos);

        // 3) POST muestra el RESULTADO
        return "practica1_result";
    }

    // =====================================================================
    //  FILEDRIVE
    // =====================================================================

    // ---------- 1. Explorador / navegador de ficheros remoto (prácticas 1, 3 y 4)

    @GetMapping("/drive")
    public String explorar(@RequestParam(defaultValue = "") String ruta, Model model) {
        try {
            model.addAttribute("ficheros", drive.listar(ruta));
        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("ficheros", new ArrayList<FicheroInfo>());
            ruta = "";
        }
        model.addAttribute("ruta", ruta);
        model.addAttribute("padre", ruta.contains("/") ? ruta.substring(0, ruta.lastIndexOf('/')) : "");
        model.addAttribute("migas", migas(ruta));

        CrearModel crear = new CrearModel();
        crear.setCarpeta(ruta);
        model.addAttribute("crear", crear);
        BorrarModel borrar = new BorrarModel();
        borrar.setCarpeta(ruta);
        model.addAttribute("borrar", borrar);
        BuscarModel buscar = new BuscarModel();
        buscar.setCarpeta(ruta);
        model.addAttribute("buscar", buscar);
        return "drive";
    }

    // Práctica 1: toda la información de java.io.File de un fichero o carpeta
    @GetMapping("/drive/info")
    public String info(@RequestParam String ruta, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("info", drive.info(ruta));
            model.addAttribute("migas", migas(ruta.contains("/") ? ruta.substring(0, ruta.lastIndexOf('/')) : ""));
            return "info";
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/drive";
        }
    }

    // Descargar un fichero del drive
    @GetMapping("/drive/descargar")
    public ResponseEntity<FileSystemResource> descargar(@RequestParam String ruta) {
        File f;
        try {
            f = drive.resolver(ruta);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
        if (!f.isFile()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.getName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(f));
    }

    // Subir un fichero a la carpeta actual
    @PostMapping("/drive/subir")
    public String subir(@RequestParam String carpeta, @RequestParam("fichero") MultipartFile fichero, RedirectAttributes flash) {
        try {
            if (fichero.isEmpty() || fichero.getOriginalFilename() == null) throw new IOException("No se ha elegido ningún fichero");
            File destino = drive.resolver(carpeta + "/" + new File(fichero.getOriginalFilename()).getName());
            fichero.transferTo(destino);
            flash.addFlashAttribute("mensaje", "Subido " + drive.relativa(destino));
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigir(carpeta);
    }

    // ---------- 2. Creación de ficheros (prácticas 2 y 5)

    @PostMapping("/drive/crear")
    public String crear(@ModelAttribute CrearModel crear, RedirectAttributes flash) {
        try {
            if (crear.getNombre() == null || crear.getNombre().isBlank()) throw new IOException("Falta el nombre");
            if ("carpeta".equals(crear.getTipo())) {
                File f = drive.crearCarpeta(crear.getCarpeta(), crear.getNombre().trim());
                flash.addFlashAttribute("mensaje", "Carpeta creada: " + drive.relativa(f));
            } else {
                // Práctica 5: si no se indica extensión se crea un .txt
                String nombre = crear.getNombre().trim();
                if (!nombre.contains(".")) nombre += ".txt";
                File f = drive.crearFichero(crear.getCarpeta(), nombre);
                if (crear.getContenido() != null && !crear.getContenido().isEmpty()) {
                    drive.guardarTexto(drive.relativa(f), crear.getContenido());
                }
                flash.addFlashAttribute("mensaje", "Fichero creado: " + drive.relativa(f));
            }
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigir(crear.getCarpeta());
    }

    // Práctica 2: estructura Abuelo/Abuela - Padre/Madre - Hijos creada con bucles
    @PostMapping("/drive/familia")
    public String familia(@RequestParam String carpeta, RedirectAttributes flash) {
        try {
            List<String> creados = drive.crearEstructuraFamilia(carpeta);
            flash.addFlashAttribute("mensaje", "Estructura creada (" + creados.size() + " ficheros): " + String.join(", ", creados));
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigir(carpeta);
    }

    // ---------- 3. Eliminación de ficheros (práctica 2)

    // El formulario envía _method=delete (HiddenHttpMethodFilter, como en el Ejemplo 4 DELETE MAPPING)
    @DeleteMapping("/drive")
    public String borrar(@RequestParam String ruta, RedirectAttributes flash) {
        String carpeta = ruta.contains("/") ? ruta.substring(0, ruta.lastIndexOf('/')) : "";
        try {
            drive.borrar(ruta);
            flash.addFlashAttribute("mensaje", "Borrado: " + ruta);
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigir(carpeta);
    }

    // Práctica 2: recorrer recursivamente buscando un fichero por nombre y eliminarlo
    @PostMapping("/drive/borrar-nombre")
    public String borrarPorNombre(@ModelAttribute BorrarModel borrar, RedirectAttributes flash) {
        try {
            List<String> borrados = drive.buscarYBorrar(borrar.getCarpeta(), borrar.getNombre());
            if (borrados.isEmpty()) {
                flash.addFlashAttribute("error", "No se ha encontrado ningún fichero llamado " + borrar.getNombre());
            } else {
                flash.addFlashAttribute("mensaje", "Borrados: " + String.join(", ", borrados));
            }
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigir(borrar.getCarpeta());
    }

    // ---------- Práctica 4: búsqueda de texto (línea y columna)

    @GetMapping("/buscar")
    public String buscarSinDatos() {
        return "redirect:/drive";
    }

    @PostMapping("/buscar")
    public String buscar(@ModelAttribute BuscarModel buscar, Model model) {
        try {
            model.addAttribute("resultados", drive.buscarTexto(buscar.getCarpeta(), buscar.getTexto(), buscar.isRecursivo()));
        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("buscar", buscar);
        return "buscar";
    }

    // ---------- 4. Edición de ficheros de texto (prácticas 3 y 5)

    @GetMapping("/editor")
    public String editor(@RequestParam String ruta, Model model, RedirectAttributes flash) {
        try {
            if (drive.resolver(ruta).length() > MAX_EDITOR) {
                throw new IOException("El fichero es demasiado grande para el editor (máx. 2 MB): descárgalo");
            }
            EditorModel editor = new EditorModel();
            editor.setRuta(ruta);
            editor.setContenido(drive.leerTexto(ruta));
            model.addAttribute("editor", editor);
            model.addAttribute("info", drive.info(ruta));
            model.addAttribute("migas", migas(ruta.contains("/") ? ruta.substring(0, ruta.lastIndexOf('/')) : ""));
            return "editor";
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/drive";
        }
    }

    private static final long MAX_EDITOR = 2 * 1024 * 1024;

    // accion = guardar | final | principio
    @PostMapping("/editor")
    public String guardarEditor(@ModelAttribute EditorModel editor, @RequestParam String accion, RedirectAttributes flash) {
        try {
            switch (accion) {
                case "final":
                    drive.anadirFinal(editor.getRuta(), editor.getTexto());
                    flash.addFlashAttribute("mensaje", "Texto añadido al final");
                    break;
                case "principio":
                    drive.anadirPrincipio(editor.getRuta(), editor.getTexto());
                    flash.addFlashAttribute("mensaje", "Texto añadido al principio");
                    break;
                default:
                    drive.guardarTexto(editor.getRuta(), editor.getContenido());
                    flash.addFlashAttribute("mensaje", "Fichero guardado");
            }
        } catch (IOException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        flash.addAttribute("ruta", editor.getRuta());
        return "redirect:/editor";
    }

    // ---------- Práctica 6: editor SDMX-CSV (EUROSTAT)

    @GetMapping("/csv")
    public String csvForm(@RequestParam(required = false) String fichero, Model model) {
        CsvModel csv = new CsvModel();
        csv.setFichero(fichero);
        return csvResultado(csv, model);
    }

    @PostMapping("/csv")
    public String csvFiltrar(@ModelAttribute CsvModel csv, Model model) {
        return csvResultado(csv, model);
    }

    private String csvResultado(CsvModel csv, Model model) {
        List<String> ficheros = drive.ficherosPorExtension(".csv");
        if ((csv.getFichero() == null || csv.getFichero().isBlank()) && !ficheros.isEmpty()) {
            csv.setFichero(ficheros.get(0));
        }
        model.addAttribute("ficherosCsv", ficheros);
        model.addAttribute("csv", csv);
        if (csv.getFichero() == null || csv.getFichero().isBlank()) {
            return "csv";
        }
        try {
            SdmxCsv datos = new SdmxCsv(drive.resolver(csv.getFichero()));
            List<List<String>> filas = datos.filtrar(csv.getPais(), csv.getFecha());
            model.addAttribute("cabecera", datos.getCabecera());
            model.addAttribute("totalFilas", datos.getFilas().size());
            model.addAttribute("numFiltradas", filas.size());
            model.addAttribute("resumen", datos.resumen(filas));
            if (csv.getColumna() != null && !csv.getColumna().isBlank()) {
                if (datos.columna(csv.getColumna()) < 0) {
                    model.addAttribute("error", "No existe la columna " + csv.getColumna());
                } else {
                    model.addAttribute("valoresColumna", limitar(datos.soloColumna(filas, csv.getColumna())));
                }
            }
            model.addAttribute("filas", limitar(filas));
        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "csv";
    }

    // Para no mandar al navegador tablas enormes
    private <T> List<T> limitar(List<T> lista) {
        return lista.size() > 500 ? lista.subList(0, 500) : lista;
    }

    // ---------- 5. Edición de ficheros de acceso aleatorio (prácticas 7 y 8)

    @GetMapping("/aleatorio")
    public String aleatorio(@RequestParam(defaultValue = "personas.dat") String fichero, Model model) {
        RegistroModel reg = new RegistroModel();
        reg.setFichero(fichero);
        return aleatorioResultado(reg, model);
    }

    @PostMapping("/aleatorio")
    public String aleatorioAccion(@ModelAttribute RegistroModel reg, Model model) {
        try {
            FicheroAleatorio faa = abrirAleatorio(reg.getFichero());
            Map<String, String> datos = datosRegistro(reg);
            int fila = reg.getFila() == null ? -1 : reg.getFila();
            if (reg.getAccion() == null) throw new IOException("Falta la acción");
            switch (reg.getAccion()) {
                case "insertar":
                    if (reg.getFila() == null) {
                        long pos = faa.insertar(datos);
                        model.addAttribute("mensaje", "insertar(reg) → registro " + pos);
                    } else {
                        // Se permiten huecos, pero no tan grandes como para crear un fichero gigante
                        if (fila > faa.getNumReg() + 1000) {
                            throw new IOException("La fila " + fila + " está demasiado lejos del final (hay " + faa.getNumReg() + " registros)");
                        }
                        faa.insertar(datos, fila);
                        model.addAttribute("mensaje", "insertar(reg, " + fila + ")");
                    }
                    break;
                case "update":
                    // Solo se cambian los campos que se han rellenado
                    datos.values().removeIf(String::isBlank);
                    if (datos.isEmpty()) throw new IOException("Rellena algún campo para modificar");
                    faa.update(fila, datos);
                    model.addAttribute("mensaje", "update(" + fila + ", " + datos + ")");
                    break;
                case "updateCampo":
                    faa.update(fila, reg.getCampo(), reg.getValor());
                    model.addAttribute("mensaje", "update(" + fila + ", \"" + reg.getCampo() + "\", \"" + reg.getValor() + "\")");
                    break;
                case "delete":
                    faa.delete(fila);
                    model.addAttribute("mensaje", "delete(" + fila + ")");
                    break;
                case "selectCampo":
                    model.addAttribute("consulta", "selectCampo(" + fila + ", \"" + reg.getCampo() + "\")");
                    model.addAttribute("resultado", "\"" + faa.selectCampo(fila, reg.getCampo()) + "\"");
                    break;
                case "selectColumna":
                    model.addAttribute("consulta", "selectColumna(\"" + reg.getCampo() + "\")");
                    model.addAttribute("resultado", faa.selectColumna(reg.getCampo()).toString());
                    break;
                case "selectRowList":
                    model.addAttribute("consulta", "selectRowList(" + fila + ")");
                    model.addAttribute("resultado", faa.selectRowList(fila).toString());
                    break;
                case "selectRowMap":
                    model.addAttribute("consulta", "selectRowMap(" + fila + ")");
                    model.addAttribute("resultado", faa.selectRowMap(fila).toString());
                    break;
                default:
                    throw new IOException("Acción desconocida: " + reg.getAccion());
            }
        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
            model.asMap().remove("consulta");
        }
        return aleatorioResultado(reg, model);
    }

    // Solo se permiten ficheros .dat para no estropear un fichero de texto escribiendo registros
    private FicheroAleatorio abrirAleatorio(String fichero) throws IOException {
        if (fichero == null || !fichero.endsWith(".dat")) {
            throw new IOException("El fichero de acceso aleatorio debe ser .dat");
        }
        return FicheroAleatorio.personas(drive.resolver(fichero));
    }

    private Map<String, String> datosRegistro(RegistroModel reg) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("DNI", vacio(reg.getDni()));
        datos.put("NOMBRE", vacio(reg.getNombre()));
        datos.put("CIUDAD", vacio(reg.getCiudad()));
        datos.put("EDAD", vacio(reg.getEdad()));
        datos.put("FECHA_ALTA", vacio(reg.getFechaAlta()));
        return datos;
    }

    private String vacio(String s) {
        return s == null ? "" : s;
    }

    private String aleatorioResultado(RegistroModel reg, Model model) {
        List<Map<String, String>> registros = new ArrayList<>();
        // La estructura del registro se muestra siempre, aunque el fichero no sea válido
        FicheroAleatorio estructura = FicheroAleatorio.personas(null);
        model.addAttribute("campos", estructura.getCampos());
        model.addAttribute("longReg", estructura.getLongReg());
        try {
            FicheroAleatorio faa = abrirAleatorio(reg.getFichero());
            for (int i = 0; i < faa.getNumReg(); i++) {
                registros.add(faa.selectRowMap(i));
            }
            model.addAttribute("tamano", drive.resolver(reg.getFichero()).length());
        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
        }
        List<String> ficheros = drive.ficherosPorExtension(".dat");
        if (!ficheros.contains(reg.getFichero())) ficheros.add(0, reg.getFichero());
        model.addAttribute("ficherosDat", ficheros);
        model.addAttribute("registros", registros);
        model.addAttribute("reg", reg);
        return "aleatorio";
    }

    // ---------- 6. Cambio de formato / encoding de ficheros (práctica 9)

    @GetMapping("/encoding")
    public String encodingForm(@RequestParam(required = false) String ruta, Model model) {
        EncodingModel enc = new EncodingModel();
        if (ruta != null) {
            enc.setEntrada(ruta);
            int punto = ruta.lastIndexOf('.');
            enc.setSalida(punto > 0 ? ruta.substring(0, punto) + "_convertido" + ruta.substring(punto) : ruta + "_convertido");
        }
        model.addAttribute("enc", enc);
        model.addAttribute("ficheros", drive.ficherosPorExtension());
        model.addAttribute("encodings", Drive.ENCODINGS);
        return "encoding";
    }

    @PostMapping("/encoding")
    public String encodingConvertir(@ModelAttribute("enc") EncodingModel enc, Model model) {
        enc.setResultado(drive.convertirEncoding(enc.getEntrada(), enc.getEncEntrada(), enc.getSalida(), enc.getEncSalida()));
        model.addAttribute("ficheros", drive.ficherosPorExtension());
        model.addAttribute("encodings", Drive.ENCODINGS);
        return "encoding";
    }

    // ---------- Auxiliares

    private String redirigir(String carpeta) {
        return "redirect:/drive?ruta=" + java.net.URLEncoder.encode(carpeta == null ? "" : carpeta, java.nio.charset.StandardCharsets.UTF_8);
    }

    // Migas de pan: para "a/b/c" → [["a","a"], ["b","a/b"], ["c","a/b/c"]]
    private List<String[]> migas(String ruta) {
        List<String[]> migas = new ArrayList<>();
        String acumulada = "";
        for (String parte : ruta.split("/")) {
            if (parte.isEmpty()) continue;
            acumulada = acumulada.isEmpty() ? parte : acumulada + "/" + parte;
            migas.add(new String[] { parte, acumulada });
        }
        return migas;
    }
}
