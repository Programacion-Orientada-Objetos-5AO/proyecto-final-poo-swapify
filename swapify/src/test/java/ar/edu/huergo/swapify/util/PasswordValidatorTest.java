package ar.edu.huergo.swapify.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

    @Test
    @DisplayName("Debería aceptar contraseñas con mayúsculas y números")
    void deberiaAceptarContraseniaValida() {
        assertTrue(PasswordValidator.isValid("Password123"));
    }

    @Test
    @DisplayName("Debería aceptar contraseñas válidas con caracteres especiales")
    void deberiaAceptarContraseniaConCaracteresEspeciales() {
        PasswordValidator.validate("Swapify123#");
    }

    @Test
    @DisplayName("Debería rechazar contraseñas sin mayúsculas")
    void deberiaRechazarSinMayusculas() {
        assertFalse(PasswordValidator.isValid("password123"));
    }

    @Test
    @DisplayName("Debería rechazar contraseñas sin números")
    void deberiaRechazarSinNumeros() {
        assertFalse(PasswordValidator.isValid("Password"));
    }

    @Test
    @DisplayName("Debería rechazar contraseñas demasiado cortas")
    void deberiaRechazarContraseniasCortas() {
        assertFalse(PasswordValidator.isValid("Pa12"));
    }

    @Test
    @DisplayName("Debería lanzar excepción con mensaje descriptivo")
    void deberiaLanzarExcepcionConMensaje() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PasswordValidator.validate("password"));

        assertTrue(exception.getMessage()
                .contains("al menos 8 caracteres, una letra mayúscula y un número"));
    }
}
