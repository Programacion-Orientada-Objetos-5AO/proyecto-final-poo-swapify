package ar.edu.huergo.swapify.entity.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class UsuarioTest {

    @Test
    public void testNoArgsConstructorAndSettersGetters() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Juan");
        usuario.setMail("juan@example.com");
        usuario.setNumeroDeTelefono("123456789");
        usuario.setContraseña("password123");

        assertEquals(1L, usuario.getId());
        assertEquals("Juan", usuario.getNombre());
        assertEquals("juan@example.com", usuario.getMail());
        assertEquals("123456789", usuario.getNumeroDeTelefono());
        assertEquals("password123", usuario.getContraseña());
    }

    @Test
    public void testAllArgsConstructor() {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", "987654321", "pass456");

        assertEquals(1L, usuario.getId());
        assertEquals("Ana", usuario.getNombre());
        assertEquals("ana@example.com", usuario.getMail());
        assertEquals("987654321", usuario.getNumeroDeTelefono());
        assertEquals("pass456", usuario.getContraseña());
    }
}
