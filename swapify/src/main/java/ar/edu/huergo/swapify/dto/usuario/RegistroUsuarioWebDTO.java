package ar.edu.huergo.swapify.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Modelo de formulario para registrar nuevas cuentas desde la interfaz web.
 */
@Data
public class RegistroUsuarioWebDTO {
    @NotBlank @Email
    private String username;

    @NotBlank @Size(min = 6, max = 60)
    private String password;

    @NotBlank
    private String nombre;

    @NotBlank
    private String numeroDeTelefono;
}
