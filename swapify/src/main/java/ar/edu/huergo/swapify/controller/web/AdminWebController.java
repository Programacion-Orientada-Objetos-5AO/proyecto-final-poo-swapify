package ar.edu.huergo.swapify.controller.web;

import java.time.LocalDateTime;
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
        model.addAttribute("usuarios", List.of());
        model.addAttribute("publicaciones", List.of());
        model.addAttribute("publicacionesPorUsuario", Map.of());
        model.addAttribute("rolesPorUsuario", Map.of());
        model.addAttribute("passwordPolicy", PasswordValidator.getValidationMessage());
        model.addAttribute("resumenPublicaciones", Map.of(
                "total", 0L,
                "activas", 0L,
                "enNegociacion", 0L,
                "finalizadas", 0L,
                "oficiales", 0L
        ));
        model.addAttribute("resumenOfertas", Map.of(
                "total", 0L,
                "pendientes", 0L,
                "aceptadas", 0L,
                "rechazadas", 0L
        ));
        model.addAttribute("panelCargaError", false);

        try {
            List<Usuario> usuarios = usuarioService.getAllUsuarios().stream()
                    .sorted(Comparator.comparing(Usuario::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
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
                                .map(String::trim)
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
        } catch (Exception e) {
            log.error("No se pudo cargar el panel administrativo", e);
            model.addAttribute("panelCargaError", true);
        }
        return "admin/panel";
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

    @PostMapping("/usuarios/{id}/banear")
    public String banearUsuario(@PathVariable("id") Long id,
                                @RequestParam("dias") Integer dias,
                                @RequestParam(value = "motivo", required = false) String motivo,
                                RedirectAttributes ra) {
        try {
            if (dias == null || dias <= 0) {
                ra.addFlashAttribute("error", "Indicá la cantidad de días de suspensión");
                return "redirect:/web/admin";
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = usuarioService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            if (auth != null && !(auth instanceof AnonymousAuthenticationToken)
                    && auth.getName() != null && auth.getName().equalsIgnoreCase(usuario.getUsername())) {
                ra.addFlashAttribute("error", "No podés suspender tu propia cuenta");
                return "redirect:/web/admin";
            }
            LocalDateTime hasta = LocalDateTime.now().plusDays(dias.longValue());
            usuarioService.banearUsuario(id, hasta, motivo);
            ra.addFlashAttribute("success", "La cuenta quedará suspendida hasta " + hasta.toLocalDate());
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo banear al usuario {}", id, e);
            ra.addFlashAttribute("error", "No pudimos suspender la cuenta: " + e.getMessage());
        }
        return "redirect:/web/admin";
    }

    @PostMapping("/usuarios/{id}/levantar-ban")
    public String levantarBan(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            usuarioService.levantarBan(id);
            ra.addFlashAttribute("success", "Se rehabilitó la cuenta seleccionada");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo rehabilitar al usuario {}", id, e);
            ra.addFlashAttribute("error", "No pudimos rehabilitar la cuenta: " + e.getMessage());
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
