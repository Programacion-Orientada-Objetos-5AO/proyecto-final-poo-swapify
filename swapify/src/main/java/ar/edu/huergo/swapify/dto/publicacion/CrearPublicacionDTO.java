package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    private List<String> imagenesBase64 = new ArrayList<>();

    private List<String> imagenesContentType = new ArrayList<>();

    @JsonIgnore
    private transient List<MultipartFile> imagenesArchivos = new ArrayList<>();

    /**
     * Valida que la carga incluya al menos una imagen ya sea como archivo o en
     * formato Base64.
     *
     * @return {@code true} cuando existe algún tipo de representación de imagen.
     */
    @JsonIgnore
    @AssertTrue(message = "Debés adjuntar al menos una imagen")
    public boolean isImagenPresente() {
        boolean hayArchivos = imagenesArchivos != null && imagenesArchivos.stream().anyMatch(f -> f != null && !f.isEmpty());
        boolean hayBase64 = imagenesBase64 != null && imagenesBase64.stream().anyMatch(cadena -> cadena != null && !cadena.isBlank());
        return hayArchivos || hayBase64;
    }
}
