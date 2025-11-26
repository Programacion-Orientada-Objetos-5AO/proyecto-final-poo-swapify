package ar.edu.huergo.swapify.entity.producto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Producto")
@Data
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nombre obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "Categoria obligatoria")
    @Size(min = 2, max = 100, message = "La categoria debe tener entre 2 y 100 caracteres")
    private String categoria;

    //@NotBlank(message = "Precio obligatorio") - Not applicable on Double
    //@Size(min = 1, message = "El precio debe ser mayor a 1") - Not applicable on Double
    private Double precio;

    private Integer stock;
}


