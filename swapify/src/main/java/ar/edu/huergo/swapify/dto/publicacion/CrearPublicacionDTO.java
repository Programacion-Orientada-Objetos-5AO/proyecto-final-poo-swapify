package ar.edu.huergo.swapify.dto.publicacion;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CrearPublicacionDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    // Puede ser null si es solo referencial
    private BigDecimal precio;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotBlank(message = "Debe indicar el objeto a cambiar")
    private String objetoACambiar;

    private String imagenBase64;

    private String imagenContentType;

    @JsonIgnore
    private transient MultipartFile imagenArchivo;
}
