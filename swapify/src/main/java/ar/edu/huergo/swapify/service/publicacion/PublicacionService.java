package ar.edu.huergo.swapify.service.publicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PublicacionMapper publicacionMapper;

    public Publicacion crearPublicacion(CrearPublicacionDTO dto, ar.edu.huergo.swapify.entity.security.Usuario usuario) {
        try {
            if (dto == null) throw new IllegalArgumentException("Datos de publicación inválidos");
            Publicacion p = publicacionMapper.toEntity(dto);
            p.setUsuario(usuario);
            return publicacionRepository.save(p);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear publicación: " + e.getMessage(), e);
        }
    }

    // NUEVO
    public List<Publicacion> listarTodas() {
        try {
            return publicacionRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Error al listar publicaciones: " + e.getMessage(), e);
        }
    }

    // NUEVO
    public Publicacion obtenerPorId(Long id) {
        try {
            return publicacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener publicación por ID: " + e.getMessage(), e);
        }
    }

    public List<Publicacion> obtenerPublicacionesDeFecha(LocalDate fecha) {
        try {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.atTime(LocalTime.MAX);
            return publicacionRepository.findByFechaPublicacionBetween(inicio, fin);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener publicaciones por fecha: " + e.getMessage(), e);
        }
    }

    public BigDecimal sumaPreciosEnFecha(LocalDate fecha) {
        try {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.atTime(LocalTime.MAX);
            BigDecimal suma = publicacionRepository.sumaPreciosEntre(inicio, fin);
            return (suma != null) ? suma : BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("Error al sumar precios en fecha: " + e.getMessage(), e);
        }
    }

    public List<Publicacion> obtenerPublicacionesPorUsuario(Long usuarioId) {
        try {
            return publicacionRepository.findByUsuarioId(usuarioId);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener publicaciones por usuario: " + e.getMessage(), e);
        }
    }
}
