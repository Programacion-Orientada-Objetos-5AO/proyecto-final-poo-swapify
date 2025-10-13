package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ar.edu.huergo.swapify.entity.publicacion.EstadoPublicacion;
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
    private List<String> imagenesDataUri;
    private String imagenPrincipalDataUri;
    private EstadoPublicacion estado;
    private boolean oficial;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaCierre;
}
