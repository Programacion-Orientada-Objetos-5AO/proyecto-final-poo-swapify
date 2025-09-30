package ar.edu.huergo.swapify.entity.publicacion;

import ar.edu.huergo.swapify.entity.security.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "Oferta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"publicacion", "usuario"})
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotBlank(message = "La propuesta no puede estar vacía")
    @Column(nullable = false, length = 2000)
    private String mensaje;

    @Column(name = "propuesta_objeto", length = 255)
    private String propuestaObjeto;

    @Column(name = "fecha_oferta", nullable = false)
    private LocalDateTime fechaOferta;

    @PrePersist
    public void prePersist() {
        if (fechaOferta == null) {
            fechaOferta = LocalDateTime.now();
        }
    }
}
