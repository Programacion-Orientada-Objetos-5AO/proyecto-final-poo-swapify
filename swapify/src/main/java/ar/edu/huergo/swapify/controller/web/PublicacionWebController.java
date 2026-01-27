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
import ar.edu.huergo.swapify.entity.publicacion.EstadoPublicacion;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import ar.edu.huergo.swapify.service.publicacion.OfertaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Controlador web responsable de renderizar y gestionar las vistas de
 * publicaciones de la plataforma.
 */
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
@Slf4j
public class PublicacionWebController {

    private final PublicacionService publicacionService;
    private final OfertaService ofertaService;

    /**
     * Muestra la página principal con el listado público de publicaciones.
     */
    @GetMapping({"", "/"})
    public String home(Model model) {
        List<Publicacion> publicaciones = publicacionService.listarDisponibles();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        model.addAttribute("misPublicaciones", false);
        model.addAttribute("requiereLogin", false);
        model.addAttribute("consulta", null);
        return "publicaciones/lista";
    }

    /**
     * Lista todas las publicaciones disponibles de manera equivalente a la
     * página de inicio.
     */
    @GetMapping("/publicaciones")
    public String listar(@RequestParam(value = "q", required = false) String consulta, Model model) {
        List<Publicacion> publicaciones = (consulta != null && !consulta.isBlank())
                ? publicacionService.buscarDisponibles(consulta)
                : publicacionService.listarDisponibles();
        model.addAttribute("publicaciones", publicaciones);
        model.addAttribute("titulo", "Publicaciones");
        model.addAttribute("misPublicaciones", false);
        model.addAttribute("requiereLogin", false);
        model.addAttribute("consulta", consulta);
        return "publicaciones/lista";
    }

    /**
     * Lista únicamente las publicaciones pertenecientes al usuario autenticado.
     */
    @GetMapping("/publicaciones/mias")
    public String listarPropias(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            model.addAttribute("publicaciones", List.of());
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", true);
            model.addAttribute("consulta", null);
            return "publicaciones/lista";
        }

