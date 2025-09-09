package ar.edu.huergo.swapify.Controller.security;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid LoginDTO request) {
        // 1) Autenticar credenciales username/password (lanza excepción si no son válidas)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // 2) Cargar UserDetails y derivar roles/authorities
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        List<String> roles =
                userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        // 3) Generar token JWT firmado con el username como subject y los roles como claims
        String token = jwtTokenService.generarToken(userDetails, roles);
        // 4) Responder con el token (el cliente deberá enviarlo en el header Authorization)
        return ResponseEntity.ok(Map.of("token", token));
    }
}


