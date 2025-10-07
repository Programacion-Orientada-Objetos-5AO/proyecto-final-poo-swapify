package ar.edu.huergo.swapify.controller;

import ar.edu.huergo.swapify.service.publicacion.OfertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones/{publicacionId}/ofertas")
@RequiredArgsConstructor
public class OfertaController {

    private final OfertaService ofertaService;

    @PostMapping("/{ofertaId}/aceptar")
    public ResponseEntity<Void> aceptarOferta(@PathVariable Long publicacionId,
                                              @PathVariable Long ofertaId,
                                              @AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new AccessDeniedException("Debés iniciar sesión para gestionar ofertas");
        }
        ofertaService.aceptarOferta(publicacionId, ofertaId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ofertaId}/rechazar")
    public ResponseEntity<Void> rechazarOferta(@PathVariable Long publicacionId,
                                               @PathVariable Long ofertaId,
                                               @AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new AccessDeniedException("Debés iniciar sesión para gestionar ofertas");
        }
        ofertaService.rechazarOferta(publicacionId, ofertaId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
