package com.example.literalura.bookshelf;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    // Búsqueda exacta por nombre para reutilizar autores ya guardados y no duplicarlos
    Optional<Autor> findByNombre(String nombre);

    // Autores vivos en un anio: nacieron antes o en ese anio, y aún no habían muerto (o no tienen fecha de muerte)
    @Query("SELECT a FROM Autor a WHERE a.anioNacimiento <= :anio " +
           "AND (a.anioFallecimiento IS NULL OR a.anioFallecimiento >= :anio)")
    List<Autor> buscarVivosEnAnio(@Param("anio") int anio);
}
