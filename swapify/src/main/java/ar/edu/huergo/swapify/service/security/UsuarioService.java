package ar.edu.huergo.swapify.service.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Rol;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.publicacion.OfertaRepository;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import ar.edu.huergo.swapify.repository.security.NotificacionRepository;
import ar.edu.huergo.swapify.repository.security.RolRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import ar.edu.huergo.swapify.service.security.NotificacionService;
import ar.edu.huergo.swapify.util.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final OfertaRepository ofertaRepository;
    private final PublicacionRepository publicacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario registrar(Usuario usuario, String password, String verificacionPassword) {
        if (password == null || verificacionPassword == null) {
            throw new IllegalArgumentException("Las contraseñas no pueden ser null");
        }
        if (!password.equals(verificacionPassword)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        PasswordValidator.validate(password);
        String usernameNormalizado = normalizarEmail(usuario.getUsername());
        usuario.setUsername(usernameNormalizado);
        if (usuarioRepository.existsByUsernameIgnoreCase(usernameNormalizado)) {
            throw new IllegalArgumentException("El email ya está en uso");
        }
        if (usuario.getNombre() != null && !usuario.getNombre().trim().isEmpty()) {
            String nombreNormalizado = usuario.getNombre().trim();
            if (usuarioRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
                throw new IllegalArgumentException("El nombre de usuario ya está en uso");
            }
            usuario.setNombre(nombreNormalizado);
        }

        usuario.setPassword(passwordEncoder.encode(password));
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow(() -> new IllegalArgumentException("Rol 'CLIENTE' no encontrado"));
        usuario.setRoles(new java.util.HashSet<>(Set.of(rolCliente)));
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (usuario.esAdministrador()) {
            throw new IllegalArgumentException("No se pueden eliminar cuentas administrativas desde este panel");
        }

        ofertaRepository.deleteByUsuarioId(usuarioId);

        List<Publicacion> publicaciones = publicacionRepository.findByUsuarioId(usuarioId);
        for (Publicacion publicacion : publicaciones) {
            if (publicacion.getId() != null) {
                ofertaRepository.deleteByPublicacionId(publicacion.getId());
            }
        }
        ofertaRepository.flush();

        if (!publicaciones.isEmpty()) {
            publicacionRepository.deleteAll(publicaciones);
            publicacionRepository.flush();
        }

        notificacionRepository.deleteByUsuarioId(usuarioId);
        notificacionRepository.flush();

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public Usuario banearUsuario(Long usuarioId, LocalDateTime hasta, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.esAdministrador()) {
            throw new IllegalArgumentException("No se pueden suspender cuentas administrativas");
        }
        usuario.setBaneadoHasta(hasta);
        usuario.setMotivoBan(motivo);
        notificacionService.notificarBan(usuario, hasta, motivo);
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional
    public Usuario levantarBan(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        usuario.setBaneadoHasta(null);
        usuario.setMotivoBan(null);
        notificacionService.notificarBan(usuario, null, null);
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional
    public void cambiarPasswordPropia(String username, String actual, String nueva, String confirmacion) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        if (nueva == null || confirmacion == null || !nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas nuevas no coinciden");
        }
        PasswordValidator.validate(nueva);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(normalizarEmail(username))
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (actual == null || !passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es válida");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
    }

    @Transactional
    public void cambiarNombrePropio(String username, String nombre) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(normalizarEmail(username))
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        String nombreNormalizado = nombre.trim();
        if (usuarioRepository.existsByNombreIgnoreCase(nombreNormalizado) &&
            !nombreNormalizado.equalsIgnoreCase(usuario.getNombre())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        usuario.setNombre(nombreNormalizado);
        usuarioRepository.saveAndFlush(usuario);
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
