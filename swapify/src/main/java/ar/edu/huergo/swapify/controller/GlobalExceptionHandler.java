package ar.edu.huergo.swapify.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Manejador global de excepciones de la API.
 *
 * Beneficios:
 * - Centraliza el manejo de errores: evita try/catch repetidos en controladores.
 * - Respuestas consistentes: devuelve Problem Details (RFC 7807) con estructura uniforme.
 * - Observabilidad: registra logs claros por tipo de error para facilitar el troubleshooting.
 *
 * Qué es:
 * - {@link RestControllerAdvice}: intercepta excepciones lanzadas por controladores REST
 *   y transforma los errores en respuestas HTTP estandarizadas.
 * - Usa {@link ProblemDetail} para describir el problema con status, title, detail y propiedades extra.
 * - Usa SLF4J vía Lombok (@Slf4j) para emitir logs con el nivel adecuado.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final long MAX_JSON_STRING_LENGTH = 20_000_000L;
    private static final Pattern LENGTH_PATTERN = Pattern.compile("length \\((\\d+)\\)");

    /**
     * Genera una respuesta estandarizada para errores de validación en payloads
     * con {@code @Valid}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validación fallida");
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setDetail("Se encontraron errores de validación en el payload");
        problem.setProperty("errores", errors);
        problem.setType(URI.create("https://http.dev/problems/validation-error"));
        log.warn("Solicitud inválida: errores de validación {}", errors);
        return problem;
    }

    /**
     * Atiende violaciones de constraints declarativas devolviendo Problem
     * Details con información de la regla incumplida.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Violación de constraint");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/constraint-violation"));
        log.warn("Violación de constraint: {}", ex.getMessage());
        return problem;
    }

    /**
     * Responde ante cuerpos de solicitud ilegibles diferenciando los casos de
     * imágenes demasiado grandes.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException mappingEx) {
            Throwable root = mappingEx.getCause();
            if (root instanceof StreamConstraintsException constraintsEx) {
                long actual = extraerLongitud(constraintsEx.getMessage());
                log.warn("Payload Base64 rechazado: longitud {} mayor al límite {}", actual, MAX_JSON_STRING_LENGTH);

                ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
                problem.setTitle("Imagen demasiado grande");
                problem.setDetail("La imagen codificada supera el máximo permitido por la API (20 MB). Reducila e intentá nuevamente.");
                problem.setType(URI.create("https://http.dev/problems/file-too-large"));
                if (actual > 0) {
                    problem.setProperty("tamanioActual", actual);
                }
                problem.setProperty("limite", MAX_JSON_STRING_LENGTH);
                return problem;
            }
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Cuerpo de la solicitud inválido");
        problem.setDetail("No se pudo interpretar la solicitud enviada");
        problem.setType(URI.create("https://http.dev/problems/unreadable-body"));
        log.warn("No se pudo leer el cuerpo de la solicitud: {}", ex.getMessage());
        return problem;
    }

    /**
     * Devuelve un Problem Details 404 cuando el recurso solicitado no existe.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso no encontrado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/not-found"));
        log.info("Recurso no encontrado: {}", ex.getMessage());
        return problem;
    }

    /**
     * Maneja cualquier excepción no prevista para evitar exponer detalles del
     * servidor.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Error interno del servidor");
        problem.setDetail("Ha ocurrido un error inesperado");
        problem.setType(URI.create("https://http.dev/problems/internal-error"));
        log.error("Error no controlado", ex);
        return problem;
    }

    /**
     * Responde con un 400 cuando se detectan argumentos inválidos en la lógica
     * de negocio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Argumento inválido");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/invalid-argument"));
        return problem;
    }

    /**
     * Devuelve un 401 cuando las credenciales proporcionadas son incorrectas.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Credenciales inválidas");
        problem.setDetail("Las credenciales proporcionadas son incorrectas");
        problem.setType(URI.create("https://http.dev/problems/unauthorized"));
        log.warn("Intento de acceso con credenciales inválidas: {}", ex.getMessage());
        return problem;
    }

    /**
     * Envía un 403 cuando la persona autenticada intenta acceder a recursos sin
     * permisos suficientes.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Acceso denegado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/forbidden"));
        log.warn("Acceso denegado: {}", ex.getMessage());
        return problem;
    }

    /**
     * Informa sobre parámetros faltantes en solicitudes HTTP.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingRequestParam(MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Parámetro requerido faltante");
        problem.setDetail("Falta un parámetro requerido en la solicitud");
        problem.setType(URI.create("https://http.dev/problems/missing-parameter"));

        Map<String, String> faltantes = new HashMap<>();
        faltantes.put(ex.getParameterName(), ex.getParameterType());
        problem.setProperty("faltantes", faltantes);

        log.warn("Parámetro requerido faltante: nombre='{}', tipo='{}'", ex.getParameterName(), ex.getParameterType());
        return problem;
    }

    /**
     * Obtiene la longitud reportada en el mensaje de error del parser JSON.
     *
     * @param mensaje detalle textual de la excepción.
     * @return longitud extraída o {@code -1} si no se pudo determinar.
     */
    private long extraerLongitud(String mensaje) {
        if (mensaje == null) {
            return -1L;
        }
        Matcher matcher = LENGTH_PATTERN.matcher(mensaje);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1L;
    }
}


