package ar.edu.huergo.swapify.service.publicacion;

import ar.edu.huergo.swapify.dto.publicacion.CrearOfertaDTO;
import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.entity.publicacion.EstadoPublicacion;
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
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Necesitás iniciar sesión para ofertar");
        }

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));

        if (!publicacion.estaActiva()) {
            throw new IllegalStateException("La publicación no acepta nuevas ofertas en este momento");
        }

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (publicacion.getUsuario() != null && publicacion.getUsuario().getUsername() != null
                && publicacion.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())) {
            throw new IllegalStateException("No podés ofertar en tu propia publicación");
        }

        boolean yaOferto = ofertaRepository
                .existsByPublicacionIdAndUsuarioUsernameIgnoreCase(publicacionId, usuario.getUsername());
        if (yaOferto) {
            throw new IllegalStateException("Ya enviaste una oferta para esta publicación");
        }

        Oferta oferta = new Oferta();
        oferta.setPublicacion(publicacion);
        oferta.setUsuario(usuario);
        oferta.setMensaje(dto.getMensaje());
        oferta.setPropuestaObjeto(dto.getPropuestaObjeto());
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(LocalDateTime.now());

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
    public Oferta aceptarOferta(Long publicacionId, Long ofertaId, String username, boolean esAdmin) {
        Oferta oferta = obtenerOfertaParaGestion(publicacionId, ofertaId, username, esAdmin);

        if (oferta.estaAceptada()) {
            return oferta;
        }
        if (oferta.estaRechazada()) {
            throw new IllegalStateException("La oferta ya fue rechazada");
        }

        LocalDateTime ahora = LocalDateTime.now();
        oferta.setEstado(EstadoOferta.ACEPTADA);
        oferta.setFechaRespuesta(ahora);

        Publicacion publicacion = oferta.getPublicacion();
        if (publicacion != null) {
            publicacion.marcarEnNegociacion(ahora);
            publicacionRepository.save(publicacion);
        }

        List<Oferta> otrasOfertas = ofertaRepository.findByPublicacionIdAndIdNot(publicacionId, ofertaId);
        boolean huboCambios = false;
        for (Oferta otra : otrasOfertas) {
            if (otra.estaPendiente()) {
                otra.setEstado(EstadoOferta.RECHAZADA);
                otra.setFechaRespuesta(ahora);
                huboCambios = true;
            }
        }
        if (huboCambios) {
            ofertaRepository.saveAll(otrasOfertas);
        }

        return ofertaRepository.save(oferta);
    }

    @Transactional
    public Oferta rechazarOferta(Long publicacionId, Long ofertaId, String username, boolean esAdmin) {
        Oferta oferta = obtenerOfertaParaGestion(publicacionId, ofertaId, username, esAdmin);

        if (oferta.estaRechazada()) {
            return oferta;
        }
        if (oferta.estaAceptada()) {
            throw new IllegalStateException("La oferta ya fue aceptada");
        }

        LocalDateTime ahora = LocalDateTime.now();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        oferta.setFechaRespuesta(ahora);
        return ofertaRepository.save(oferta);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Oferta> obtenerOfertaAceptada(Long publicacionId) {
        return ofertaRepository.findFirstByPublicacionIdAndEstadoOrderByFechaRespuestaDesc(publicacionId,
                EstadoOferta.ACEPTADA);
    }

    @Transactional(readOnly = true)
    public long contarPorEstado(EstadoOferta estado) {
        return ofertaRepository.countByEstado(estado);
    }

    @Transactional(readOnly = true)
    public long contarTotal() {
        return ofertaRepository.count();
    }

    private Oferta obtenerOfertaParaGestion(Long publicacionId, Long ofertaId, String username, boolean esAdmin) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Debés iniciar sesión para gestionar ofertas");
        }

        Oferta oferta = ofertaRepository.findByIdAndPublicacionId(ofertaId, publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Oferta no encontrada"));

        Publicacion publicacion = oferta.getPublicacion();
        boolean esPropietario = publicacion != null && publicacion.getUsuario() != null
                && publicacion.getUsuario().getUsername() != null
                && publicacion.getUsuario().getUsername().equalsIgnoreCase(username.trim());
        if (!esPropietario && !esAdmin) {
            throw new AccessDeniedException("No tenés permiso para gestionar esta oferta");
        }
        if (publicacion != null && EstadoPublicacion.FINALIZADA.equals(publicacion.getEstado())) {
            throw new IllegalStateException("La publicación ya fue finalizada");
        }
        return oferta;
    }
}
