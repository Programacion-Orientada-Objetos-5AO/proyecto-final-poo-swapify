package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CrearOfertaDTO {

    @NotBlank(message = "Contanos qué ofrecés")
    @Size(max = 2000, message = "La oferta es demasiado extensa")
    private String mensaje;

    @NotBlank(message = "Indicá el nombre del artículo que ofrecés")
    @Size(max = 120, message = "El nombre del artículo es demasiado largo")
    private String nombreArticulo;

    @PositiveOrZero(message = "El precio referencial no puede ser negativo")
    private BigDecimal precioArticulo;

    @NotBlank(message = "Describí el artículo que ofrecés")
    @Size(max = 2000, message = "La descripción del artículo es demasiado extensa")
    private String descripcionArticulo;

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
