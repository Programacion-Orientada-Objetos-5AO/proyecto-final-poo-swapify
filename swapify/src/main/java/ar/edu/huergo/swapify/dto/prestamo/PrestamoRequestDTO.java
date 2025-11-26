package ar.edu.huergo.swapify.dto.prestamo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrestamoRequestDTO(
        @NotBlank(message = "El titulo del libro es obligatorio")
        String tituloLibro,
        @NotBlank(message = "El nombre del usuario es obligatorio")
        String nombreUsuario,
        @NotNull(message = "Los dias de prestamo son obligatorios")
        @Min(value = 1, message = "Los dias de prestamo deben ser al menos 1")
        Integer diasPrestamo) {
}
