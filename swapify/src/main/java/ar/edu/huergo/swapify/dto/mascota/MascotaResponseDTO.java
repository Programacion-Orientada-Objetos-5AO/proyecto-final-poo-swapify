package ar.edu.huergo.swapify.dto.mascota;

import lombok.Data;

@Data
public class MascotaResponseDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private Integer edad;
    private Boolean adoptado = false;
}

