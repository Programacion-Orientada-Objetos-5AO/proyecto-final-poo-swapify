package ar.edu.huergo.swapify.service.prestamo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.huergo.swapify.dto.prestamo.PrestamoRequestDTO;
import ar.edu.huergo.swapify.dto.prestamo.PrestamoResponseDTO;
import ar.edu.huergo.swapify.dto.prestamo.ResumenUsuarioDTO;
import ar.edu.huergo.swapify.entity.prestamo.Prestamo;
import ar.edu.huergo.swapify.repository.prestamo.PrestamoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;

    @Transactional
    public PrestamoResponseDTO crear(PrestamoRequestDTO request) {
        Prestamo prestamo = new Prestamo();
        aplicarDatos(prestamo, request, true);
        prestamo.setDevuelto(false);
        prestamo.setFechaDevolucionReal(null);
        Prestamo guardado = prestamoRepository.save(prestamo);
        return mapear(guardado);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponseDTO> listar() {
        return prestamoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPrestamo"))
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrestamoResponseDTO obtener(Long id) {
        return mapear(buscarPorId(id));
    }

    @Transactional
    public PrestamoResponseDTO actualizar(Long id, PrestamoRequestDTO request) {
        Prestamo prestamo = buscarPorId(id);
        aplicarDatos(prestamo, request, false);
        Prestamo actualizado = prestamoRepository.save(prestamo);
        return mapear(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!prestamoRepository.existsById(id)) {
            throw new EntityNotFoundException("Prestamo no encontrado");
        }
        prestamoRepository.deleteById(id);
    }

    @Transactional
    public PrestamoResponseDTO marcarDevuelto(Long id) {
        Prestamo prestamo = buscarPorId(id);
        if (!prestamo.isDevuelto()) {
            prestamo.setDevuelto(true);
            prestamo.setFechaDevolucionReal(LocalDate.now());
            prestamoRepository.save(prestamo);
        }
        return mapear(prestamo);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponseDTO> listarVencidos() {
        LocalDate hoy = LocalDate.now();
        return prestamoRepository.findByDevueltoFalseAndFechaDevolucionBefore(hoy)
                .stream()
                .sorted(Comparator.comparing(Prestamo::getFechaDevolucion))
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponseDTO> listarPorUsuario(String usuario) {
        String normalizado = normalizar(usuario);
        if (normalizado.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre de usuario");
        }
        return prestamoRepository.findByNombreUsuarioIgnoreCase(normalizado)
                .stream()
                .sorted(Comparator.comparing(Prestamo::getFechaPrestamo).reversed())
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumenUsuarioDTO resumenPorUsuario(String usuario) {
        String normalizado = normalizar(usuario);
        if (normalizado.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre de usuario");
        }
        List<Prestamo> prestamos = prestamoRepository.findByNombreUsuarioIgnoreCase(normalizado);
        LocalDate hoy = LocalDate.now();

        int total = prestamos.size();
        int activos = (int) prestamos.stream().filter(p -> !p.isDevuelto()).count();
        int vencidos = (int) prestamos.stream()
                .filter(p -> !p.isDevuelto())
                .filter(p -> p.getFechaDevolucion() != null && p.getFechaDevolucion().isBefore(hoy))
                .count();

        String libroMasPrestado = prestamos.stream()
                .filter(p -> p.getTituloLibro() != null && !p.getTituloLibro().isBlank())
                .collect(Collectors.groupingBy(p -> p.getTituloLibro().trim(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        long devueltos = prestamos.stream().filter(Prestamo::isDevuelto).count();
        long devueltosPuntuales = prestamos.stream()
                .filter(Prestamo::isDevuelto)
                .filter(p -> p.getFechaDevolucionReal() != null && p.getFechaDevolucion() != null)
                .filter(p -> !p.getFechaDevolucionReal().isAfter(p.getFechaDevolucion()))
                .count();
        double tasa = devueltos == 0 ? 0.0 : (double) devueltosPuntuales / devueltos;

        return new ResumenUsuarioDTO(normalizado, total, activos, vencidos, libroMasPrestado, tasa);
    }

    private Prestamo buscarPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prestamo no encontrado"));
    }

    private void aplicarDatos(Prestamo prestamo, PrestamoRequestDTO request, boolean esNuevo) {
        if (request == null) {
            throw new IllegalArgumentException("Datos de prestamo invalidos");
        }
        String titulo = normalizar(request.tituloLibro());
        if (titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        String usuario = normalizar(request.nombreUsuario());
        if (usuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        int dias = validarDias(request.diasPrestamo());
        prestamo.setTituloLibro(titulo);
        prestamo.setNombreUsuario(usuario);
        prestamo.setDiasPrestamo(dias);

        LocalDate fechaBase = esNuevo ? LocalDate.now() : prestamo.getFechaPrestamo();
        if (fechaBase == null) {
            fechaBase = LocalDate.now();
        }
        if (esNuevo) {
            prestamo.setFechaPrestamo(fechaBase);
        }
        prestamo.setFechaDevolucion(fechaBase.plusDays(dias));
    }

    private int validarDias(Integer dias) {
        if (dias == null || dias < 1) {
            throw new IllegalArgumentException("Los dias de prestamo deben ser mayores a cero");
        }
        return dias;
    }

    private PrestamoResponseDTO mapear(Prestamo prestamo) {
        return new PrestamoResponseDTO(
                prestamo.getId(),
                prestamo.getTituloLibro(),
                prestamo.getNombreUsuario(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaDevolucion(),
                prestamo.isDevuelto());
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }
}
