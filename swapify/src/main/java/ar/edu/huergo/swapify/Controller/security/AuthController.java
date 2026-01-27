package ar.edu.huergo.swapify.controller.security;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.huergo.swapify.dto.security.LoginDTO;
import ar.edu.huergo.swapify.service.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de autenticación para el consumo del cliente SPA o móvil.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    /**
     * Autentica credenciales y devuelve un token JWT con los roles asociados.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid LoginDTO request) {
        String usernameNormalizado = normalizarEmail(request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameNormalizado, request.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameNormalizado);
        List<String> roles =
                userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        String token = jwtTokenService.generarToken(userDetails, roles);
        return ResponseEntity.ok(Map.of("token", token));
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}


