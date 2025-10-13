package ar.edu.huergo.swapify.entity.security;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Representa un aviso interno enviado a una persona usuaria ante eventos
 * relevantes de la plataforma (ofertas, moderación, administración, etc).
 */
@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
@ToString(exclude = "usuario")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String mensaje;

    @Column(length = 80)
    private String tipo;

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(length = 255)
    private String enlace;

    @Column(length = 255)
    private String icono;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
