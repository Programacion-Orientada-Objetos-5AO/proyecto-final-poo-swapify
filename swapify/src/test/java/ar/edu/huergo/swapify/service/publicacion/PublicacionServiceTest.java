package ar.edu.huergo.swapify.service.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.publicacion.PublicacionImagen;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
public class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PublicacionMapper publicacionMapper;

    @Mock
    private ar.edu.huergo.swapify.repository.security.UsuarioRepository usuarioRepository;

    @Mock
    private ar.edu.huergo.swapify.repository.publicacion.OfertaRepository ofertaRepository;

    @InjectMocks
    private PublicacionService publicacionService;

    @Test
    public void testCrearPublicacion_Success() {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/png");
        dto.getImagenesBase64().add("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcAAn8B9pEJcwAAAABJRU5ErkJggg==");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"),
                "Libro de programación", "Otro libro", LocalDateTime.now(), usuario, List.of(), null, null);

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Publicacion result = publicacionService.crearPublicacion(dto, usuario);

        assertThat(result.tieneImagenes()).isTrue();
        assertThat(result.getImagenesOrdenadas()).hasSize(1);
        PublicacionImagen imagen = result.getImagenesOrdenadas().get(0);
        assertThat(imagen.getDatos()).isNotEmpty();
        assertThat(imagen.getContentType()).isEqualTo("image/png");
        assertThat(imagen.getDataUri()).startsWith("data:image/png;base64,");
        verify(publicacionRepository).save(publicacion);
    }

    @Test
    public void testCrearPublicacion_SinImagenLanzaExcepcion() {
        CrearPublicacionDTO dto = crearDtoBasico();

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion();

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(dto, usuario));

        assertThat(exception.getMessage()).isEqualTo("Debés adjuntar al menos una imagen");
    }

    @Test
    public void testCrearPublicacion_Base64Invalido() {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/png");
        dto.getImagenesBase64().add("no_es_base64");

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion();

        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(publicacionMapper.toEntity(dto)).thenReturn(publicacion);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(dto, usuario));

        assertThat(exception.getMessage()).containsIgnoringCase("base64");
    }

    @Test
    public void testCrearPublicacion_DtoNull() {
        Usuario usuario = new Usuario("test@example.com", "password");

        assertThrows(IllegalArgumentException.class, () -> publicacionService.crearPublicacion(null, usuario));
    }

    @Test
    public void testListarTodas() {
        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion pub1 = new Publicacion(null, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                LocalDateTime.now(), usuario, List.of(), null, null);
        Publicacion pub2 = new Publicacion(null, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2",
                LocalDateTime.now(), usuario, List.of(), null, null);
        List<Publicacion> publicaciones = Arrays.asList(pub1, pub2);

        when(publicacionRepository.findAllByOrderByFechaPublicacionDesc()).thenReturn(publicaciones);

        List<Publicacion> result = publicacionService.listarTodas();

        assertEquals(publicaciones, result);
    }

    @Test
    public void testListarTodas_PopulaDataUriParaImagenes() {
        Usuario usuario = new Usuario("test@example.com", "password");
        PublicacionImagen imagen = new PublicacionImagen();
        imagen.setDatos(new byte[] {1, 2, 3, 4});
        imagen.setContentType("image/png");
        Publicacion pub = new Publicacion(1L, "Libro", BigDecimal.TEN, "Desc", "Obj",
                LocalDateTime.now(), usuario, List.of(imagen), null, null);

        when(publicacionRepository.findAllByOrderByFechaPublicacionDesc()).thenReturn(List.of(pub));

        List<Publicacion> result = publicacionService.listarTodas();

        PublicacionImagen procesada = result.get(0).getImagenesOrdenadas().get(0);
        assertThat(procesada.getBase64()).isNotBlank();
        assertThat(procesada.getDataUri()).startsWith("data:image/png;base64,");
    }

    @Test
    public void testObtenerPorId_Success() {
        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion publicacion = new Publicacion(null, "Libro", new BigDecimal("100.00"), "Desc", "Obj",
                LocalDateTime.now(), usuario, List.of(), null, null);
        when(publicacionRepository.findById(1L)).thenReturn(Optional.of(publicacion));

        Publicacion result = publicacionService.obtenerPorId(1L);

        assertEquals(publicacion, result);
    }

    @Test
    public void testObtenerPorId_NotFound() {
        when(publicacionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> publicacionService.obtenerPorId(1L));
    }

    @Test
    public void testObtenerPublicacionesDeFecha() {
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        Usuario usuario = new Usuario("test@example.com", "password");
        Publicacion pub1 = new Publicacion(1L, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                inicio.plusHours(1), usuario, List.of(), null, null);
        List<Publicacion> publicaciones = Arrays.asList(pub1);

        when(publicacionRepository.findByFechaPublicacionBetween(inicio, fin)).thenReturn(publicaciones);

        List<Publicacion> result = publicacionService.obtenerPublicacionesDeFecha(fecha);

        assertEquals(publicaciones, result);
    }

    @Test
    public void testSumaPreciosEnFecha() {
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        BigDecimal suma = new BigDecimal("300.00");

        when(publicacionRepository.sumaPreciosEntre(inicio, fin)).thenReturn(suma);

        BigDecimal result = publicacionService.sumaPreciosEnFecha(fecha);

        assertEquals(suma, result);
    }

    @Test
    public void testEliminarPublicacionPropietario() {
        Usuario propietario = new Usuario("propietario@example.com", "secret");
        Publicacion publicacion = new Publicacion(10L, "Libro", BigDecimal.TEN, "Desc", "Obj",
                LocalDateTime.now(), propietario, List.of(), null, null);

        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacion));

        publicacionService.eliminarPublicacion(10L, "propietario@example.com", false);

        verify(ofertaRepository).deleteByPublicacionId(10L);
        verify(publicacionRepository).delete(publicacion);
    }

    @Test
    public void testEliminarPublicacionAdmin() {
        Usuario propietario = new Usuario("propietario@example.com", "secret");
        Publicacion publicacion = new Publicacion(11L, "Libro", BigDecimal.TEN, "Desc", "Obj",
                LocalDateTime.now(), propietario, List.of(), null, null);

        when(publicacionRepository.findById(11L)).thenReturn(Optional.of(publicacion));

        publicacionService.eliminarPublicacion(11L, "admin@example.com", true);

        verify(ofertaRepository).deleteByPublicacionId(11L);
        verify(publicacionRepository).delete(publicacion);
    }

    @Test
    public void testEliminarPublicacionSinPermiso() {
        Usuario propietario = new Usuario("propietario@example.com", "secret");
        Publicacion publicacion = new Publicacion(12L, "Libro", BigDecimal.TEN, "Desc", "Obj",
                LocalDateTime.now(), propietario, List.of(), null, null);

        when(publicacionRepository.findById(12L)).thenReturn(Optional.of(publicacion));

        assertThrows(AccessDeniedException.class,
                () -> publicacionService.eliminarPublicacion(12L, "otro@example.com", false));

        verify(ofertaRepository, never()).deleteByPublicacionId(12L);
        verify(publicacionRepository, never()).delete(publicacion);
    }

    @Test
    public void testEliminarPublicacionNoEncontrada() {
        when(publicacionRepository.findById(13L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> publicacionService.eliminarPublicacion(13L, "usuario@example.com", false));

        verify(ofertaRepository, never()).deleteByPublicacionId(13L);
        verify(publicacionRepository, never()).delete(any(Publicacion.class));
    }

    @Test
    public void testSumaPreciosEnFecha_Null() {
        LocalDate fecha = LocalDate.of(2023, 1, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        when(publicacionRepository.sumaPreciosEntre(inicio, fin)).thenReturn(null);

        BigDecimal result = publicacionService.sumaPreciosEnFecha(fecha);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testCrearPublicacion_DecodeBase64ConSaltosDeLinea() {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/png");
        dto.getImagenesBase64().add("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcA\nAn8B9pEJcwAAAABJRU5ErkJggg==");

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

        PublicacionImagen imagen = resultado.getImagenesOrdenadas().get(0);
        assertThat(imagen.getDatos()).isNotEmpty();
        assertThat(imagen.getContentType()).isEqualTo("image/png");
        assertThat(imagen.getDataUri()).startsWith("data:image/png;base64,");
    }

    @Test
    public void testCrearPublicacion_DecodeBase64ConEspaciosComoMas() {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/jpeg");
        dto.getImagenesBase64().add("/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAwICQgICAwKCQwLCw4RDRYPDxkZGhkZGSclHSoqKi4xNDQ0NTw8Pj84QkFCQkJFRUVFRkpKSkpKSkpK/2wBDAQ0NDhIRERgVFRgqHCAoKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqKCoqK//AABEIAAEAAgMBIgACEQEDEQH/xAAXAAADAQAAAAAAAAAAAAAAAAAAAAUG/8QAFhABAQEAAAAAAAAAAAAAAAAAABEB/8QAFQEBAQAAAAAAAAAAAAAAAAAAAgT/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwD9AQ//2Q==".replace('+', ' '));

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

        PublicacionImagen imagen = resultado.getImagenesOrdenadas().get(0);
        assertThat(imagen.getDatos()).isNotEmpty();
        assertThat(imagen.getContentType()).isEqualTo("image/jpeg");
        assertThat(imagen.getDataUri()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    public void testCrearPublicacion_ComprimeImagenGrande() throws Exception {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/png");
        dto.getImagenesBase64().add(generarImagenAleatoriaBase64(3000));

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

        PublicacionImagen imagen = resultado.getImagenesOrdenadas().get(0);
        assertThat(imagen.getDatos()).isNotEmpty();
        assertThat(imagen.getDatos().length).isLessThanOrEqualTo(5_000_000);
        assertThat(imagen.getContentType()).isEqualTo("image/png");
    }

    @Test
    void testCrearPublicacion_LanzaErrorAmigableCuandoNoHayMemoria() {
        CrearPublicacionDTO dto = crearDtoBasico();
        dto.getImagenesContentType().add("image/png");
        dto.getImagenesBase64().add("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/AcAAn8B9pEJcwAAAABJRU5ErkJggg==");

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

    private CrearPublicacionDTO crearDtoBasico() {
        CrearPublicacionDTO dto = new CrearPublicacionDTO();
        dto.setNombre("Libro");
        dto.setPrecio(new BigDecimal("100.00"));
        dto.setDescripcion("Libro de programación");
        dto.setObjetoACambiar("Otro libro");
        return dto;
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
