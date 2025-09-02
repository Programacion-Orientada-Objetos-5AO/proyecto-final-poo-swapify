package ar.edu.huergo.swapify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/publicaciones/**").permitAll()
                .anyRequest().permitAll()  // Permitir todo por simplicidad
            )
            .csrf().disable();  // Deshabilitar CSRF para evitar problemas con Postman

        return http.build();
    }
}
