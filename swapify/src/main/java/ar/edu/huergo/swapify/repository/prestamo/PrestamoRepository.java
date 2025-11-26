package ar.edu.huergo.swapify.repository.prestamo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.huergo.swapify.entity.prestamo.Prestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByDevueltoFalseAndFechaDevolucionBefore(LocalDate fechaLimite);

    List<Prestamo> findByNombreUsuarioIgnoreCase(String nombreUsuario);
}
