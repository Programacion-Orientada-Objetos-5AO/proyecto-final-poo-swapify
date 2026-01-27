package ar.edu.huergo.swapify.controller.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.service.security.UsuarioService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/web/mi-cuenta")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CuentaWebController {

    private final UsuarioService usuarioService;

    @GetMapping("/seguridad")
    public String seguridad(Model model) {
        model.addAttribute("titulo", "Seguridad de la cuenta");
        return "auth/seguridad";
    }

    @PostMapping("/seguridad/password")
    public String cambiarPassword(@RequestParam("actual") String actual,
                                  @RequestParam("nueva") String nueva,
                                  @RequestParam("confirmacion") String confirmacion,
                                  RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión nuevamente");
            return "redirect:/web/login";
        }
        try {
            usuarioService.cambiarPasswordPropia(auth.getName(), actual, nueva, confirmacion);
            ra.addFlashAttribute("success", "Actualizaste tu contraseña correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No pudimos actualizar la contraseña: " + e.getMessage());
        }
        return "redirect:/web/mi-cuenta/seguridad";
    }

    @PostMapping("/seguridad/nombre")
    public String cambiarNombre(@RequestParam("nombre") String nombre,
                                RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión nuevamente");
            return "redirect:/web/login";
        }
        try {
            usuarioService.cambiarNombrePropio(auth.getName(), nombre);
            ra.addFlashAttribute("success", "Actualizaste tu nombre de usuario correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No pudimos actualizar el nombre: " + e.getMessage());
        }
        return "redirect:/web/mi-cuenta/seguridad";
    }
}
