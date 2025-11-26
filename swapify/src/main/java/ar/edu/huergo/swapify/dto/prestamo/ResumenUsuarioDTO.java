package ar.edu.huergo.swapify.dto.prestamo;

public record ResumenUsuarioDTO(
        String nombreUsuario,
        Integer totalPrestamos,
        Integer prestamosActivos,
        Integer prestamosVencidos,
        String libroMasPrestado,
        Double tasaDevolucionPuntual) {
}
