package com.example.demo;

// Prácticas 7 y 8: formulario del fichero de acceso aleatorio
public class RegistroModel {
    private String fichero = "personas.dat";
    private String accion;
    private Integer fila;
    private String campo;
    private String valor;
    private String dni;
    private String nombre;
    private String ciudad;
    private String edad;
    private String fechaAlta;

    public String getFichero() { return fichero; }
    public void setFichero(String fichero) { this.fichero = fichero; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public Integer getFila() { return fila; }
    public void setFila(Integer fila) { this.fila = fila; }
    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }
    public String getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(String fechaAlta) { this.fechaAlta = fechaAlta; }
}
