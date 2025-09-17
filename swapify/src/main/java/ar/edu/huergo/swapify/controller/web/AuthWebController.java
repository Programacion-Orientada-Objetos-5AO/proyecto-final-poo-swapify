package ar.edu.huergo.swapify.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.dto.security.RegistrarDTO;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.service.security.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador web (Thymeleaf) para vistas de autenticación
 * Sirve páginas HTML para login y registro
 */
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
public class AuthWebController {

    private final UsuarioService usuarioService;

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
}
