package ar.edu.huergo.swapify.controller.web;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.huergo.swapify.dto.publicacion.CrearOfertaDTO;
import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import ar.edu.huergo.swapify.service.publicacion.OfertaService;
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
@Slf4j
public class PublicacionWebController {

    private final PublicacionService publicacionService;
    private final OfertaService ofertaService;

    /** Home → lista de publicaciones */
    @GetMapping({"", "/"})
    public String home(Model model) {
        List<Publicacion> publicaciones = publicacionService.listarTodas();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        model.addAttribute("misPublicaciones", false);
        model.addAttribute("requiereLogin", false);
        return "publicaciones/lista";
    }

    /** Lista */
    @GetMapping("/publicaciones")
    public String listar(Model model) {
        List<Publicacion> publicaciones = publicacionService.listarTodas();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        model.addAttribute("misPublicaciones", false);
        model.addAttribute("requiereLogin", false);
        return "publicaciones/lista";
    }

    @GetMapping("/publicaciones/mias")
    public String listarPropias(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            model.addAttribute("publicaciones", List.of());
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", true);
            return "publicaciones/lista";
        }

        try {
            List<Publicacion> publicaciones = publicacionService.listarPorUsuario(auth.getName());
            model.addAttribute("publicaciones", publicaciones);
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", false);
        } catch (Exception e) {
            log.error("Error al listar las publicaciones del usuario {}", auth.getName(), e);
            model.addAttribute("publicaciones", List.of());
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", false);
            model.addAttribute("error", "No pudimos cargar tus publicaciones en este momento. Intentá nuevamente más tarde.");
        }
        return "publicaciones/lista";
    }

    /** Detalle de una publicación */
    @GetMapping("/publicaciones/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Publicacion p = publicacionService.obtenerPorId(id);
            model.addAttribute("publicacion", p);
            model.addAttribute("ofertas", ofertaService.listarPorPublicacion(id));
            if (!model.containsAttribute("nuevaOferta")) {
                model.addAttribute("nuevaOferta", new CrearOfertaDTO());
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esPropietario = auth != null && !(auth instanceof AnonymousAuthenticationToken)
                    && auth.isAuthenticated() && p.getUsuario() != null
                    && auth.getName().equals(p.getUsuario().getUsername());
            model.addAttribute("esPropietario", esPropietario);
            model.addAttribute("estadoAceptada", EstadoOferta.ACEPTADA);
            model.addAttribute("estadoPendiente", EstadoOferta.PENDIENTE);
            model.addAttribute("estadoRechazada", EstadoOferta.RECHAZADA);
            model.addAttribute("titulo", "Detalle de la publicación");
            return "publicaciones/detalle";
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", "Publicación no encontrada");
            return "redirect:/web/publicaciones";
        }
    }

    @GetMapping("/publicaciones/{id}/ofertas")
    public String administrarOfertas(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + id;
        }

        try {
            Publicacion publicacion = publicacionService.obtenerPorId(id);
            if (publicacion.getUsuario() == null || publicacion.getUsuario().getUsername() == null
                    || !auth.getName().equals(publicacion.getUsuario().getUsername())) {
                ra.addFlashAttribute("error", "No tenés permiso para gestionar las ofertas de esta publicación");
                return "redirect:/web/publicaciones/" + id;
            }

            List<Oferta> ofertas = ofertaService.listarPorPublicacion(id);
            model.addAttribute("publicacion", publicacion);
            model.addAttribute("ofertas", ofertas);
            model.addAttribute("estadoAceptada", EstadoOferta.ACEPTADA);
            model.addAttribute("estadoPendiente", EstadoOferta.PENDIENTE);
            model.addAttribute("estadoRechazada", EstadoOferta.RECHAZADA);
            model.addAttribute("resumenOfertas", Map.of(
                    "total", Long.valueOf(ofertas.size()),
                    "pendientes", ofertas.stream().filter(Oferta::estaPendiente).count(),
                    "aceptadas", ofertas.stream().filter(Oferta::estaAceptada).count(),
                    "rechazadas", ofertas.stream().filter(Oferta::estaRechazada).count()
            ));
            model.addAttribute("titulo", "Ofertas de la publicación");
            model.addAttribute("esPropietario", true);
            return "publicaciones/ofertas";
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", "Publicación no encontrada");
            return "redirect:/web/publicaciones";
        }
    }

    /** Formulario de alta */
    @GetMapping("/publicaciones/nueva")
    public String formNueva(Model model) {
        if (!model.containsAttribute("publicacion")) {
            model.addAttribute("publicacion", new CrearPublicacionDTO());
        }
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
            ra.addFlashAttribute("publicacion", dto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.publicacion", result);
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
            return "redirect:/web/publicaciones/mias";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al crear: " + e.getMessage());
            return "redirect:/web/publicaciones/nueva";
        }
    }

    @PostMapping("/publicaciones/{id}/ofertas")
    public String ofertar(@PathVariable Long id,
                          @Valid @ModelAttribute("nuevaOferta") CrearOfertaDTO dto,
                          BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("errorOferta", "Revisá tu propuesta antes de enviarla");
            ra.addFlashAttribute("nuevaOferta", dto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.nuevaOferta", result);
            return "redirect:/web/publicaciones/" + id;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("errorOferta", "Necesitás iniciar sesión para ofertar");
            return "redirect:/web/publicaciones/" + id;
        }

        try {
            ofertaService.crearOferta(id, dto, auth.getName());
            ra.addFlashAttribute("successOferta", "¡Tu oferta fue enviada al propietario!");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorOferta", "No pudimos registrar tu oferta: " + e.getMessage());
        }
        return "redirect:/web/publicaciones/" + id;
    }

    @PostMapping("/publicaciones/{publicacionId}/ofertas/{ofertaId}/aceptar")
    public String aceptarOferta(@PathVariable Long publicacionId,
                                @PathVariable Long ofertaId,
                                @RequestParam(value = "redirect", required = false) String redirect,
                                RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("errorOferta", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + publicacionId;
        }

        try {
            ofertaService.aceptarOferta(publicacionId, ofertaId, auth.getName());
            ra.addFlashAttribute("successOferta", "¡Aceptaste la oferta seleccionada! Nos comunicaremos para coordinar el intercambio.");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorOferta", "No pudimos actualizar la oferta: " + e.getMessage());
        }
        return redireccionarGestionOfertas(publicacionId, redirect);
    }

    @PostMapping("/publicaciones/{publicacionId}/ofertas/{ofertaId}/rechazar")
    public String rechazarOferta(@PathVariable Long publicacionId,
                                 @PathVariable Long ofertaId,
                                 @RequestParam(value = "redirect", required = false) String redirect,
                                 RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("errorOferta", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + publicacionId;
        }

        try {
            ofertaService.rechazarOferta(publicacionId, ofertaId, auth.getName());
            ra.addFlashAttribute("successOferta", "La oferta fue rechazada correctamente.");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorOferta", "No pudimos actualizar la oferta: " + e.getMessage());
        }
        return redireccionarGestionOfertas(publicacionId, redirect);
    }

    private String redireccionarGestionOfertas(Long publicacionId, String redirect) {
        if (redirect != null && redirect.equalsIgnoreCase("panel")) {
            return "redirect:/web/publicaciones/" + publicacionId + "/ofertas";
        }
        return "redirect:/web/publicaciones/" + publicacionId;
    }

    @PostMapping("/publicaciones/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión para eliminar una publicación");
            return "redirect:/web/publicaciones";
        }

        try {
            publicacionService.eliminarPublicacion(id, auth.getName());
            ra.addFlashAttribute("success", "La publicación se eliminó correctamente");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No pudimos eliminar la publicación: " + e.getMessage());
        }
        return "redirect:/web/publicaciones/mias";
    }

    /** Página simple “Acerca” (opcional) */
    @GetMapping("/acerca")
    public String acerca(Model model) {
        model.addAttribute("titulo", "Acerca de Swapify");
        return "acerca";
    }
}
