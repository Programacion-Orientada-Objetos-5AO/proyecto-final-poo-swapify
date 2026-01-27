package ar.edu.huergo.swapify.entity.publicacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ar.edu.huergo.swapify.entity.security.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@ToString(exclude = {"usuario", "imagenes", "legacyImagen"})
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nombre", column = @Column(name = "nombre", nullable = false, length = 120)),
            @AttributeOverride(name = "precio", column = @Column(name = "precio", precision = 12, scale = 2)),
            @AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 2000))
    })
    private Articulo articulo = new Articulo();

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

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<PublicacionImagen> imagenes = new ArrayList<>();

    @Lob
    @Column(name = "imagen", columnDefinition = "LONGBLOB")
    private byte[] legacyImagen;

    @Column(name = "imagen_content_type", length = 100)
    private String legacyImagenContentType;

    public Publicacion(Long id, String nombre, java.math.BigDecimal precio, String descripcion,
            String objetoACambiar, LocalDateTime fechaPublicacion, Usuario usuario, List<PublicacionImagen> imagenes,
            LocalDateTime fechaReserva, LocalDateTime fechaCierre) {
        this.id = id;
        this.articulo = new Articulo(nombre, precio, descripcion);
        this.objetoACambiar = objetoACambiar;
        this.fechaPublicacion = fechaPublicacion;
        this.usuario = usuario;
        this.estado = EstadoPublicacion.ACTIVA;
        this.oficial = false;
        this.fechaReserva = fechaReserva;
        this.fechaCierre = fechaCierre;
        if (imagenes != null) {
            imagenes.forEach(this::agregarImagen);
        }
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
        ordenarImagenes();
    }

    public boolean tieneImagenes() {
        return imagenes != null && !imagenes.isEmpty();
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }

    public String getNombre() {
        return articulo != null ? articulo.getNombre() : null;
    }

    public void setNombre(String nombre) {
        asegurarArticulo().setNombre(nombre);
    }

    public java.math.BigDecimal getPrecio() {
        return articulo != null ? articulo.getPrecio() : null;
    }

    public void setPrecio(java.math.BigDecimal precio) {
        asegurarArticulo().setPrecio(precio);
    }

    public String getDescripcion() {
        return articulo != null ? articulo.getDescripcion() : null;
    }

    public void setDescripcion(String descripcion) {
        asegurarArticulo().setDescripcion(descripcion);
    }

    public void limpiarImagenes() {
        if (imagenes != null) {
            imagenes.clear();
        }
    }

    public void agregarImagen(PublicacionImagen imagen) {
        if (imagen == null) {
            return;
        }
        if (imagenes == null) {
            imagenes = new ArrayList<>();
        }
        imagen.setPublicacion(this);
        if (imagen.getOrden() < 0) {
            imagen.setOrden(imagenes.size());
        }
        imagenes.add(imagen);
        ordenarImagenes();
    }

    public List<PublicacionImagen> getImagenesOrdenadas() {
        ordenarImagenes();
        return imagenes;
    }

    public PublicacionImagen getImagenPrincipal() {
        return tieneImagenes() ? getImagenesOrdenadas().get(0) : null;
    }

    private void ordenarImagenes() {
        if (imagenes != null) {
            imagenes.sort(Comparator.comparingInt(PublicacionImagen::getOrden));
            for (int i = 0; i < imagenes.size(); i++) {
                PublicacionImagen imagen = imagenes.get(i);
                if (imagen.getOrden() != i) {
                    imagen.setOrden(i);
                }
                imagen.setPublicacion(this);
            }
        }
    }

    public boolean estaActiva() {
        return estado != null && estado.admiteOfertas();
    }

    public boolean estaEnNegociacion() {
        return estado != null && estado.estaReservada();
    }

    public boolean estaFinalizada() {
        return estado != null && EstadoPublicacion.FINALIZADA.equals(estado);
    }

    public void marcarEnNegociacion(LocalDateTime fecha) {
        this.estado = EstadoPublicacion.EN_NEGOCIACION;
        this.fechaReserva = fecha;
    }

    public void marcarFinalizada(LocalDateTime fecha) {
        this.estado = EstadoPublicacion.FINALIZADA;
        this.fechaCierre = fecha;
    }

    public void reactivar() {
        this.estado = EstadoPublicacion.ACTIVA;
        this.fechaReserva = null;
        this.fechaCierre = null;
    }

    public void pausar() {
        this.estado = EstadoPublicacion.PAUSADA;
    }

    public byte[] getLegacyImagen() {
        return legacyImagen != null ? Arrays.copyOf(legacyImagen, legacyImagen.length) : null;
    }

    public void setLegacyImagen(byte[] legacyImagen) {
        this.legacyImagen = legacyImagen != null ? Arrays.copyOf(legacyImagen, legacyImagen.length) : null;
    }

    public String getLegacyImagenContentType() {
        return legacyImagenContentType;
    }

    public void setLegacyImagenContentType(String legacyImagenContentType) {
        this.legacyImagenContentType = legacyImagenContentType;
    }

    private Articulo asegurarArticulo() {
        if (this.articulo == null) {
            this.articulo = new Articulo();
        }
        return this.articulo;
    }
}
