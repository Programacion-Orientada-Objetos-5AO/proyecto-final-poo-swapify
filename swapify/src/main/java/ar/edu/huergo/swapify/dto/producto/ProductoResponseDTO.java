package ar.edu.huergo.swapify.dto.producto;

import lombok.Data;

@Data
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private String categoria;
    private Double precio;
    private Integer stock;
}
