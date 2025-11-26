package ar.edu.huergo.swapify.dto.prestamo;

import java.time.LocalDate;

public record PrestamoResponseDTO(
        Long id,
        String tituloLibro,
        String nombreUsuario,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucion,
        Boolean devuelto) {
}
