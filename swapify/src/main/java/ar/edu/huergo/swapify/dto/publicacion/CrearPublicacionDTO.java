package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO utilizado para recibir los datos de creación de una publicación desde
 * formularios web o peticiones API.
 */
@Data
public class CrearPublicacionDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Precio referencial del artículo. Puede omitirse cuando el intercambio no
     * involucra dinero.
     */
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotBlank(message = "Debe indicar el objeto a cambiar")
    private String objetoACambiar;

    private String imagenBase64;

    private String imagenContentType;

    @JsonIgnore
    private transient MultipartFile imagenArchivo;

    /**
     * Valida que la carga incluya una imagen ya sea como archivo o en formato
     * Base64.
     *
     * @return {@code true} cuando existe algún tipo de representación de imagen.
     */
    @JsonIgnore
    @AssertTrue(message = "La imagen es obligatoria")
    public boolean isImagenPresente() {
        boolean tieneArchivo = imagenArchivo != null && !imagenArchivo.isEmpty();
        boolean tieneBase64 = imagenBase64 != null && !imagenBase64.isBlank();
        return tieneArchivo || tieneBase64;
    }
}
