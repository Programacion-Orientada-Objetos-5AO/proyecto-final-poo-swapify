package ar.edu.huergo.swapify.controller.web;

import java.util.List;
import java.util.Map;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.dto.security.RegistrarDTO;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.service.security.JwtTokenService;
import ar.edu.huergo.swapify.service.security.UsuarioService;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador web (Thymeleaf) para vistas de autenticación
 * Sirve páginas HTML para login y registro
 */
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
public class AuthWebController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    /** Página de login */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("titulo", "Iniciar sesión");
        return "auth/login";
    }

    /** Página de registro */
    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("titulo", "Crear cuenta");
        return "auth/registro";
    }

    /** Procesar registro desde formulario */
    @PostMapping("/registro")
    public String registrar(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String verificacionPassword,
                           RedirectAttributes ra) {
        try {
            // Crear DTO para validación
            RegistrarDTO registrarDTO = new RegistrarDTO(username, password, verificacionPassword);

            // Validar manualmente (ya que no usamos @Valid aquí)
            if (!password.equals(verificacionPassword)) {
                ra.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:/web/registro";
            }

            // Registrar usuario
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            Usuario nuevoUsuario = usuarioService.registrar(usuario, password, verificacionPassword);

            ra.addFlashAttribute("success", "Cuenta creada exitosamente. Ahora puedes iniciar sesión.");
            return "redirect:/web/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
            return "redirect:/web/registro";
        }
    }

    /** Procesar login desde AJAX y devolver token */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public String loginDesdeFormulario(@RequestParam String username,
                                       @RequestParam String password,
                                       RedirectAttributes ra,
                                       HttpServletResponse response) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();

            String token = jwtTokenService.generarToken(userDetails, roles);
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setMaxAge(86400); // 1 día
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

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> loginAjax(@RequestParam String username, @RequestParam String password) {
        try {
            // 1) Autenticar credenciales
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // 2) Cargar UserDetails y roles
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();

            // 3) Generar token JWT
            String token = jwtTokenService.generarToken(userDetails, roles);

            ResponseCookie cookie = ResponseCookie.from("jwtToken", token)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .httpOnly(true)
                .build();

            // 4) Responder con el token
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

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
}
