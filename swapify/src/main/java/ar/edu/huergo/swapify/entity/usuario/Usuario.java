package ar.edu.huergo.swapify.entity.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "mail")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // se genera automáticamente

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Email(message = "Email inválido")
    @NotBlank(message = "El mail es obligatorio")
    @Column(name = "mail", nullable = false, length = 150)
    private String mail;

    @NotBlank(message = "El número de teléfono es obligatorio")
    @Column(name = "telefono", nullable = false, length = 30)
    private String numeroDeTelefono;

    // SOLO para pruebas. Más adelante, hashealo (BCrypt) antes de guardar.
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false, length = 255)
    private String contraseña;
}
