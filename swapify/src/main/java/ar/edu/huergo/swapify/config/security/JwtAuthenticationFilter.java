package ar.edu.huergo.swapify.config.security;

import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.edu.huergo.swapify.service.security.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    // Este método se ejecuta en cada solicitud HTTP que llega a la aplicación.
    // Flujo resumido del filtro:
    // 1) Lee el header Authorization y extrae el token si comienza con "Bearer ".
    // 2) Usa JwtTokenService para obtener el username del token.
    // 3) Si no hay autenticación previa en el contexto y el token es válido,
    // crea un UsernamePasswordAuthenticationToken con las autoridades del usuario
    // y lo coloca en el SecurityContext.
    // 4) Continúa la cadena de filtros para que el request llegue al controlador.
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization"); //Obtiene el header Authorization
        // Verifica si el header no es nulo y comienza con "Bearer "
        // (es el formato estándar para tokens JWT).
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = null;
            // Intenta extraer el username del token usando JwtTokenService.
            // Si falla, username se queda como null.
            try {
                username = jwtTokenService.extraerUsername(token);
            } catch (Exception ignored) {
            }

            // Si hay un username y no hay autenticación previa en el contexto,
            // carga los detalles del usuario desde el UserDetailsService.
            // Si el token es válido, crea un UsernamePasswordAuthenticationToken
            // con las autoridades del usuario y lo coloca en el SecurityContext.
            // Esto permite que el usuario esté autenticado para el resto del request.
            // Si el token no es válido, no se hace nada y el request sigue sin autenticación.
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtTokenService.esTokenValido(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null,
                                    userDetails.getAuthorities());
                    authToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}


