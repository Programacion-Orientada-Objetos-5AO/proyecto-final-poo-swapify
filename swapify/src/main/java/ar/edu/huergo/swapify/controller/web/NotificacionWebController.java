package ar.edu.huergo.swapify.controller.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.service.security.NotificacionService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/web/notificaciones")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificacionWebController {

    private final NotificacionService notificacionService;

    @GetMapping
    public String listar(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return "redirect:/web/login";
        }
        model.addAttribute("titulo", "Notificaciones");
        model.addAttribute("notificaciones", notificacionService.obtenerUltimas(auth.getName()));
        return "auth/notificaciones";
    }

    @PostMapping("/{id}/leer")
    public String marcarLeida(@PathVariable("id") Long id, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                notificacionService.marcarComoLeida(id, auth.getName());
            } catch (Exception e) {
                ra.addFlashAttribute("error", "No pudimos actualizar la notificación: " + e.getMessage());
            }
        }
        return "redirect:/web/notificaciones";
    }

    @PostMapping("/todas/leidas")
    public String marcarTodas(RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                notificacionService.marcarTodasComoLeidas(auth.getName());
            } catch (Exception e) {
                ra.addFlashAttribute("error", "No pudimos actualizar las notificaciones: " + e.getMessage());
            }
        }
        return "redirect:/web/notificaciones";
    }
}
