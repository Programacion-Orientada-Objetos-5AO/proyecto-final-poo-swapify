package ar.edu.huergo.swapify.dto.publicacion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CrearOfertaDTO {

    @NotBlank(message = "Contanos qué ofrecés")
    @Size(max = 2000, message = "La oferta es demasiado extensa")
    private String mensaje;

    @Size(max = 255, message = "El nombre del objeto es muy largo")
    private String propuestaObjeto;

    private String imagenBase64;

    private String imagenContentType;

    @JsonIgnore
    private transient MultipartFile imagenArchivo;

    @JsonIgnore
    public boolean tieneImagen() {
        boolean archivo = imagenArchivo != null && !imagenArchivo.isEmpty();
        boolean base64 = imagenBase64 != null && !imagenBase64.isBlank();
        return archivo || base64;
    }
}
