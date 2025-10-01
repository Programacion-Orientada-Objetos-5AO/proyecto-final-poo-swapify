package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ar.edu.huergo.swapify.entity.security.Usuario;

public class PublicacionTest {

    @Test
    public void testNoArgsConstructorAndSettersGetters() {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(1L);
        publicacion.setNombre("Libro");
        publicacion.setPrecio(new BigDecimal("100.00"));
        publicacion.setDescripcion("Libro de programación");
        publicacion.setObjetoACambiar("Otro libro");
        publicacion.setFechaPublicacion(LocalDateTime.of(2023, 1, 1, 12, 0));
        byte[] imagen = new byte[] {1, 2, 3};
        publicacion.setImagen(imagen);
        publicacion.setImagenContentType("image/png");

        assertEquals(1L, publicacion.getId());
        assertEquals("Libro", publicacion.getNombre());
        assertEquals(new BigDecimal("100.00"), publicacion.getPrecio());
        assertEquals("Libro de programación", publicacion.getDescripcion());
        assertEquals("Otro libro", publicacion.getObjetoACambiar());
        assertEquals(LocalDateTime.of(2023, 1, 1, 12, 0), publicacion.getFechaPublicacion());
        assertArrayEquals(imagen, publicacion.getImagen());
        assertNotSame(imagen, publicacion.getImagen());
        assertEquals("image/png", publicacion.getImagenContentType());
        assertTrue(publicacion.tieneImagen());
    }

    @Test
    public void testAllArgsConstructor() {
        LocalDateTime fecha = LocalDateTime.of(2023, 2, 2, 15, 30);
        Usuario usuario = new Usuario("test@example.com", "password");
        byte[] imagen = new byte[] {9, 8, 7};
        String contentType = "image/jpeg";
        Publicacion publicacion = new Publicacion(2L, "Mesa", new BigDecimal("200.00"),
                "Mesa de madera", "Silla", fecha, usuario, imagen, contentType, null, null);

        assertEquals(2L, publicacion.getId());
        assertEquals("Mesa", publicacion.getNombre());
        assertEquals(new BigDecimal("200.00"), publicacion.getPrecio());
        assertEquals("Mesa de madera", publicacion.getDescripcion());
        assertEquals("Silla", publicacion.getObjetoACambiar());
        assertEquals(fecha, publicacion.getFechaPublicacion());
        assertEquals(usuario, publicacion.getUsuario());
        assertArrayEquals(imagen, publicacion.getImagen());
        assertNotSame(imagen, publicacion.getImagen());
        assertEquals(contentType, publicacion.getImagenContentType());
    }

    @Test
    public void testPrePersistSetsFechaPublicacionIfNull() {
        Publicacion publicacion = new Publicacion();
        publicacion.setFechaPublicacion(null);

        publicacion.prePersist();

        assertNotNull(publicacion.getFechaPublicacion());
        assertTrue(publicacion.getFechaPublicacion().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(publicacion.getFechaPublicacion().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    public void testPrePersistDoesNotOverrideFechaPublicacionIfSet() {
        LocalDateTime fecha = LocalDateTime.of(2023, 3, 3, 10, 0);
        Publicacion publicacion = new Publicacion();
        publicacion.setFechaPublicacion(fecha);

        publicacion.prePersist();

        assertEquals(fecha, publicacion.getFechaPublicacion());
    }

    @Test
    public void testLimpiarImagen() {
        Publicacion publicacion = new Publicacion();
        publicacion.setImagen(new byte[] {4, 5, 6});
        publicacion.setImagenContentType("image/png");

        publicacion.limpiarImagen();

        assertNull(publicacion.getImagen());
        assertNull(publicacion.getImagenContentType());
        assertFalse(publicacion.tieneImagen());
    }
}
