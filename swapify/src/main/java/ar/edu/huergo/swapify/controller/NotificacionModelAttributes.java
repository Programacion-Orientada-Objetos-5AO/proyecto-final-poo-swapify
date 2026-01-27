package ar.edu.huergo.swapify.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.huergo.swapify.service.security.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class NotificacionModelAttributes {

    private final NotificacionService notificacionService;

    @ModelAttribute("notificacionesSinLeer")
    public Long notificacionesSinLeer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return 0L;
        }
        try {
            return notificacionService.contarNoLeidas(auth.getName());
        } catch (Exception e) {
            log.warn("No se pudieron obtener las notificaciones sin leer para {}", auth.getName(), e);
            return 0L;
        }
    }
}
