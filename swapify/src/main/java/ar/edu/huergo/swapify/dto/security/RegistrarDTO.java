package ar.edu.huergo.swapify.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrarDTO(
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String username,
        @NotBlank(message = "El nombre de usuario es requerido")
        String nombre,
        @NotBlank(message = "La contraseña es requerida")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{16,}$", message = "La contraseña debe tener al menos 16 caracteres, una mayúscula, una minúscula, un número y un carácter especial")
        String password,
        @NotBlank(message = "La verificación de contraseña es requerida")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{16,}$", message = "La verificación de contraseña debe tener al menos 16 caracteres, una mayúscula, una minúscula, un número y un carácter especial")
        String verificacionPassword) {
}
