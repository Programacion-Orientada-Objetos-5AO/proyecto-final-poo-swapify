package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MostrarPublicacionDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private String descripcion;
    private String objetoACambiar;
    private LocalDateTime fechaPublicacion;
    private String usuarioUsername;
}
