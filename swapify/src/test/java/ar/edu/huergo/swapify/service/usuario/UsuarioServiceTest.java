package ar.edu.huergo.swapify.service.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.huergo.swapify.dto.usuario.CrearUsuarioDTO;
import ar.edu.huergo.swapify.entity.usuario.Usuario;
import ar.edu.huergo.swapify.mapper.usuario.UsuarioMapper;
import ar.edu.huergo.swapify.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    public void testCrear_Success() {
        // Given
        CrearUsuarioDTO dto = new CrearUsuarioDTO();
        dto.setNombre("Juan");
        dto.setMail("juan@example.com");
        dto.setNumeroDeTelefono("123456789");
        dto.setContraseña("password123");
        Usuario usuario = new Usuario(1L, "Juan", "juan@example.com", "123456789", "password123");

        when(usuarioRepository.existsByMailIgnoreCase(dto.getMail())).thenReturn(false);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        // When
        Usuario result = usuarioService.crear(dto);

        // Then
        assertEquals(usuario, result);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    public void testCrear_DtoNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> usuarioService.crear(null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    public void testCrear_MailExists() {
        // Given
        CrearUsuarioDTO dto = new CrearUsuarioDTO();
        dto.setNombre("Juan");
        dto.setMail("juan@example.com");
        dto.setNumeroDeTelefono("123456789");
        dto.setContraseña("password123");

        when(usuarioRepository.existsByMailIgnoreCase(dto.getMail())).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> usuarioService.crear(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    public void testListarTodos() {
        // Given
        Usuario usuario1 = new Usuario(1L, "Juan", "juan@example.com", "123", "pass1");
        Usuario usuario2 = new Usuario(2L, "Ana", "ana@example.com", "456", "pass2");
        List<Usuario> usuarios = Arrays.asList(usuario1, usuario2);

        when(usuarioRepository.findAll()).thenReturn(usuarios);

        // When
        List<Usuario> result = usuarioService.listarTodos();

        // Then
        assertEquals(usuarios, result);
    }

    @Test
    public void testObtenerPorId_Success() {
        // Given
        Usuario usuario = new Usuario(1L, "Juan", "juan@example.com", "123", "pass");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // When
        Usuario result = usuarioService.obtenerPorId(1L);

        // Then
        assertEquals(usuario, result);
    }

    @Test
    public void testObtenerPorId_NotFound() {
        // Given
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> usuarioService.obtenerPorId(1L));
    }
}
