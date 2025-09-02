package ar.edu.huergo.swapify.dto.publicacion;


import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReportePublicacionesDTO {
    private long cantidadPublicaciones;
    private BigDecimal sumaPrecios; 
    private List<MostrarPublicacionDTO> publicaciones;
}
