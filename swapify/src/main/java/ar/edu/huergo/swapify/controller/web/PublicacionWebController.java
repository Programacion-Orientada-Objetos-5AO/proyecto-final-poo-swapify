package ar.edu.huergo.swapify.controller.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Controlador web (Thymeleaf) para vistas de publicaciones
 * Sirve páginas HTML (no JSON)
 */
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
public class PublicacionWebController {

    private final PublicacionService publicacionService;

    /** Home → lista de publicaciones */
    @GetMapping({"", "/"})
    public String home(Model model) {
        List<Publicacion> publicaciones = publicacionService.listarTodas();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        return "publicaciones/lista";
    }

    /** Lista */
    @GetMapping("/publicaciones")
    public String listar(Model model) {
        List<Publicacion> publicaciones = publicacionService.listarTodas();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        return "publicaciones/lista";
    }

    /** Detalle de una publicación */
    @GetMapping("/publicaciones/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Publicacion p = publicacionService.obtenerPorId(id);
            model.addAttribute("publicacion", p);
            model.addAttribute("titulo", "Detalle de la publicación");
            return "publicaciones/detalle";
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", "Publicación no encontrada");
            return "redirect:/web/publicaciones";
        }
    }

    /** Formulario de alta */
    @GetMapping("/publicaciones/nueva")
    public String formNueva(Model model) {
        model.addAttribute("publicacion", new CrearPublicacionDTO());
        model.addAttribute("titulo", "Crear publicación");
        return "publicaciones/formulario";
    }

    /** Procesar alta usando el usuario autenticado */
    @PostMapping("/publicaciones")
    public String crear(@Valid @ModelAttribute("publicacion") CrearPublicacionDTO dto,
                        BindingResult result,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Revisá los datos del formulario");
            return "redirect:/web/publicaciones/nueva";
        }

        try {
            // Tomamos el username del contexto de seguridad
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : null;

            // Creamos un Usuario “shell” con solo el username:
            // el Service se encarga de buscar el managed user y setearlo en la entidad.
            Usuario u = new Usuario();
            u.setUsername(username);

            publicacionService.crearPublicacion(dto, u);
            ra.addFlashAttribute("success", "Publicación creada correctamente");
            return "redirect:/web/publicaciones";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al crear: " + e.getMessage());
            return "redirect:/web/publicaciones/nueva";
        }
    }

    /** Página simple “Acerca” (opcional) */
    @GetMapping("/acerca")
    public String acerca(Model model) {
        model.addAttribute("titulo", "Acerca de Swapify");
        return "acerca";
    }
}
