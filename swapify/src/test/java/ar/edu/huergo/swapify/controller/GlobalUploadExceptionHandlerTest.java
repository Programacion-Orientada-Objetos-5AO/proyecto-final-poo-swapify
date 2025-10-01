package ar.edu.huergo.swapify.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GlobalUploadExceptionHandlerTest {

    private final GlobalUploadExceptionHandler handler = new GlobalUploadExceptionHandler();

    @Test
    void manejarArchivoDemasiadoGrande_redirigeFormularioWeb() {
        SizeLimitExceededException sizeLimitExceeded = new SizeLimitExceededException(
                "Archivo demasiado grande", 22_020_096L, 20_971_520L);
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(20_971_520L,
                new IllegalStateException(sizeLimitExceeded));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/web/publicaciones");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object resultado = handler.manejarArchivoDemasiadoGrande(ex, request, redirectAttributes);

        assertThat(resultado).isEqualTo("redirect:/web/publicaciones/nueva");
        assertThat(redirectAttributes.getFlashAttributes())
                .containsKey("error");
        String mensaje = (String) redirectAttributes.getFlashAttributes().get("error");
        assertThat(mensaje)
                .contains("supera el límite permitido");
    }

    @Test
    void manejarArchivoDemasiadoGrande_devuelveProblemDetailParaApi() {
        SizeLimitExceededException sizeLimitExceeded = new SizeLimitExceededException(
                "Archivo demasiado grande", 22_020_096L, 20_971_520L);
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(20_971_520L,
                new IllegalStateException(sizeLimitExceeded));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/publicaciones");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object resultado = handler.manejarArchivoDemasiadoGrande(ex, request, redirectAttributes);

        assertThat(resultado).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) resultado;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getDetail()).contains("supera el límite permitido");
    }
}
