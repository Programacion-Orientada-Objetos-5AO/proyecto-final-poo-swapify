package ar.edu.huergo.swapify.entity.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entidad que representa un rol de autorización asignable a las personas
 * usuarias del sistema.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre único del rol (por ejemplo {@code ADMIN} o {@code CLIENTE}).
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    /**
     * Construye un rol únicamente con su nombre.
     *
     * @param nombre identificador legible del rol.
     */
    public Rol(String nombre) {
        this.nombre = nombre;
    }
}


