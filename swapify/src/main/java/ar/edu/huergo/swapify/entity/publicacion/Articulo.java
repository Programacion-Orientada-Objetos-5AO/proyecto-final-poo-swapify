package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un artículo comercializable con los datos básicos que pueden
 * aparecer tanto en publicaciones como en ofertas.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Articulo {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
}
