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

    public Publicacion crearPublicacion(CrearPublicacionDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Datos de publicación inválidos");
        Publicacion p = publicacionMapper.toEntity(dto);
        return publicacionRepository.save(p);
    }

    // NUEVO
    public List<Publicacion> listarTodas() {
        return publicacionRepository.findAll();
    }

    // NUEVO
    public Publicacion obtenerPorId(Long id) {
        return publicacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
    }

    public List<Publicacion> obtenerPublicacionesDeFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return publicacionRepository.findByFechaPublicacionBetween(inicio, fin);
    }

    public BigDecimal sumaPreciosEnFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(inicio, fin);
        return (suma != null) ? suma : BigDecimal.ZERO;
    }
}
