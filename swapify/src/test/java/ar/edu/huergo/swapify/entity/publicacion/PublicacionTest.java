package ar.edu.huergo.swapify.entity.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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

        assertEquals(1L, publicacion.getId());
        assertEquals("Libro", publicacion.getNombre());
        assertEquals(new BigDecimal("100.00"), publicacion.getPrecio());
        assertEquals("Libro de programación", publicacion.getDescripcion());
        assertEquals("Otro libro", publicacion.getObjetoACambiar());
        assertEquals(LocalDateTime.of(2023, 1, 1, 12, 0), publicacion.getFechaPublicacion());
    }

    @Test
    public void testAllArgsConstructor() {
        LocalDateTime fecha = LocalDateTime.of(2023, 2, 2, 15, 30);
        Publicacion publicacion = new Publicacion(2L, "Mesa", new BigDecimal("200.00"), "Mesa de madera", "Silla", fecha);

        assertEquals(2L, publicacion.getId());
        assertEquals("Mesa", publicacion.getNombre());
        assertEquals(new BigDecimal("200.00"), publicacion.getPrecio());
        assertEquals("Mesa de madera", publicacion.getDescripcion());
        assertEquals("Silla", publicacion.getObjetoACambiar());
        assertEquals(fecha, publicacion.getFechaPublicacion());
    }

    @Test
    public void testPrePersistSetsFechaPublicacionIfNull() {
        Publicacion publicacion = new Publicacion();
        publicacion.setFechaPublicacion(null);

        publicacion.prePersist();

        assertNotNull(publicacion.getFechaPublicacion());
        // The fechaPublicacion should be close to now, allow a few seconds difference
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
}
