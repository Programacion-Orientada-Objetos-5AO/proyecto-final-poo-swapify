package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import ar.edu.huergo.swapify.entity.security.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPublicacion estado = EstadoPublicacion.ACTIVA;

    @Column(name = "es_oficial", nullable = false)
    private boolean oficial;

    @Column(name = "fecha_reserva")
    private LocalDateTime fechaReserva;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Lob
    @Column(name = "imagen", columnDefinition = "LONGBLOB")
    private byte[] imagen;

    @Column(name = "imagen_content_type", length = 100)
    private String imagenContentType;

    @Transient
    private String imagenBase64;

    @Transient
    private String imagenDataUri;

    public Publicacion(Long id, String nombre, BigDecimal precio, String descripcion,
            String objetoACambiar, LocalDateTime fechaPublicacion, Usuario usuario, byte[] imagen,
            String imagenContentType, LocalDateTime fechaReserva, LocalDateTime fechaCierre) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.descripcion = descripcion;
        this.objetoACambiar = objetoACambiar;
        this.fechaPublicacion = fechaPublicacion;
        this.usuario = usuario;
        this.estado = EstadoPublicacion.ACTIVA;
        this.oficial = false;
        this.fechaReserva = fechaReserva;
        this.fechaCierre = fechaCierre;
        setImagen(imagen);
        this.imagenContentType = imagenContentType;
    }

    /**
     * Inicializa la fecha de publicación cuando aún no fue definida.
     */
    @PrePersist
    public void prePersist() {
        if (this.fechaPublicacion == null) {
            this.fechaPublicacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoPublicacion.ACTIVA;
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

    public boolean estaActiva() {
        return estado != null && estado.admiteOfertas();
    }

    public boolean estaEnNegociacion() {
        return estado != null && estado.estaReservada();
    }

    public boolean estaFinalizada() {
        return EstadoPublicacion.FINALIZADA.equals(estado);
    }

    public void marcarEnNegociacion(LocalDateTime momento) {
        this.estado = EstadoPublicacion.EN_NEGOCIACION;
        this.fechaReserva = momento;
        this.fechaCierre = null;
    }

    public void marcarFinalizada(LocalDateTime momento) {
        this.estado = EstadoPublicacion.FINALIZADA;
        this.fechaCierre = momento;
    }

    public void reactivar() {
        this.estado = EstadoPublicacion.ACTIVA;
        this.fechaReserva = null;
        this.fechaCierre = null;
    }

    public void pausar() {
        this.estado = EstadoPublicacion.PAUSADA;
    }
}
