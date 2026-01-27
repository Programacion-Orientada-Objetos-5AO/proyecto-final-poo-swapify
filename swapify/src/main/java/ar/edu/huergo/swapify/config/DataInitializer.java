package ar.edu.huergo.swapify.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.huergo.swapify.entity.security.Rol;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.security.RolRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import ar.edu.huergo.swapify.util.PasswordValidator;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(RolRepository rolRepository, UsuarioRepository usuarioRepository, PasswordEncoder encoder) {
        return args -> {
            Rol admin = rolRepository.findByNombre("ADMIN")
                    .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));
            Rol cliente = rolRepository.findByNombre("CLIENTE")
                    .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

            asegurarUsuario(
                    "admin@huergo.edu.ar",
                    "AdminSuperSegura@123",
                    "Administrador",
                    encoder,
                    usuarioRepository,
                    Set.of(admin, cliente));

            asegurarUsuario(
                    "cliente@huergo.edu.ar",
                    "ClienteSeguro@123",
                    "Cliente ",
                    encoder,
                    usuarioRepository,
                    Set.of(cliente));
        };
    }

    private void asegurarUsuario(String username,
                                 String rawPassword,
                                 String nombre,
                                 PasswordEncoder encoder,
                                 UsuarioRepository usuarioRepository,
                                 Set<Rol> rolesRequeridos) {
        PasswordValidator.validate(rawPassword);

        String usernameNormalizado = normalizarEmail(username);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(usernameNormalizado).orElse(null);
        if (usuario == null) {
            usuario = new Usuario(usernameNormalizado, encoder.encode(rawPassword));
        } else {
            String passwordActual = usuario.getPassword();
            if (passwordActual == null || passwordActual.isBlank()
                    || !encoder.matches(rawPassword, passwordActual)) {
                usuario.setPassword(encoder.encode(rawPassword));
            }
            if (usuario.getRoles() == null) {
                usuario.setRoles(new HashSet<>());
            }
        }

        if (nombre != null && !nombre.isBlank()) {
            usuario.setNombre(nombre.trim());
        }

        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            usuario.setRoles(new HashSet<>(rolesRequeridos));
        } else {
            usuario.getRoles().addAll(rolesRequeridos);
        }

        usuario.setUsername(usernameNormalizado);
        usuarioRepository.save(usuario);
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}


