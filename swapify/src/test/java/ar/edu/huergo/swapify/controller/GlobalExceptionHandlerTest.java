package ar.edu.huergo.swapify.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonMappingException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleHttpMessageNotReadable_detectaBase64DemasiadoGrande() {
        StreamConstraintsException constraints = new StreamConstraintsException(
                "String value length (20051112) exceeds the maximum allowed (20000000, from `StreamReadConstraints.getMaxStringLength()`)");
        JsonMappingException mappingException = new JsonMappingException("json demasiado grande", (JsonLocation) null, constraints);
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error", mappingException, null);

        ProblemDetail problem = handler.handleHttpMessageNotReadable(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getTitle()).isEqualTo("Imagen demasiado grande");
        assertThat(problem.getDetail()).contains("supera el máximo permitido");
        assertThat(problem.getProperties()).containsEntry("tamanioActual", 20_051_112L);
        assertThat(problem.getProperties()).containsEntry("limite", 20_000_000L);
    }

    @Test
    void handleHttpMessageNotReadable_paraErroresGenericosDevuelveBadRequest() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("JSON inválido");

        ProblemDetail problem = handler.handleHttpMessageNotReadable(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Cuerpo de la solicitud inválido");
    }
}
