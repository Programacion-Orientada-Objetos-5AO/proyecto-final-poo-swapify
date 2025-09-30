package ar.edu.huergo.swapify.dto.publicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearOfertaDTO {

    @NotBlank(message = "Contanos qué ofrecés")
    @Size(max = 2000, message = "La oferta es demasiado extensa")
    private String mensaje;

    @Size(max = 255, message = "El nombre del objeto es muy largo")
    private String propuestaObjeto;
}
