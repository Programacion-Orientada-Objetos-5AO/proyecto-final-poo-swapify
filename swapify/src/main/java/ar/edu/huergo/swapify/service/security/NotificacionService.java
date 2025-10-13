package ar.edu.huergo.swapify.service.security;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Notificacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.security.NotificacionRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void notificarNuevaOferta(Publicacion publicacion, Oferta oferta) {
        if (publicacion == null || publicacion.getUsuario() == null) {
            return;
        }
        Usuario destinatario = publicacion.getUsuario();
        String titulo = "Recibiste una nueva oferta";
        String mensaje = String.format("%s dejó una propuesta sobre %s.",
                oferta.getUsuario() != null ? oferta.getUsuario().getUsername() : "Una persona usuaria",
                publicacion.getNombre());
        crearNotificacion(destinatario, titulo, mensaje, "OFERTA", "/web/publicaciones/" + publicacion.getId());
    }

    @Transactional
    public void notificarOfertaAceptada(Oferta oferta) {
        if (oferta == null || oferta.getUsuario() == null || oferta.getPublicacion() == null) {
            return;
        }
        Usuario destinatario = oferta.getUsuario();
        String titulo = "¡Tu oferta fue aceptada!";
        String mensaje = String.format("Coordiná el intercambio de %s. El propietario ya reservó la publicación.",
                oferta.getPublicacion().getNombre());
        crearNotificacion(destinatario, titulo, mensaje, "OFERTA_ACEPTADA",
                "/web/publicaciones/" + oferta.getPublicacion().getId());
    }

    @Transactional
    public void notificarOfertaRechazada(Oferta oferta) {
        if (oferta == null || oferta.getUsuario() == null || oferta.getPublicacion() == null) {
            return;
        }
        Usuario destinatario = oferta.getUsuario();
        String titulo = "Tu oferta fue respondida";
        String mensaje = String.format("La publicación %s rechazó tu propuesta. Podés explorar alternativas.",
                oferta.getPublicacion().getNombre());
        crearNotificacion(destinatario, titulo, mensaje, "OFERTA_RECHAZADA",
                "/web/publicaciones/" + oferta.getPublicacion().getId());
    }

    @Transactional
    public void notificarBan(Usuario usuario, LocalDateTime hasta, String motivo) {
        if (usuario == null) {
            return;
        }
        String titulo = hasta != null && hasta.isAfter(LocalDateTime.now())
                ? "Tu cuenta fue suspendida temporalmente"
                : "Tu cuenta quedó habilitada nuevamente";
        StringBuilder mensaje = new StringBuilder();
        if (hasta != null && hasta.isAfter(LocalDateTime.now())) {
            mensaje.append("No podrás iniciar sesión hasta el ")
                    .append(hasta.toLocalDate())
                    .append(".");
            if (motivo != null && !motivo.isBlank()) {
                mensaje.append(" Motivo: ").append(motivo);
            }
        } else {
            mensaje.append("Podés volver a usar Swapify con normalidad. Recordá respetar las normas de convivencia.");
        }
        crearNotificacion(usuario, titulo, mensaje.toString(), "ADMIN", "/web/publicaciones");
    }

    @Transactional(readOnly = true)
    public List<Notificacion> obtenerUltimas(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .map(usuario -> notificacionRepository.findTop50ByUsuarioIdOrderByFechaCreacionDesc(usuario.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .map(usuario -> notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()))
                .orElse(0L);
    }

    @Transactional
    public void marcarComoLeida(Long id, String username) {
        if (id == null || username == null || username.isBlank()) {
            return;
        }
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notificación no encontrada"));
        if (!notificacion.getUsuario().getUsername().equalsIgnoreCase(username)) {
            throw new EntityNotFoundException("Notificación no encontrada");
        }
        notificacion.setLeida(true);
    }

    @Transactional
    public void marcarTodasComoLeidas(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        usuarioRepository.findByUsernameIgnoreCase(username).ifPresent(usuario -> {
            List<Notificacion> notificaciones = notificacionRepository
                    .findTop50ByUsuarioIdOrderByFechaCreacionDesc(usuario.getId());
            notificaciones.forEach(n -> n.setLeida(true));
        });
    }

    private void crearNotificacion(Usuario usuario, String titulo, String mensaje, String tipo, String enlace) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setEnlace(enlace);
        if ("OFERTA".equals(tipo)) {
            notificacion.setIcono("bi-chat-dots");
        } else if ("OFERTA_ACEPTADA".equals(tipo)) {
            notificacion.setIcono("bi-handshake");
        } else if ("OFERTA_RECHAZADA".equals(tipo)) {
            notificacion.setIcono("bi-x-circle");
        } else {
            notificacion.setIcono("bi-exclamation-triangle");
        }
        notificacionRepository.save(notificacion);
    }
}
