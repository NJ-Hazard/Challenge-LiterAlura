package com.example.literalura.bookshelf;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

// Esta clase hace dos cosas: recibe el JSON de la API Y se guarda en la BD
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "libros")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El JsonAlias es 'title' porque así se llama el campo en el JSON de la API, pero en la BD lo guardamos como 'titulo'
    @JsonAlias("title")
    @Column(unique = true)
    private String titulo;

    @JsonAlias("download_count")
    private Integer descargas;

    private String idioma;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "autor_id")
    private Autor autor;

    // Campos temporales para recibir las listas del JSON
    @JsonAlias("languages")
    @Transient
    private List<String> idiomasJson;

    @JsonAlias("authors")
    @Transient
    private List<AutorJson> autoresJson;

    // Constructor vacío necesario para JPA y para Jackson al leer el JSON
    public Libro() {}

    // Extrae el primer idioma y primer autor de las listas que llegaron del JSON
    public void preparar() {
        if (idiomasJson != null && !idiomasJson.isEmpty()) {
            this.idioma = idiomasJson.get(0);
        }
        if (autoresJson != null && !autoresJson.isEmpty()) {
            AutorJson a = autoresJson.get(0);
            Autor nuevoAutor = new Autor();
            nuevoAutor.setNombre(a.nombre);
            nuevoAutor.setAnioNacimiento(a.anioNacimiento);
            nuevoAutor.setAnioFallecimiento(a.anioFallecimiento);
            this.autor = nuevoAutor;
        }
    }

    public Long getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public Integer getDescargas() { return descargas; }
    public void setDescargas(Integer descargas) { this.descargas = descargas; }

    public Autor getAutor() { return autor; }
    public void setAutor(Autor autor) { this.autor = autor; }

    public List<String> getIdiomasJson() { return idiomasJson; }
    public void setIdiomasJson(List<String> idiomasJson) { this.idiomasJson = idiomasJson; }

    public List<AutorJson> getAutoresJson() { return autoresJson; }
    public void setAutoresJson(List<AutorJson> autoresJson) { this.autoresJson = autoresJson; }

    @Override
    public String toString() {
        // Si el autor es null, mostramos "desconocido" para evitar NullPointerException al imprimir
        String nombreAutor = autor != null ? autor.getNombre() : "desconocido";
        return "\n  Título:    " + titulo +
               "\n  Autor:     " + nombreAutor +
               "\n  Idioma:    " + idioma +
               "\n  Descargas: " + descargas;
    }

    // Clase interna solo para leer el JSON de autores — no se guarda en ninguna tabla
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AutorJson {
        @JsonAlias("name")
        public String nombre;

        @JsonAlias("birth_year")
        public Integer anioNacimiento;

        @JsonAlias("death_year")
        public Integer anioFallecimiento;
    }
}