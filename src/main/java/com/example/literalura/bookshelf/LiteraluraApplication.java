package com.example.literalura.bookshelf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class LiteraluraApplication implements CommandLineRunner {

    // Acceso a la base de datos con esquema Repository
    private final LibroRepository libroRepo;
    private final AutorRepository autorRepo;

    // CRear acceso a la API, objetcMapper y Scanner desde el inicio
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Scanner scanner = new Scanner(System.in);

    // Limite de lirbos por página en la búsqueda
    private final int limiteLibrosBusqueda = 5;
    // El idioma elegido
    private String idiomaActual = "es";

    public LiteraluraApplication(LibroRepository libroRepo, AutorRepository autorRepo) {
        this.libroRepo = libroRepo;
        this.autorRepo = autorRepo;
    }

    public static void main(String[] args) {
        SpringApplication.run(LiteraluraApplication.class, args);
    }

    // Reescribiendo la función run()
    @Override
    public void run(String... args) {
        mostrarMenu();
    }

    private void mostrarMenu() {
        int opcion = -1;

        while (opcion != 0) {
            System.out.println("\n=============================");
            System.out.println(" 📚  LiterAlura | Idioma: " + idiomaActual);
            System.out.println("=============================");
            System.out.println(" 1 - Buscar libro por título");
            System.out.println(" 2 - Ver todos mis libros");
            System.out.println(" 3 - Ver todos los autores");
            System.out.println(" 4 - Autores vivos en un año");
            System.out.println(" 5 - Libros por idioma");
            System.out.println(" 6 - Cambiar idioma de búsqueda");
            System.out.println(" 0 - Salir");
            System.out.println("=============================");
            System.out.print("Elige una opción: ");

            try {
                // Leer entrada como entero en una sola línea
                opcion = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Escribe solo el número de la opción.");
                continue;
            }

            switch (opcion) {
                case 1 -> buscarLibro();
                case 2 -> verLibros();
                case 3 -> verAutores();
                case 4 -> autoresVivosEnAnio();
                case 5 -> librosPorIdioma();
                case 6 -> cambiarIdioma();
                case 0 -> System.out.println("¡Hasta luego!");
                default -> System.out.println("Opción no válida. Elige entre 0 y 6.");
            }
        }

        scanner.close();
    }

    // Busca en la API Gutendex, filtra por título e idioma, permite guardar al elegir libro
    private void buscarLibro() {
        System.out.print("Título a buscar: ");
        String titulo = scanner.nextLine().trim();

        if (titulo.isBlank()) {
            System.out.println("El título no puede estar vacío.");
            return;
        }

        System.out.println("Buscando '" + titulo + "' en idioma '" + idiomaActual + "'...");
        System.out.println("Esto puede tardar unos segundos, aconsejamos paciencia :)");

        String url = "https://gutendex.com/books?search=" + titulo.replace(" ", "%20")
                   + "&languages=" + idiomaActual;

        while (url != null) {
            String json = hacerGet(url);
            if (json == null) return;

            RespuestaApi respuesta;
            try {
                respuesta = objectMapper.readValue(json, RespuestaApi.class);
            } catch (Exception e) {
                System.out.println("Error de la API: " + e.getMessage());
                return;
            }

            if (respuesta.results == null || respuesta.results.isEmpty()) {
                System.out.println("No se encontraron libros con ese título en idioma '" + idiomaActual + "'.");
                return;
            }

            // Mostrar solo los primeros resultados
            List<Libro> pagina = respuesta.results.stream().limit(limiteLibrosBusqueda).toList();
            // Llamar al método preparar() para cada libro, para extraer el primer idioma y autor de las listas del JSON
            pagina.forEach(Libro::preparar);

            System.out.println("\n Resultados =================");
            for (int i = 0; i < pagina.size(); i++) {
                Libro libro = pagina.get(i);
                // If terneario por si el libro no tiene autor
                String autor = libro.getAutor() != null ? libro.getAutor().getNombre() : "desconocido";
                System.out.println("  [" + (i + 1) + "] " + libro.getTitulo() + "  (" + autor + ")");
            }

            System.out.println();
            System.out.println("  [1–" + pagina.size() + "] Guardar ese libro");
            if (respuesta.next != null) System.out.println("  [s] Ver siguientes resultados");
            System.out.println("  [0] Cancelar búsqueda");
            System.out.print("Elige: ");

            String eleccion = scanner.nextLine().trim().toLowerCase();

            if (eleccion.equals("0")) {
                return;
            }

            if (eleccion.equals("s") && respuesta.next != null) {
                // Cargar siguiente pagina si existe
                url = respuesta.next;
                continue;
            }

            try {
                int posicion = Integer.parseInt(eleccion);

                if (posicion < 1 || posicion > pagina.size()) {
                    System.out.println("Número de libro invalido.");
                    return;
                }

                Libro libro = pagina.get(posicion - 1);

                // Usamos 'Optional' por si no retorna nada, evitando NUllLPointerException
                Optional<Libro> existente = libroRepo.findByTituloIgnoreCase(libro.getTitulo());
                if (existente.isPresent()) {
                    // No guardadr si ya guardamos el libro
                    System.out.println("Este libro ya está en tu biblioteca:" + existente.get());
                    return;
                }

                if (libro.getAutor() != null) {
                    Optional<Autor> autorExistente = autorRepo.findByNombre(libro.getAutor().getNombre());
                    // No guardadr si ya guardamos el autor
                    if (autorExistente.isPresent()) {
                        libro.setAutor(autorExistente.get());
                    } else {
                        // Si NO existe, guardar antes que el libro
                        Autor autorNuevo = libro.getAutor();
                        autorRepo.save(autorNuevo);
                        libro.setAutor(autorNuevo);
                    }
                }

                libroRepo.save(libro);
                System.out.println("Libro guardado:" + libro);
                System.out.println("Autor guardado:" + libro.getAutor());
                return;

            } catch (NumberFormatException e) {
                System.out.println("Opción inválida");
                return;
            }
        }
    }

    // Muestra todos los libros guardados en la base de datos
    private void verLibros() {
        List<Libro> libros = libroRepo.findAll();

        // Si no hay libros guardados
        if (libros.isEmpty()) {
            System.out.println("Tu biblioteca está vacía. Busca un libro primero.");
            return;
        }

        System.out.println("\n=== Tus libros (" + libros.size() + ") ===");
        // Usar forEach como una forma de mostrar cada libro con su autor, sin usar for
        libros.forEach(System.out::println);
    }

    // Muestra todos los autores guardados en la base de datos
    private void verAutores() {
        List<Autor> autores = autorRepo.findAll();

        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados todavía.");
            return;
        }

        System.out.println("\n=== Autores registrados (" + autores.size() + ") ===");
        autores.forEach(autor -> System.out.println("  - " + autor));
    }

    // Busca en la base de deatos los autores cuyo rango de vida incluye el anio ingresado
    private void autoresVivosEnAnio() {
        System.out.print("¿En qué año? ");
        try {
            int anio = Integer.parseInt(scanner.nextLine().trim());
            List<Autor> autores = autorRepo.buscarVivosEnAnio(anio);

            if (autores.isEmpty()) {
                System.out.println("No hay autores en tu biblioteca que estuvieran vivos en " + anio + ".");
                return;
            }

            System.out.println("\n=== Autores vivos en " + anio + " ===");
            autores.forEach(autor -> System.out.println("  - " + autor));

        } catch (NumberFormatException e) {
            System.out.println("Escribe un año válido, por ejemplo: 1800");
        }
    }

    // Muestra los libros guardados filtrando por el código de idioma que el usuario elija
    private void librosPorIdioma() {
        System.out.println("Idiomas disponibles: en (Inglés) | es (Español) | fr (Francés) | de (Alemán)");
        System.out.print("Código de idioma: ");
        String codigo = scanner.nextLine().trim().toLowerCase();

        if (!List.of("en", "es", "fr", "de").contains(codigo)) {
            System.out.println("Código no reconocido. Usa: en, es, fr o de.");
            return;
        }

        List<Libro> libros = libroRepo.findByIdioma(codigo);

        System.out.println("\nLibros en idioma '" + codigo + "': " + libros.size());
        libros.forEach(libro -> System.out.println("  - " + libro.getTitulo()));
    }

    // Cambia el idioma activo que se usará en búsquedas de API y consultas de BD
    private void cambiarIdioma() {
        System.out.println("Idiomas disponibles: en (Inglés) | es (Español) | fr (Francés) | de (Alemán)");
        System.out.print("Nuevo idioma: ");
        String codigo = scanner.nextLine().trim().toLowerCase();

        if (!List.of("en", "es", "fr", "de").contains(codigo)) {
            System.out.println("Código no reconocido. Usa: en, es, fr o de.");
            return;
        }

        idiomaActual = codigo;
        System.out.println("Idioma cambiado a '" + idiomaActual + "'.");
    }

    // Hace una petición GET y devuelve el cuerpo como texto, o null si hubo error
    private String hacerGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("La API respondió con error. Código: " + response.statusCode());
                return null;
            }

            return response.body();

        } catch (Exception e) {
            System.out.println("No se pudo conectar con la API: " + e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RespuestaApi {
        public List<Libro> results;
        public String next;
    }
}
