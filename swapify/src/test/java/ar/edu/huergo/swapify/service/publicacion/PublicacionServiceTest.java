package ar.edu.huergo.swapify.service.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    @Test.
    public void testCrearPublicacion_Success() {
        // Given
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Libro");
        dto.setPrecio(new BigDecimal("100.00"));
        dto.setDescripcion("Libro de programación");
        dto.setObjetoACambiar("Otro libro");
        dto.setImagenContentType("image/png");
        dto.setImagenBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcAAn8B9pEJcwAAAABJRU5ErkJggg==");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"),
                "Libro de programación", "Otro libro", LocalDateTime.now(), usuario, null, null, null, null);

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
    public void testCrearPublicacion_SinImagenLanzaExcepcion() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Libro");
        dto.setPrecio(new BigDecimal("100.00"));
        dto.setDescripcion("Libro de programación");
        dto.setObjetoACambiar("Otro libro");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion();

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(dto, usuario));

        assertThat(exception.getMessage()).isEqualTo("La imagen es obligatoria");
    }

    @Test
    public void testCrearPublicacion_Base64Invalido() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Libro");
        dto.setPrecio(new BigDecimal("100.00"));
        dto.setDescripcion("Libro de programación");
        dto.setObjetoACambiar("Otro libro");
        dto.setImagenContentType("image/png");
        dto.setImagenBase64("no_es_base64");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion();

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(dto, usuario));

        assertThat(exception.getMessage()).isEqualTo("La imagen está dañada o tiene un formato inválido");
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
                LocalDateTime.now(), usuario, null, null, null, null);
        Publicacion pub2 = new Publicacion(null, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2",
                LocalDateTime.now(), usuario, null, null, null, null);
        List<Publicacion> publicaciones = Arrays.asList(pub1, pub2);

        when(publicacionRepository.findAllByOrderByFechaPublicacionDesc()).thenReturn(publicaciones);

        // When
        List<Publicacion> result = publicacionService.listarTodas();

        // Then
        assertEquals(publicaciones, result);
    }

    @Test
    public void testListarTodas_PopulaDataUriParaImagenes() {
        Usuario usuario = new Usuario("test@example.com", "password");
        byte[] imagen = new byte[] {1, 2, 3, 4};
        Publicacion pub = new Publicacion(1L, "Libro", BigDecimal.TEN, "Desc", "Obj",
                LocalDateTime.now(), usuario, imagen, "image/png", null, null);

        when(publicacionRepository.findAllByOrderByFechaPublicacionDesc()).thenReturn(List.of(pub));

        List<Publicacion> result = publicacionService.listarTodas();

        assertThat(result.get(0).getImagenBase64()).isNotBlank();
        assertThat(result.get(0).getImagenDataUri())
                .startsWith("data:image/png;base64,");
    }

    @Test
    public void testObtenerPorId_Success() {
        // Given
        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"), "Desc", "Obj",
                LocalDateTime.now(), usuario, null, null, null, null);
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
                inicio.plusHours(1), usuario, null, null, null, null);
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

    @Test
    public void testCrearPublicacion_DecodeBase64ConSaltosDeLinea() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Bicicleta");
        dto.setPrecio(new BigDecimal("250.00"));
        dto.setDescripcion("Rodado 29 casi nueva");
        dto.setObjetoACambiar("Notebook");
        dto.setImagenContentType("image/png");
        dto.setImagenBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcA\nAn8B9pEJcwAAAABJRU5ErkJggg==");

        Usuario usuario = new Usuario("usuario@test.com", "secreta");
        Publicacion entidad = new Publicacion();
        entidad.setNombre(dto.getNombre());
        entidad.setPrecio(dto.getPrecio());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setObjetoACambiar(dto.getObjetoACambiar());

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(entidad);
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Publicacion resultado = publicacionService.crearPublicacion(dto, usuario);

        assertThat(resultado.getImagen()).isNotNull();
        assertThat(resultado.getImagen().length).isGreaterThan(0);
        assertEquals("image/png", resultado.getImagenContentType());
    }

    @Test
    public void testCrearPublicacion_DecodeBase64ConEspaciosComoMas() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Cámara");
        dto.setPrecio(new BigDecimal("999.99"));
        dto.setDescripcion("Incluye lente 50mm");
        dto.setObjetoACambiar("Tablet");
        dto.setImagenContentType("image/jpeg");
        dto.setImagenBase64("/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAwICQgICAwKCQwLCw4RDRYPDxkZGhkZGSclHSoqKi4xNDQ0NTw8Pj84QkFCQkJFRUVFRkpKSkpKSkpK/2wBDAQ0NDhIRERgVFRgqHCAoKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqK//AABEIAAEAAgMBIgACEQEDEQH/xAAXAAADAQAAAAAAAAAAAAAAAAAAAAUG/8QAFhABAQEAAAAAAAAAAAAAAAAAABEB/8QAFQEBAQAAAAAAAAAAAAAAAAAAAgT/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwD9AQ//2Q=="
                .replace('+', ' '));

        Usuario usuario = new Usuario("espacios@test.com", "secreta");
        Publicacion entidad = new Publicacion();
        entidad.setNombre(dto.getNombre());
        entidad.setPrecio(dto.getPrecio());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setObjetoACambiar(dto.getObjetoACambiar());

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(entidad);
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Publicacion resultado = publicacionService.crearPublicacion(dto, usuario);

        assertThat(resultado.getImagen()).isNotNull();
        assertThat(resultado.getImagen().length).isGreaterThan(0);
        assertEquals("image/jpeg", resultado.getImagenContentType());
    }

    @Test
    public void testCrearPublicacion_ComprimeImagenGrande() throws Exception {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Cuadro");
        dto.setPrecio(new BigDecimal("1200.00"));
        dto.setDescripcion("Pintura artística en lienzo");
        dto.setObjetoACambiar("Bicicleta");
        dto.setImagenContentType("image/png");
        dto.setImagenBase64(generarImagenAleatoriaBase64(3000));

        Usuario usuario = new Usuario("grande@test.com", "secreta");
        Publicacion entidad = new Publicacion();
        entidad.setNombre(dto.getNombre());
        entidad.setPrecio(dto.getPrecio());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setObjetoACambiar(dto.getObjetoACambiar());

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(entidad);
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Publicacion resultado = publicacionService.crearPublicacion(dto, usuario);

        assertThat(resultado.getImagen()).isNotNull();
        assertThat(resultado.getImagen().length).isGreaterThan(0);
        assertThat(resultado.getImagen().length).isLessThanOrEqualTo(5_000_000);
        assertEquals("image/png", resultado.getImagenContentType());
    }

    @Test
    void testCrearPublicacion_LanzaErrorAmigableCuandoNoHayMemoria() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Poster gigante");
        dto.setPrecio(new BigDecimal("500.00"));
        dto.setDescripcion("Imagen enorme");
        dto.setObjetoACambiar("Auto");
        dto.setImagenContentType("image/png");
        dto.setImagenBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcAAn8B9pEJcwAAAABJRU5ErkJggg==");

        Usuario usuario = new Usuario("sin-memoria@test.com", "clave");
        Publicacion entidad = new Publicacion();

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(entidad);

        try (MockedStatic<ImageIO> imageIO = org.mockito.Mockito.mockStatic(ImageIO.class)) {
            imageIO.when(() -> ImageIO.read(any(ByteArrayInputStream.class)))
                    .thenThrow(new OutOfMemoryError("Simulado"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> publicacionService.crearPublicacion(dto, usuario));

            assertThat(ex.getMessage())
                    .isEqualTo("La imagen es demasiado grande para procesarla. Reducila e intentá nuevamente.");
        }
    }

    private String generarImagenAleatoriaBase64(int dimension) throws Exception {
        BufferedImage imagen = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagen.createGraphics();
        try {
            Random random = new Random(42L);
            for (int y = 0; y < dimension; y++) {
                for (int x = 0; x < dimension; x++) {
                    g2d.setColor(new Color(random.nextInt(0x1000000)));
                    g2d.fillRect(x, y, 1, 1);
                }
            }
        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", baos);
        byte[] datos = baos.toByteArray();
        assertThat(datos.length).isGreaterThan(5_000_000);
        return Base64.getEncoder().encodeToString(datos);
    }
}
