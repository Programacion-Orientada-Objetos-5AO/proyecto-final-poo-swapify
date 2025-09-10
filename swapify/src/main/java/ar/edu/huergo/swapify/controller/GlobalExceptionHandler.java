package ar.edu.huergo.swapify.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j // Lombok: inyecta un logger SLF4J llamado 'log'
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        // Log de advertencia con detalle de campos inválidos
        log.warn("Solicitud inválida: errores de validación {}", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Violación de constraint");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/constraint-violation"));
        // Log de advertencia con el detalle de la violación
        log.warn("Violación de constraint: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso no encontrado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/not-found"));
        // Log informativo: recurso solicitado no existe
        log.info("Recurso no encontrado: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Error interno del servidor");
        problem.setDetail("Ha ocurrido un error inesperado");
        problem.setType(URI.create("https://http.dev/problems/internal-error"));
        // Log de error con stacktrace para diagnóstico
        log.error("Error no controlado", ex);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Argumento inválido");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://http.dev/problems/invalid-argument"));
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Credenciales inválidas");
        problem.setDetail("Las credenciales proporcionadas son incorrectas");
        problem.setType(URI.create("https://http.dev/problems/unauthorized"));
        // Log de advertencia: credenciales incorrectas
        log.warn("Intento de acceso con credenciales inválidas: {}", ex.getMessage());
        return problem;
    }

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

    
}


