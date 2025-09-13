package ar.edu.huergo.swapify.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // Configuración central de Spring Security con JWT:
        // - Deshabilitamos CSRF porque no usamos cookies/sesiones en un API stateless.
        // - Forzamos manejo de sesión sin estado (los datos de auth vienen en el JWT).
        // - Permitimos libre acceso solo al login, el resto requiere autenticación y roles.
        // - Registramos nuestro filtro JWT antes del filtro de usuario/contraseña.
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registrar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/publicaciones").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/reporte").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(
                        exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler())
                                .authenticationEntryPoint(authenticationEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(java.util.Map.of("type",
                    "https://http.dev/problems/access-denied", "title", "Acceso denegado", "status",
                    403, "detail", "No tienes permisos para acceder a este recurso"));

            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(java.util.Map.of("type",
                    "https://http.dev/problems/unauthorized", "title", "No autorizado", "status",
                    401, "detail", "Credenciales inválidas o faltantes"));

            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        // Adaptamos nuestra entidad Usuario a UserDetails de Spring Security.
        return username -> usuarioRepository.findByUsername(username)
                .map(usuario -> org.springframework.security.core.userdetails.User
                        .withUsername(usuario.getUsername()).password(usuario.getPassword())
                        .roles(usuario.getRoles().stream().map(r -> r.getNombre())
                                .toArray(String[]::new))
                        .build())
                .orElseThrow(
                        () -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        // Provider de autenticación que usa nuestro UserDetailsService y el encoder
        // para validar credentials en /api/auth/login.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        // Exponemos el AuthenticationManager que usará el controlador de login.
        return configuration.getAuthenticationManager();
    }
}


