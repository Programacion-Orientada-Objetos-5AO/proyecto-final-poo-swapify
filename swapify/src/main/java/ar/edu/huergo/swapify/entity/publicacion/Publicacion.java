package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Publicacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 120)
    private String nombre;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;  // dinero → BigDecimal

    @NotBlank(message = "La descripción es obligatoria")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    @NotBlank(message = "Debe indicar el objeto a cambiar")
    @Column(name = "objeto_a_cambiar", nullable = false, length = 255)
    private String objetoACambiar;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaPublicacion == null) {
            this.fechaPublicacion = LocalDateTime.now();
        }
    }
}
