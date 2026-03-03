package com.example.literalura.bookshelf;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    // Búsqueda exacta por título ignorando mayúsculas, para evitar duplicados al guardar
    Optional<Libro> findByTituloIgnoreCase(String titulo);

    // Búsqueda de todos los libros de un idioma dado
    List<Libro> findByIdioma(String idioma);
}
