package ar.edu.huergo.swapify.entity.publicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

        PublicacionImagen imagen = new PublicacionImagen();
        imagen.setDatos(new byte[] {1, 2, 3});
        imagen.setContentType("image/png");
        publicacion.agregarImagen(imagen);

        assertEquals(1L, publicacion.getId());
        assertEquals("Libro", publicacion.getNombre());
        assertEquals(new BigDecimal("100.00"), publicacion.getPrecio());
        assertEquals("Libro de programación", publicacion.getDescripcion());
        assertNotNull(publicacion.getArticulo());
        assertEquals("Libro", publicacion.getArticulo().getNombre());
        assertEquals("Otro libro", publicacion.getObjetoACambiar());
        assertEquals(LocalDateTime.of(2023, 1, 1, 12, 0), publicacion.getFechaPublicacion());
        assertTrue(publicacion.tieneImagenes());
        assertEquals(1, publicacion.getImagenesOrdenadas().size());
        PublicacionImagen primera = publicacion.getImagenesOrdenadas().get(0);
        assertEquals("image/png", primera.getContentType());
        assertEquals(0, primera.getOrden());
        assertEquals(publicacion, primera.getPublicacion());
    }

    @Test
    public void testAllArgsConstructor() {
        LocalDateTime fecha = LocalDateTime.of(2023, 2, 2, 15, 30);
        Usuario usuario = new Usuario("test@example.com", "password");
        PublicacionImagen imagen = new PublicacionImagen();
        imagen.setDatos(new byte[] {9, 8, 7});
        imagen.setContentType("image/jpeg");
        Publicacion publicacion = new Publicacion(2L, "Mesa", new BigDecimal("200.00"),
                "Mesa de madera", "Silla", fecha, usuario, List.of(imagen), null, null);

        assertEquals(2L, publicacion.getId());
        assertEquals("Mesa", publicacion.getNombre());
        assertEquals(new BigDecimal("200.00"), publicacion.getPrecio());
        assertEquals("Mesa de madera", publicacion.getDescripcion());
        assertNotNull(publicacion.getArticulo());
        assertEquals("Mesa", publicacion.getArticulo().getNombre());
        assertEquals("Silla", publicacion.getObjetoACambiar());
        assertEquals(fecha, publicacion.getFechaPublicacion());
        assertEquals(usuario, publicacion.getUsuario());
        assertTrue(publicacion.tieneImagenes());
        PublicacionImagen principal = publicacion.getImagenPrincipal();
        assertNotNull(principal);
        assertEquals("image/jpeg", principal.getContentType());
        assertEquals(0, principal.getOrden());
        assertEquals(publicacion, principal.getPublicacion());
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
    public void testLimpiarImagenes() {
        Publicacion publicacion = new Publicacion();
        PublicacionImagen imagen = new PublicacionImagen();
        imagen.setDatos(new byte[] {4, 5, 6});
        imagen.setContentType("image/png");
        publicacion.agregarImagen(imagen);

        publicacion.limpiarImagenes();

        assertFalse(publicacion.tieneImagenes());
        assertEquals(0, publicacion.getImagenesOrdenadas().size());
    }
}
