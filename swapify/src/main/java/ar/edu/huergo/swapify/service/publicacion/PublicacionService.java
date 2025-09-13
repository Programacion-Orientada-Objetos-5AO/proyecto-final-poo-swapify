package ar.edu.huergo.swapify.service.publicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ar.edu.huergo.swapify.repository.security.UsuarioRepository usuarioRepository;

    @Transactional
    public Publicacion crearPublicacion(CrearPublicacionDTO dto, ar.edu.huergo.swapify.entity.security.Usuario usuario) {
        if (dto == null) throw new IllegalArgumentException("Datos de publicación inválidos");
        var managedUsuario = usuarioRepository.findByUsername(usuario.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Publicacion p = publicacionMapper.toEntity(dto);
        p.setUsuario(managedUsuario);
        p.setFechaPublicacion(LocalDateTime.now());
        return publicacionRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Publicacion> listarTodas() {
        return publicacionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Publicacion obtenerPorId(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesDeFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return publicacionRepository.findByFechaPublicacionBetween(inicio, fin);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumaPreciosEnFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(inicio, fin);
        return (suma != null) ? suma : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Publicacion> obtenerPublicacionesPorUsuario(Long usuarioId) {
        return publicacionRepository.findByUsuarioId(usuarioId);
    }
}
