package ar.edu.huergo.swapify.controller.web;

import java.util.List;
import java.util.Map;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.dto.security.LoginDTO;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.service.security.JwtTokenService;
import ar.edu.huergo.swapify.service.security.UsuarioService;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Controlador web encargado del flujo de autenticación y registro en las
 * vistas Thymeleaf.
 */
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
public class AuthWebController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    /** Duración en segundos del token persistido en la cookie del navegador. */
    private static final int COOKIE_MAX_AGE_SECONDS = 86400;

    /**
     * Renderiza el formulario de inicio de sesión.
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("titulo", "Iniciar sesión");
        return "auth/login";
    }

    /**
     * Muestra el formulario para crear una nueva cuenta.
     */
    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("titulo", "Crear cuenta");
        return "auth/registro";
    }

    /**
     * Procesa la creación de una cuenta desde el formulario web.
     */
    @PostMapping("/registro")
    public String registrar(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String verificacionPassword,
                           RedirectAttributes ra) {
        try {
            if (!password.equals(verificacionPassword)) {
                ra.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:/web/registro";
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(normalizarEmail(username));
            usuarioService.registrar(usuario, password, verificacionPassword);

            ra.addFlashAttribute("success", "Cuenta creada exitosamente. Ahora puedes iniciar sesión.");
            return "redirect:/web/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
            return "redirect:/web/registro";
        }
    }

    /**
     * Autentica desde el formulario tradicional y persiste el token en una
     * cookie para el uso posterior en las vistas.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public String loginDesdeFormulario(@RequestParam String username,
                                       @RequestParam String password,
                                       RedirectAttributes ra,
                                       HttpServletResponse response) {
        try {
            String usuarioNormalizado = normalizarEmail(username);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usuarioNormalizado, password));
            UserDetails userDetails = userDetailsService.loadUserByUsername(usuarioNormalizado);
            List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();

            String token = jwtTokenService.generarToken(userDetails, roles);
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);

            ra.addFlashAttribute("success", "Sesión iniciada correctamente");
            return "redirect:/web/publicaciones";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Credenciales inválidas");
            return "redirect:/web/login";
        }
    }

    /**
     * Autentica desde solicitudes AJAX devolviendo el token firmado en el cuerpo
     * de la respuesta y mediante cookie.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> loginAjax(@RequestBody @Valid LoginDTO payload) {
        try {
            if (payload == null || payload.username() == null || payload.username().isBlank()
                    || payload.password() == null || payload.password().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Usuario y contraseña son obligatorios"));
            }

            String usuarioNormalizado = normalizarEmail(payload.username());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuarioNormalizado, payload.password()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(usuarioNormalizado);
            List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();

            String token = jwtTokenService.generarToken(userDetails, roles);

            ResponseCookie cookie = ResponseCookie.from("jwtToken", token)
                .path("/")
                .maxAge(Duration.ofSeconds(COOKIE_MAX_AGE_SECONDS))
                .httpOnly(true)
                .build();

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Solicitud inválida");
        return Map.of("error", mensaje);
    }

    /**
     * Elimina el token del cliente y limpia el contexto de seguridad.
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes ra) {
        Cookie cookie = new Cookie("jwtToken", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        ra.addFlashAttribute("success", "Sesión cerrada correctamente");
        return "redirect:/web/publicaciones";
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
