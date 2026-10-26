# FileDrive

Proyecto del Tema 1 (Ficheros) de **Acceso a Datos**, 2º DAM. Es un explorador de ficheros remoto hecho con **Spring Boot + Thymeleaf** que junta las tareas 1-9 del tema.

## Arrancar

```bash
./mvnw spring-boot:run
```

Abrir <http://localhost:8080>. Los ficheros se guardan en la carpeta `drive/` del proyecto; se puede cambiar con `filedrive.raiz` en `application.properties`.

## Funcionalidades

| # | Funcionalidad | Prácticas | Dónde |
|---|---|---|---|
| 1 | Explorador/navegador de ficheros remoto (información de `java.io.File`, vocales por fichero, buscar texto por línea y columna, subir y descargar) | 1, 3, 4 | `/drive`, `/drive/info`, `/buscar` |
| 2 | Creación de ficheros y carpetas (con contenido inicial) y de la estructura Abuelo/Abuela → Padre/Madre → Hijos con bucles | 2, 5 | `/drive/crear`, `/drive/familia` |
| 3 | Eliminación de ficheros (`@DeleteMapping`) y búsqueda recursiva por nombre para borrar | 2 | `DELETE /drive`, `/drive/borrar-nombre` |
| 4 | Edición de ficheros de texto: guardar, añadir al principio y al final; editor de datos SDMX-CSV de EUROSTAT (filtro por país, por fecha y por columna, más un resumen de mínimo y máximo por país) | 3, 5, 6 | `/editor`, `/csv` |
| 5 | Ficheros de acceso aleatorio con `RandomAccessFile`: registros de longitud fija (texto, int y fecha); `insertar`, `selectCampo`, `selectColumna`, `selectRowList`, `selectRowMap`, `update` y `delete` | 7, 8 | `/aleatorio` |
| 6 | Cambio de encoding entre ASCII, UTF-8, UTF-16 e ISO-8859-1 (`InputStreamReader` → `OutputStreamWriter`) | 9 | `/encoding` |

## Estructura

Sigue los pasos para integrar las tareas en el proyecto:

- `UserController.java`: un `@GetMapping` y un `@PostMapping` por funcionalidad.
- `*Model.java`: una clase modelo por práctica, con las entradas y salidas de su formulario.
- `Drive.java`: la lógica de ficheros (prácticas 1-5 y 9).
- `FicheroAleatorio.java`: el fichero binario de registros (prácticas 7 y 8).
- `SdmxCsv.java`: la lectura y los filtros del CSV de EUROSTAT (práctica 6).
- `templates/`: las vistas Thymeleaf, que comparten `fragments.html` y `static/css/filedrive.css`.

### Registro del fichero aleatorio (`personas.dat`)

| Campo | Tipo | Bytes |
|---|---|---|
| DNI | texto UTF-8 | 9 |
| NOMBRE | texto UTF-8 | 32 |
| CIUDAD | texto UTF-8 | 24 |
| EDAD | `int` | 4 |
| FECHA_ALTA | `long` (días desde 1970) | 8 |

Cada registro ocupa 77 bytes, así que el registro *n* empieza en el byte *n* × 77. `delete` pone el registro entero a ceros sin mover los demás.
