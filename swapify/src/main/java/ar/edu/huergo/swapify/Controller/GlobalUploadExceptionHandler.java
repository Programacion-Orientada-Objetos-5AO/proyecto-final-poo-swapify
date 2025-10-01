package ar.edu.huergo.swapify.controller;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * Maneja errores globales de subida de archivos para devolver mensajes claros
 * tanto a las vistas web como al API REST.
 */
@Slf4j
@ControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class GlobalUploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object manejarArchivoDemasiadoGrande(MaxUploadSizeExceededException ex,
                                                HttpServletRequest request,
                                                RedirectAttributes redirectAttributes) {
        SizeInfo sizeInfo = extraerTamanos(ex);
        String tamanioActual = formatearBytes(sizeInfo.actual);
        String limite = formatearBytes(sizeInfo.permitido);
        String mensaje = String.format(
                "La imagen que intentaste subir pesa %s y supera el límite permitido de %s. Reducila e intentá nuevamente.",
                tamanioActual, limite);

        String uri = request.getRequestURI();
        log.warn("Carga rechazada en {}: archivo {} mayor al límite {}", uri, tamanioActual, limite);

        if (uri != null && uri.startsWith("/web/")) {
            redirectAttributes.addFlashAttribute("error", mensaje);
            return "redirect:/web/publicaciones/nueva";
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        problem.setTitle("Archivo demasiado grande");
        problem.setDetail(mensaje);
        problem.setType(URI.create("https://http.dev/problems/file-too-large"));
        if (uri != null && !uri.isBlank()) {
            problem.setInstance(URI.create(uri));
        }
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    private SizeInfo extraerTamanos(MaxUploadSizeExceededException ex) {
        long permitido = ex.getMaxUploadSize();
        long actual = -1L;

        Throwable causa = ex.getCause();
        if (causa instanceof IllegalStateException ise && ise.getCause() instanceof SizeLimitExceededException sle) {
            actual = sle.getActualSize();
            permitido = sle.getPermittedSize();
        }

        return new SizeInfo(actual, permitido);
    }

    private String formatearBytes(long bytes) {
        if (bytes <= 0) {
            return "desconocido";
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(java.util.Locale.forLanguageTag("es-AR"), "%.1f MB", mb);
    }

    private record SizeInfo(long actual, long permitido) {
    }
}

