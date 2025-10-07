package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import ar.edu.huergo.swapify.entity.security.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una publicación intercambiable junto con su autor,
 * precio, descripción y metadatos de imagen.
 */
@Entity
@Table(name = "Publicacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuario", "imagen", "imagenBase64", "imagenDataUri"})
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 120)
    private String nombre;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    @NotBlank(message = "Debe indicar el objeto a cambiar")
    @Column(name = "objeto_a_cambiar", nullable = false, length = 255)
    private String objetoACambiar;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    /**
     * Usuario propietario de la publicación. Se carga ansiosamente para evitar
     * problemas de inicialización diferida al mapear la entidad.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "El usuario es obligatorio")
    private Usuario usuario;

    @Lob
    @Column(name = "imagen", columnDefinition = "LONGBLOB")
    private byte[] imagen;

    @Column(name = "imagen_content_type", length = 100)
    private String imagenContentType;

    @Transient
    private String imagenBase64;

    @Transient
    private String imagenDataUri;

    /**
     * Inicializa la fecha de publicación cuando aún no fue definida.
     */
    @PrePersist
    public void prePersist() {
        if (this.fechaPublicacion == null) {
            this.fechaPublicacion = LocalDateTime.now();
        }
    }

    /**
     * Indica si la publicación posee una imagen persistida.
     *
     * @return {@code true} cuando existe contenido binario asociado.
     */
    public boolean tieneImagen() {
        return imagen != null && imagen.length > 0;
    }

    /**
     * Elimina cualquier representación de imagen vinculada a la publicación.
     */
    public void limpiarImagen() {
        this.imagen = null;
        this.imagenContentType = null;
        this.imagenBase64 = null;
        this.imagenDataUri = null;
    }

    /**
     * Obtiene una copia defensiva del contenido de imagen para evitar escapes de
     * referencias mutables.
     *
     * @return copia del arreglo de bytes o {@code null} si no hay imagen.
     */
    public byte[] getImagen() {
        return imagen != null ? Arrays.copyOf(imagen, imagen.length) : null;
    }

    /**
     * Define el contenido de imagen almacenando una copia independiente del
     * arreglo recibido.
     *
     * @param imagen datos binarios a asociar con la publicación.
     */
    public void setImagen(byte[] imagen) {
        this.imagen = (imagen != null) ? Arrays.copyOf(imagen, imagen.length) : null;
    }
}
