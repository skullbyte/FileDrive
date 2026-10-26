package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WebController {

    @GetMapping("/")
    public String inicio() {
        // Redirige al explorador de FILEDRIVE cuando el usuario entra a localhost:8080/
        return "redirect:/drive"; 
    }

    // Método que faltaba para la página de contacto (Paso 1 del ejercicio)
    @GetMapping("/contacto")
    public String contacto(Model model) {
        model.addAttribute("mensaje", "hola desde spring mvc");
        return "contacto";
    }

    @GetMapping("/producto-solo")
    public String verUnProducto(Model model) {
        Producto miProducto = new Producto(1, "Ordenador", 1200.50f);
        model.addAttribute("objeto", miProducto); 
        return "ver_producto"; 
    }

    @GetMapping("/productos")
    public String mostrarProductos(Model model) {
        // Enviar objeto individual
        model.addAttribute("objeto", new Producto(1, "Ordenador", 200f));

        List<Producto> miLista = new ArrayList<>();
        miLista.add(new Producto(1, "Ordenador", 1200.50f));
        miLista.add(new Producto(2, "Ratón", 25.0f));
        miLista.add(new Producto(3, "Teclado", 45.0f));

        model.addAttribute("lista", miLista); 
        return "productos/ver_productos"; 
    }
}