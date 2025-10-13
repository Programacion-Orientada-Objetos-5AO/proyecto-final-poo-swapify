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
import ar.edu.huergo.swapify.service.security.NotificacionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfertaService {

    private static final long MAX_IMAGE_BYTES = 3_000_000L;

    private final OfertaRepository ofertaRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

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

        procesarImagenOferta(dto, oferta);

        Oferta guardada = ofertaRepository.save(oferta);
        prepararOfertaParaLectura(guardada);
        notificacionService.notificarNuevaOferta(publicacion, guardada);
        return guardada;
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
            prepararOfertaParaLectura(oferta);
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
                notificacionService.notificarOfertaRechazada(otra);
            }
        }
        if (huboCambios) {
            ofertaRepository.saveAll(otrasOfertas);
        }

        Oferta guardada = ofertaRepository.save(oferta);
        prepararOfertaParaLectura(guardada);
        notificacionService.notificarOfertaAceptada(guardada);
        return guardada;
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
        Oferta guardada = ofertaRepository.save(oferta);
        prepararOfertaParaLectura(guardada);
        notificacionService.notificarOfertaRechazada(guardada);
        return guardada;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Oferta> obtenerOfertaAceptada(Long publicacionId) {
        java.util.Optional<Oferta> oferta = ofertaRepository.findFirstByPublicacionIdAndEstadoOrderByFechaRespuestaDesc(publicacionId,
                EstadoOferta.ACEPTADA);
        oferta.ifPresent(this::prepararOfertaParaLectura);
        return oferta;
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
        prepararOfertaParaLectura(oferta);
        return oferta;
    }

    private void procesarImagenOferta(CrearOfertaDTO dto, Oferta oferta) {
        if (!dto.tieneImagen()) {
            oferta.setImagen(null);
            oferta.setImagenContentType(null);
            return;
        }
        byte[] datos = null;
        String contentType = dto.getImagenContentType();
        if (dto.getImagenArchivo() != null && !dto.getImagenArchivo().isEmpty()) {
            MultipartFile archivo = dto.getImagenArchivo();
            try {
                datos = archivo.getBytes();
                contentType = archivo.getContentType();
            } catch (IOException e) {
                throw new IllegalArgumentException("No pudimos leer la imagen adjunta: " + e.getMessage(), e);
            }
        } else if (dto.getImagenBase64() != null && !dto.getImagenBase64().isBlank()) {
            String base64 = dto.getImagenBase64();
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(',') + 1);
            }
            datos = decodificarBase64(base64);
        }
        if (datos == null || datos.length == 0) {
            return;
        }
        if (datos.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen de la oferta supera el tamaño permitido (3 MB)");
        }
        if (!esImagenValida(datos)) {
            throw new IllegalArgumentException("El archivo adjunto no es una imagen válida");
        }
        oferta.setImagen(datos);
        oferta.setImagenContentType(contentType != null ? contentType : "image/jpeg");
    }

    private boolean esImagenValida(byte[] datos) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(datos)) {
            return ImageIO.read(bais) != null;
        } catch (IOException e) {
            log.warn("No se pudo validar la imagen adjunta", e);
            return false;
        }
    }

    private byte[] decodificarBase64(String base64Data) {
        if (base64Data == null) {
            return new byte[0];
        }
        StringBuilder limpio = new StringBuilder(base64Data.length());
        for (int i = 0; i < base64Data.length(); i++) {
            char c = base64Data.charAt(i);
            if (c == ' ') {
                limpio.append('+');
            } else if (!Character.isWhitespace(c)) {
                limpio.append(c);
            }
        }
        if (limpio.length() == 0) {
            return new byte[0];
        }
        return Base64.getMimeDecoder().decode(limpio.toString());
    }

    private void prepararOfertaParaLectura(Oferta oferta) {
        if (oferta == null) {
            return;
        }
        if (oferta.tieneImagen()) {
            byte[] imagen = oferta.getImagen();
            String base64 = Base64.getEncoder().encodeToString(imagen);
            String contentType = oferta.getImagenContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }
            oferta.setImagenBase64(base64);
            oferta.setImagenDataUri("data:" + contentType + ";base64," + base64);
        } else {
            oferta.setImagenBase64(null);
            oferta.setImagenDataUri(null);
        }
    }
}
