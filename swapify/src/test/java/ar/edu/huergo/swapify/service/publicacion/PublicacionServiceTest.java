package ar.edu.huergo.swapify.service.publicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PublicacionMapper publicacionMapper;

    @Mock
    private ar.edu.huergo.swapify.repository.security.UsuarioRepository usuarioRepository;

    @InjectMocks
    private PublicacionService publicacionService;

    @Test
    public void testCrearPublicacion_Success() {
        // Given
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Libro");
        dto.setPrecio(new BigDecimal("100.00"));
        dto.setDescripcion("Libro de programación");
        dto.setObjetoACambiar("Otro libro");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"),
                "Libro de programación", "Otro libro", LocalDateTime.now(), usuario, null, null);

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(java.util.Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);
        when(publicacionRepository.save(publicacion)).thenReturn(publicacion);

        // When
        Publicacion result = publicacionService.crearPublicacion(dto, usuario);

        // Then
        assertEquals(publicacion, result);
        verify(publicacionRepository).save(publicacion);
    }

    @Test
    public void testCrearPublicacion_DtoNull() {
        // Given
        Usuario usuario = new Usuario("test@example.com", "password");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> publicacionService.crearPublicacion(null, usuario));
    }

    @Test
    public void testListarTodas() {
        // Given
        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion pub1 = new Publicacion(null, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                LocalDateTime.now(), usuario, null, null);
        Publicacion pub2 = new Publicacion(null, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2",
                LocalDateTime.now(), usuario, null, null);
        List<Publicacion> publicaciones = Arrays.asList(pub1, pub2);

        when(publicacionRepository.findAllByOrderByFechaPublicacionDesc()).thenReturn(publicaciones);

        // When
        List<Publicacion> result = publicacionService.listarTodas();

        // Then
        assertEquals(publicaciones, result);
    }

    @Test
    public void testObtenerPorId_Success() {
        // Given
        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"), "Desc", "Obj",
                LocalDateTime.now(), usuario, null, null);
        when(publicacionRepository.findById(1L)).thenReturn(Optional.of(publicacion));

        // When
        Publicacion result = publicacionService.obtenerPorId(1L);

        // Then
        assertEquals(publicacion, result);
    }

    @Test
    public void testObtenerPorId_NotFound() {
        // Given
        when(publicacionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> publicacionService.obtenerPorId(1L));
    }

    @Test
    public void testObtenerPublicacionesDeFecha() {
        // Given
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion pub1 = new Publicacion(1L, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                inicio.plusHours(1), usuario, null, null);
        List<Publicacion> publicaciones = Arrays.asList(pub1);

        when(publicacionRepository.findByFechaPublicacionBetween(inicio, fin)).thenReturn(publicaciones);

        // When
        List<Publicacion> result = publicacionService.obtenerPublicacionesDeFecha(fecha);

        // Then
        assertEquals(publicaciones, result);
    }

    @Test
    public void testSumaPreciosEnFecha() {
        // Given
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        BigDecimal suma = new BigDecimal("300.00");

        when(publicacionRepository.sumaPreciosEntre(inicio, fin)).thenReturn(suma);

        // When
        BigDecimal result = publicacionService.sumaPreciosEnFecha(fecha);

        // Then
        assertEquals(suma, result);
    }

    @Test
    public void testSumaPreciosEnFecha_Null() {
        // Given
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        when(publicacionRepository.sumaPreciosEntre(inicio, fin)).thenReturn(null);

        // When
        BigDecimal result = publicacionService.sumaPreciosEnFecha(fecha);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }
}
