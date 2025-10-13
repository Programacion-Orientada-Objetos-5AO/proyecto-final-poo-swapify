package ar.edu.huergo.swapify.entity.publicacion;

import java.util.Arrays;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representa una imagen asociada a una publicación determinada.
 * Permite almacenar múltiples fotografías manteniendo el orden
 * en el que fueron cargadas por la persona autora.
 */
@Entity
@Table(name = "publicacion_imagen")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"publicacion", "datos", "base64", "dataUri"})
public class PublicacionImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "datos", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] datos;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Transient
    private String base64;

    @Transient
    private String dataUri;

    public void setDatos(byte[] datos) {
        this.datos = datos != null ? Arrays.copyOf(datos, datos.length) : null;
    }

    public byte[] getDatos() {
        return datos != null ? Arrays.copyOf(datos, datos.length) : null;
    }
}
