package ar.edu.huergo.swapify.service.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio responsable de crear y validar tokens JWT sin estado en el servidor.
 */
@Service
public class JwtTokenService {

    /**
     * Clave secreta usada para firmar y verificar tokens. Se crea en el constructor desde
     * application.properties (security.jwt.secret).
     */
    private final SecretKey signingKey;

    /**
     * Tiempo de vida del token en milisegundos. Se inyecta desde application.properties
     * (security.jwt.expiration-ms).
     */
    @Value("${security.jwt.expiration-ms}")
    private long expirationMillis;

    /**
     * Construye el servicio materializando la clave HMAC a partir de la cadena configurada.
     */
    public JwtTokenService(@Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * Genera un JWT para el usuario autenticado incluyendo los roles como claim.
     */
    public String generarToken(UserDetails userDetails, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        return Jwts.builder().subject(userDetails.getUsername()).issuedAt(Date.from(now))
                .expiration(Date.from(expiry)).claims(Map.of("roles", roles)).signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el nombre de usuario (subject) del token. Dispara una excepción si la firma no es
     * válida o el token es malformado.
     */
    public String extraerUsername(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload()
                .getSubject();
    }

    /**
     * Verifica que el token sea válido para el usuario dado comprobando firma, sujeto y expiración.
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            var payload = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)
                    .getPayload();

            String username = payload.getSubject();
            Date expiration = payload.getExpiration();
            return username != null && username.equals(userDetails.getUsername())
                    && expiration.after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }
}


