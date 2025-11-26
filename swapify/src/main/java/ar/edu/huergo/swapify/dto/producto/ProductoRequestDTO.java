package ar.edu.huergo.swapify.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductoRequestDTO {
    @NotBlank(message = "Nombre obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "Categoria obligatoria")
    @Size(min = 2, max = 100, message = "La categoria debe tener entre 2 y 100 caracteres")
    private String categoria;

    @NotNull(message = "Precio obligatorio")
    private Double precio;
}
