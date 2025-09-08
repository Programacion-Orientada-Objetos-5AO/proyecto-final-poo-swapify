package ar.edu.huergo.swapify.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.huergo.swapify.entity.usuario.Usuario;
import ar.edu.huergo.swapify.repository.usuario.UsuarioRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UsuarioRepository usuarioRepository, PasswordEncoder encoder) {
        return args -> {
            // Crear usuario admin si no existe
            if (usuarioRepository.findByMailIgnoreCase("admin@swapify.edu.ar").isEmpty()) {
                String adminPassword = "AdminSuperSegura@123";
                Usuario u = new Usuario();
                u.setMail("admin@swapify.edu.ar");
                u.setNombre("Administrador");
                u.setNumeroDeTelefono("0000000000");
                u.setContraseña(encoder.encode(adminPassword));
                usuarioRepository.save(u);
            }

            // Crear usuario cliente si no existe
            if (usuarioRepository.findByMailIgnoreCase("cliente@swapify.edu.ar").isEmpty()) {
                String clientePassword = "ClienteSuperSegura@123";
                Usuario u = new Usuario();
                u.setMail("cliente@swapify.edu.ar");
                u.setNombre("Cliente");
                u.setNumeroDeTelefono("1111111111");
                u.setContraseña(encoder.encode(clientePassword));
                usuarioRepository.save(u);
            }
        };
    }
}
