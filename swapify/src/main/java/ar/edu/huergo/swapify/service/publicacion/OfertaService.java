package ar.edu.huergo.swapify.service.publicacion;

import ar.edu.huergo.swapify.dto.publicacion.CrearOfertaDTO;
import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.publicacion.OfertaRepository;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Oferta crearOferta(Long publicacionId, CrearOfertaDTO dto, String username) {
        if (dto == null) {
            throw new IllegalArgumentException("Datos de oferta inválidos");
        }
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Oferta oferta = new Oferta();
        oferta.setPublicacion(publicacion);
        oferta.setUsuario(usuario);
        oferta.setMensaje(dto.getMensaje());
        oferta.setPropuestaObjeto(dto.getPropuestaObjeto());
        oferta.setEstado(EstadoOferta.PENDIENTE);

        return ofertaRepository.save(oferta);
    }

    @Transactional
    public void eliminarPorPublicacion(Long publicacionId) {
        List<Oferta> ofertas = ofertaRepository.findByPublicacionIdOrderByFechaOfertaDesc(publicacionId);
        ofertaRepository.deleteAll(ofertas);
    }

    @Transactional
    public void eliminar(Long ofertaId) {
        ofertaRepository.deleteById(ofertaId);
    }

    @Transactional
    public List<Oferta> listarPorPublicacion(Long publicacionId) {
        List<Oferta> ofertas = ofertaRepository.findByPublicacionIdOrderByFechaOfertaDesc(publicacionId);
        for (Oferta oferta : ofertas) {
            if (oferta.getEstado() == null) {
                oferta.setEstado(EstadoOferta.PENDIENTE);
            }
        }
        ofertas.sort(Comparator
                .comparing((Oferta o) -> prioridadPorEstado(o.getEstado()))
                .thenComparing(Oferta::getFechaOferta, Comparator.nullsLast(Comparator.reverseOrder())));
        return ofertas;
    }

    private int prioridadPorEstado(EstadoOferta estado) {
        if (estado == null) {
            return 3;
        }
        return switch (estado) {
            case PENDIENTE -> 0;
            case ACEPTADA -> 1;
            case RECHAZADA -> 2;
        };
    }

    @Transactional
    public Oferta aceptarOferta(Long publicacionId, Long ofertaId, String username) {
        Oferta oferta = obtenerOfertaParaGestion(publicacionId, ofertaId, username);

        if (oferta.estaAceptada()) {
            return oferta;
        }
        if (oferta.estaRechazada()) {
            throw new IllegalStateException("La oferta ya fue rechazada");
        }

        LocalDateTime ahora = LocalDateTime.now();
        oferta.setEstado(EstadoOferta.ACEPTADA);
        oferta.setFechaRespuesta(ahora);

        List<Oferta> otrasOfertas = ofertaRepository.findByPublicacionIdAndIdNot(publicacionId, ofertaId);
        for (Oferta otra : otrasOfertas) {
            if (otra.estaPendiente()) {
                otra.setEstado(EstadoOferta.RECHAZADA);
                otra.setFechaRespuesta(ahora);
            }
        }

        return oferta;
    }

    @Transactional
    public Oferta rechazarOferta(Long publicacionId, Long ofertaId, String username) {
        Oferta oferta = obtenerOfertaParaGestion(publicacionId, ofertaId, username);

        if (oferta.estaRechazada()) {
            return oferta;
        }
        if (oferta.estaAceptada()) {
            throw new IllegalStateException("La oferta ya fue aceptada");
        }

        LocalDateTime ahora = LocalDateTime.now();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        oferta.setFechaRespuesta(ahora);
        return oferta;
    }

    private Oferta obtenerOfertaParaGestion(Long publicacionId, Long ofertaId, String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Debés iniciar sesión para gestionar ofertas");
        }

        Oferta oferta = ofertaRepository.findByIdAndPublicacionId(ofertaId, publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Oferta no encontrada"));

        Publicacion publicacion = oferta.getPublicacion();
        if (publicacion == null || publicacion.getUsuario() == null
                || !username.equals(publicacion.getUsuario().getUsername())) {
            throw new AccessDeniedException("No tenés permiso para gestionar esta oferta");
        }
        return oferta;
    }
}
