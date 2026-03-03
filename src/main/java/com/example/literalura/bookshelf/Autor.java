package com.example.literalura.bookshelf;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Le decimos a Jackson que ignore campos del JSON que no nos interesan
@JsonIgnoreProperties(ignoreUnknown = true)
// Le decimos a JPA que esta clase es una tabla en la base de datos
@Entity
@Table(name = "autores")
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonAlias mapea el campo "name" del JSON a nuestra variable "nombre"
    @JsonAlias("name")
    private String nombre;

    // Usamos 'anio' para evitar usar la letra ñ en código y base de datos
    @JsonAlias("birth_year")
    private Integer anioNacimiento;

    @JsonAlias("death_year")
    private Integer anioFallecimiento;

    // Constructor vacío obligatorio para JPA
    public Autor() {}

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getAnioNacimiento() { return anioNacimiento; }
    public void setAnioNacimiento(Integer anioNacimiento) { this.anioNacimiento = anioNacimiento; }

    public Integer getAnioFallecimiento() { return anioFallecimiento; }
    public void setAnioFallecimiento(Integer anioFallecimiento) { this.anioFallecimiento = anioFallecimiento; }

    @Override
    public String toString() {
        String muerte = anioFallecimiento != null ? String.valueOf(anioFallecimiento) : "aún vivo";
        return nombre + " (" + anioNacimiento + " - " + muerte + ")";
    }
}
