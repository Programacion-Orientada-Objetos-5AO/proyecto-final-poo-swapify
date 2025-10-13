package ar.edu.huergo.swapify.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
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

/**
 * Configura los componentes de seguridad de la aplicación combinando
 * autenticación basada en JWT para el API y formularios protegidos para las
 * vistas web.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Define la cadena de filtros con las reglas de autorización y la integración
     * del filtro JWT.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/web/publicaciones", "/web/publicaciones/{id}", "/web/acerca",
                                 "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/registrar").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/web/login").permitAll()
                .requestMatchers("/web/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/web/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/publicaciones").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/publicaciones/reporte").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/web/admin/**").hasRole("ADMIN")
                .requestMatchers("/web/publicaciones/nueva").authenticated()
                .requestMatchers(HttpMethod.POST, "/web/publicaciones").authenticated()
                .anyRequest().permitAll()
            )

            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint(authenticationEntryPoint())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proveedor de hashing para las credenciales persistidas.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Genera una respuesta Problem Details estándar cuando la persona autenticada
     * carece de permisos.
     */
    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(java.util.Map.of(
                "type", "https://http.dev/problems/access-denied",
                "title", "Acceso denegado",
                "status", 403,
                "detail", "No tienes permisos para acceder a este recurso"
            ));

            response.getWriter().write(jsonResponse);
        };
    }

    /**
     * Responde con Problem Details cuando la solicitud carece de credenciales
     * válidas.
     */
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(java.util.Map.of(
                "type", "https://http.dev/problems/unauthorized",
                "title", "No autorizado",
                "status", 401,
                "detail", "Credenciales inválidas o faltantes"
            ));

            response.getWriter().write(jsonResponse);
        };
    }

    /**
     * Carga la información de usuarios desde la base de datos y la adapta a la
     * interfaz que espera Spring Security.
     */
    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsernameIgnoreCase(username)
            .map(usuario -> {
                if (usuario.estaBaneado()) {
                    throw new DisabledException("Cuenta suspendida hasta " + usuario.getBaneadoHasta());
                }
                return org.springframework.security.core.userdetails.User
                        .withUsername(usuario.getUsername())
                        .password(usuario.getPassword())
                        .roles(usuario.getRoles().stream().map(r -> r.getNombre()).toArray(String[]::new))
                        .build();
            })
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    /**
     * Configura el proveedor de autenticación DAO reutilizando el encoder y el
     * servicio de usuarios.
     */
    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} construido por Spring para el uso
     * de los controladores.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