        try {
            List<Publicacion> publicaciones = publicacionService.listarPorUsuario(auth.getName());
            model.addAttribute("publicaciones", publicaciones);
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", false);
            model.addAttribute("consulta", null);
        } catch (Exception e) {
            log.error("Error al listar las publicaciones del usuario {}", auth.getName(), e);
            model.addAttribute("publicaciones", List.of());
            model.addAttribute("titulo", "Mis publicaciones");
            model.addAttribute("misPublicaciones", true);
            model.addAttribute("requiereLogin", false);
            model.addAttribute("error", "No pudimos cargar tus publicaciones en este momento. Intentá nuevamente más tarde.");
            model.addAttribute("consulta", null);
        }
        return "publicaciones/lista";
    }

    /**
     * Visualiza el detalle de una publicación específica junto con sus ofertas
     * y acciones disponibles según el usuario autenticado.
     */
    @GetMapping("/publicaciones/{id}")
    public String ver(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        try {
            Publicacion p = publicacionService.obtenerPorId(id);
            model.addAttribute("publicacion", p);
            List<Oferta> ofertas = ofertaService.listarPorPublicacion(id);
            model.addAttribute("ofertas", ofertas);
            if (!model.containsAttribute("nuevaOferta")) {
                model.addAttribute("nuevaOferta", new CrearOfertaDTO());
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean autenticado = auth != null && !(auth instanceof AnonymousAuthenticationToken)
                    && auth.isAuthenticated();
            boolean esAdmin = autenticado && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean esPropietario = autenticado && p.getUsuario() != null
                    && auth.getName().equals(p.getUsuario().getUsername());
            var ofertaAceptada = ofertaService.obtenerOfertaAceptada(id);
            boolean esPostulanteAceptado = autenticado && ofertaAceptada.isPresent()
                    && ofertaAceptada.get().getUsuario() != null
                    && auth.getName().equals(ofertaAceptada.get().getUsuario().getUsername());
            boolean puedeCoordinar = esPropietario || esPostulanteAceptado || esAdmin;
            boolean puedeOfertar = p.estaActiva() && !esPropietario && !esAdmin;
            model.addAttribute("esPropietario", esPropietario);
            model.addAttribute("esAdmin", esAdmin);
            model.addAttribute("ofertaAceptada", ofertaAceptada.orElse(null));
            model.addAttribute("esPostulanteAceptado", esPostulanteAceptado);
            model.addAttribute("puedeCoordinar", puedeCoordinar);
            model.addAttribute("puedeOfertar", puedeOfertar);
            model.addAttribute("estadoPublicacion", p.getEstado());
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

    /**
     * Panel para que la persona propietaria gestione las ofertas recibidas.
     */
    @GetMapping("/publicaciones/{id}/ofertas")
    public String administrarOfertas(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + id;
        }

        try {
            Publicacion publicacion = publicacionService.obtenerPorId(id);
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean esPropietario = publicacion.getUsuario() != null
                    && publicacion.getUsuario().getUsername() != null
                    && auth.getName().equals(publicacion.getUsuario().getUsername());
            if (!esPropietario && !esAdmin) {
                ra.addFlashAttribute("error", "No tenés permiso para gestionar las ofertas de esta publicación");
                return "redirect:/web/publicaciones/" + id;
            }

            List<Oferta> ofertas = ofertaService.listarPorPublicacion(id);
            model.addAttribute("publicacion", publicacion);
            model.addAttribute("ofertas", ofertas);
            model.addAttribute("estadoAceptada", EstadoOferta.ACEPTADA);
            model.addAttribute("estadoPendiente", EstadoOferta.PENDIENTE);
            model.addAttribute("estadoRechazada", EstadoOferta.RECHAZADA);
            model.addAttribute("estadoPublicacion", publicacion.getEstado());
            model.addAttribute("fechaReserva", publicacion.getFechaReserva());
            model.addAttribute("resumenOfertas", Map.of(
                    "total", Long.valueOf(ofertas.size()),
                    "pendientes", ofertas.stream().filter(Oferta::estaPendiente).count(),
                    "aceptadas", ofertas.stream().filter(Oferta::estaAceptada).count(),
                    "rechazadas", ofertas.stream().filter(Oferta::estaRechazada).count()
            ));
            model.addAttribute("titulo", "Ofertas de la publicación");
            model.addAttribute("esPropietario", esPropietario);
            model.addAttribute("esAdmin", esAdmin);
            return "publicaciones/ofertas";
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", "Publicación no encontrada");
            return "redirect:/web/publicaciones";
        }
    }

    /**
     * Muestra el formulario para crear una nueva publicación.
     */
    @GetMapping("/publicaciones/nueva")
    public String formNueva(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && !(auth instanceof AnonymousAuthenticationToken)
                && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!model.containsAttribute("publicacion")) {
            model.addAttribute("publicacion", new CrearPublicacionDTO());
        }
        model.addAttribute("titulo", "Crear publicación");
        model.addAttribute("esAdmin", esAdmin);
        return "publicaciones/formulario";
    }

    /**
     * Procesa el envío del formulario de creación utilizando el usuario
     * autenticado como propietario.
     */
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : null;

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

    /**
     * Registra una nueva oferta para la publicación indicada.
     */
    @PostMapping("/publicaciones/{id}/ofertas")
    public String ofertar(@PathVariable("id") Long id,
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
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorOferta", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorOferta", "No pudimos registrar tu oferta: " + e.getMessage());
        }
        return "redirect:/web/publicaciones/" + id;
    }

    /**
     * Marca una oferta como aceptada y opcionalmente redirige al panel de
     * gestión.
     */
    @PostMapping("/publicaciones/{publicacionId}/ofertas/{ofertaId}/aceptar")
    public String aceptarOferta(@PathVariable("publicacionId") Long publicacionId,
                                @PathVariable("ofertaId") Long ofertaId,
                                @RequestParam(value = "redirect", required = false) String redirect,
                                RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("errorOferta", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + publicacionId;
        }

        try {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            ofertaService.aceptarOferta(publicacionId, ofertaId, auth.getName(), esAdmin);
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

    /**
     * Marca una oferta como rechazada y devuelve la redirección apropiada.
     */
    @PostMapping("/publicaciones/{publicacionId}/ofertas/{ofertaId}/rechazar")
    public String rechazarOferta(@PathVariable("publicacionId") Long publicacionId,
                                 @PathVariable("ofertaId") Long ofertaId,
                                 @RequestParam(value = "redirect", required = false) String redirect,
                                 RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("errorOferta", "Necesitás iniciar sesión para gestionar las ofertas");
            return "redirect:/web/publicaciones/" + publicacionId;
        }

        try {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            ofertaService.rechazarOferta(publicacionId, ofertaId, auth.getName(), esAdmin);
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

    /**
     * Determina la URL de retorno luego de gestionar una oferta.
     *
     * @param publicacionId identificador de la publicación.
     * @param redirect parámetro opcional que define un destino alternativo.
     * @return cadena con la ruta de redirección.
     */
    private String redireccionarGestionOfertas(Long publicacionId, String redirect) {
        if (redirect != null && redirect.equalsIgnoreCase("panel")) {
            return "redirect:/web/publicaciones/" + publicacionId + "/ofertas";
        }
        return "redirect:/web/publicaciones/" + publicacionId;
    }

    @PostMapping("/publicaciones/{id}/estado")
    public String actualizarEstado(@PathVariable("id") Long id,
                                   @RequestParam("estado") EstadoPublicacion estado,
                                   @RequestParam(value = "redirect", required = false) String redirect,
                                   RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión para actualizar la publicación");
            return "redirect:/web/publicaciones/" + id;
        }

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        try {
            publicacionService.actualizarEstado(id, estado, auth.getName(), esAdmin);
            String mensaje = switch (estado) {
                case ACTIVA -> "La publicación se volvió a activar";
                case EN_NEGOCIACION -> "Marcaste la publicación como reservada para coordinar el intercambio";
                case PAUSADA -> "La publicación quedó pausada temporalmente";
                case FINALIZADA -> "¡Felicitaciones! Registramos el intercambio como finalizado";
            };
            ra.addFlashAttribute("success", mensaje);
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No pudimos actualizar el estado: " + e.getMessage());
        }

        if (redirect != null && redirect.equalsIgnoreCase("panel")) {
            return "redirect:/web/publicaciones/" + id + "/ofertas";
        }
        if (redirect != null && redirect.equalsIgnoreCase("admin")) {
            return "redirect:/web/admin";
        }
        return "redirect:/web/publicaciones/" + id;
    }

    /**
     * Elimina una publicación del usuario autenticado.
     */
    @PostMapping("/publicaciones/{id}/eliminar")
    public String eliminar(@PathVariable("id") Long id,
                           @RequestParam(value = "redirect", required = false) String redirect,
                           RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Necesitás iniciar sesión para eliminar una publicación");
            return "redirect:/web/publicaciones";
        }

        try {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            publicacionService.eliminarPublicacion(id, auth.getName(), esAdmin);
            ra.addFlashAttribute("success", "La publicación se eliminó correctamente");
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No pudimos eliminar la publicación: " + e.getMessage());
        }
        if (redirect != null) {
            String destino = redirect.trim().toLowerCase(java.util.Locale.ROOT);
            return switch (destino) {
                case "admin" -> "redirect:/web/admin";
                case "catalogo", "detalle" -> "redirect:/web/publicaciones";
                case "mis" -> "redirect:/web/publicaciones/mias";
                default -> "redirect:/web/publicaciones/mias";
            };
        }
        return "redirect:/web/publicaciones/mias";
    }

    /**
     * Muestra la página informativa de la aplicación.
     */
    @GetMapping("/acerca")
    public String acerca(Model model) {
        model.addAttribute("titulo", "Acerca de Swapify");
        return "acerca";
    }
}
