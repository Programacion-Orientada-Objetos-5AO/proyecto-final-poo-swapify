package ar.edu.huergo.swapify.service.publicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PublicacionMapper publicacionMapper;

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

        Publicacion publicacion = new Publicacion(1L, "Libro", new BigDecimal("100.00"), "Libro de programación", "Otro libro", LocalDateTime.now());

        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);
        when(publicacionRepository.save(publicacion)).thenReturn(publicacion);

        // When
        Publicacion result = publicacionService.crearPublicacion(dto);

        // Then
        assertEquals(publicacion, result);
        verify(publicacionRepository).save(publicacion);
    }

    @Test
    public void testCrearPublicacion_DtoNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> publicacionService.crearPublicacion(null));
    }

    @Test
    public void testListarTodas() {
        // Given
        Publicacion pub1 = new Publicacion(1L, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1", LocalDateTime.now());
        Publicacion pub2 = new Publicacion(2L, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2", LocalDateTime.now());
        List<Publicacion> publicaciones = Arrays.asList(pub1, pub2);

        when(publicacionRepository.findAll()).thenReturn(publicaciones);

        // When
        List<Publicacion> result = publicacionService.listarTodas();

        // Then
        assertEquals(publicaciones, result);
    }

    @Test
    public void testObtenerPorId_Success() {
        // Given
        Publicacion publicacion = new Publicacion(1L, "Libro", new BigDecimal("100.00"), "Desc", "Obj", LocalDateTime.now());
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

        Publicacion pub1 = new Publicacion(1L, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1", inicio.plusHours(1));
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
