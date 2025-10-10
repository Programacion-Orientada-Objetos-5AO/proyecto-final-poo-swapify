package ar.edu.huergo.swapify.controller.web;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Rol;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.service.publicacion.OfertaService;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import ar.edu.huergo.swapify.service.security.UsuarioService;
import ar.edu.huergo.swapify.util.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;

@Controller
@RequestMapping("/web/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {

    private final PublicacionService publicacionService;
    private final OfertaService ofertaService;
    private final UsuarioService usuarioService;

    @GetMapping
    public String panel(Model model) {
        List<Usuario> usuarios = usuarioService.getAllUsuarios().stream()
                .sorted(Comparator.comparing(Usuario::getUsername))
                .toList();
        List<Publicacion> publicaciones = publicacionService.listarTodas();

        Map<Long, String> rolesPorUsuario = new LinkedHashMap<>();
        for (Usuario usuario : usuarios) {
            Long usuarioId = usuario.getId();
            if (usuarioId == null) {
                continue;
            }
            String roles = (usuario.getRoles() == null || usuario.getRoles().isEmpty())
                    ? "Sin roles"
                    : usuario.getRoles().stream()
                            .map(Rol::getNombre)
                            .filter(nombre -> nombre != null && !nombre.isBlank())
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.joining(", "));
            rolesPorUsuario.put(usuarioId, roles);
        }

        Map<Long, Long> publicacionesPorUsuario = publicaciones.stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getId() != null)
                .collect(Collectors.groupingBy(p -> p.getUsuario().getId(), Collectors.counting()));

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("publicacionesPorUsuario", publicacionesPorUsuario);
        model.addAttribute("rolesPorUsuario", rolesPorUsuario);
        model.addAttribute("resumenPublicaciones", Map.of(
                "total", Long.valueOf(publicaciones.size()),
                "activas", publicaciones.stream().filter(Publicacion::estaActiva).count(),
                "enNegociacion", publicaciones.stream().filter(Publicacion::estaEnNegociacion).count(),
                "finalizadas", publicaciones.stream().filter(Publicacion::estaFinalizada).count(),
                "oficiales", publicaciones.stream().filter(Publicacion::isOficial).count()
        ));
        model.addAttribute("resumenOfertas", Map.of(
                "total", ofertaService.contarTotal(),
                "pendientes", ofertaService.contarPorEstado(EstadoOferta.PENDIENTE),
                "aceptadas", ofertaService.contarPorEstado(EstadoOferta.ACEPTADA),
                "rechazadas", ofertaService.contarPorEstado(EstadoOferta.RECHAZADA)
        ));
        model.addAttribute("passwordPolicy", PasswordValidator.getValidationMessage());
        return "admin/panel";
    }

    @PostMapping("/usuarios/{id}/actualizar-email")
    public String actualizarEmail(@PathVariable("id") Long id,
                                  @RequestParam("email") String email,
                                  RedirectAttributes ra) {
        try {
            Usuario usuario = usuarioService.actualizarEmail(id, email);
            ra.addFlashAttribute("success", "Se actualizó el email de " + usuario.getUsername());
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo actualizar el email del usuario {}", id, e);
            ra.addFlashAttribute("error", "No pudimos actualizar el email: " + e.getMessage());
        }
        return "redirect:/web/admin";
    }

    @PostMapping("/usuarios/{id}/restablecer-password")
    public String restablecerPassword(@PathVariable("id") Long id,
                                      @RequestParam("password") String password,
                                      RedirectAttributes ra) {
        try {
            usuarioService.restablecerPassword(id, password);
            ra.addFlashAttribute("success", "Se restableció la contraseña del usuario seleccionado");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo restablecer la contraseña del usuario {}", id, e);
            ra.addFlashAttribute("error", "No pudimos restablecer la contraseña: " + e.getMessage());
        }
        return "redirect:/web/admin";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = usuarioService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            if (auth != null && !(auth instanceof AnonymousAuthenticationToken)
                    && auth.getName() != null && auth.getName().equalsIgnoreCase(usuario.getUsername())) {
                ra.addFlashAttribute("error", "No podés eliminar tu propia cuenta desde el panel de administración");
                return "redirect:/web/admin";
            }
            usuarioService.eliminarUsuario(id);
            ra.addFlashAttribute("success", "Se eliminó la cuenta " + usuario.getUsername());
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo eliminar el usuario {}", id, e);
            ra.addFlashAttribute("error", "No pudimos eliminar la cuenta: " + e.getMessage());
        }
        return "redirect:/web/admin";
    }

    @PostMapping("/publicaciones/{id}/oficial")
    public String actualizarOficialidad(@PathVariable("id") Long id,
                                        @RequestParam("valor") boolean valor,
                                        RedirectAttributes ra) {
        try {
            publicacionService.actualizarOficialidad(id, valor, null, true);
            ra.addFlashAttribute("success", valor
                    ? "La publicación ahora figura como Swapify Oficial"
                    : "La publicación volvió al estado estándar de usuario");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo actualizar la oficialidad de la publicación {}", id, e);
            ra.addFlashAttribute("error", "No pudimos actualizar la publicación: " + e.getMessage());
        }
        return "redirect:/web/admin";
    }
}
