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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoOferta estado = EstadoOferta.PENDIENTE;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    public void prePersist() {
        if (fechaOferta == null) {
            fechaOferta = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoOferta.PENDIENTE;
        }
    }

    public boolean estaPendiente() {
        return EstadoOferta.PENDIENTE.equals(estado);
    }

    public boolean estaAceptada() {
        return EstadoOferta.ACEPTADA.equals(estado);
    }

    public boolean estaRechazada() {
        return EstadoOferta.RECHAZADA.equals(estado);
    }
}
