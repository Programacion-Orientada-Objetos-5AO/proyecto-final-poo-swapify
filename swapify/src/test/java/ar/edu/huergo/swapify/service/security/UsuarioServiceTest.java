package ar.edu.huergo.swapify.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ar.edu.huergo.swapify.entity.security.Rol;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.security.RolRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;

/**
 * Tests de unidad para UsuarioService
 * 
 * CONCEPTOS DEMOSTRADOS: 1. Testing de servicios con dependencias de seguridad 2. Verificación de
 * encriptación de contraseñas 3. Testing de validaciones de negocio 4. Manejo de excepciones
 * personalizadas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Unidad - UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEjemplo;
    private Rol rolCliente;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1L);
        usuarioEjemplo.setUsername("usuario@test.com");
        usuarioEjemplo.setPassword("password123");

        rolCliente = new Rol();
        rolCliente.setId(1L);
        rolCliente.setNombre("CLIENTE");
    }

    @Test
    @DisplayName("Debería obtener todos los usuarios")
    void deberiaObtenerTodosLosUsuarios() {
        // Given
        List<Usuario> usuariosEsperados = Arrays.asList(usuarioEjemplo);
        when(usuarioRepository.findAll()).thenReturn(usuariosEsperados);

        // When
        List<Usuario> resultado = usuarioService.getAllUsuarios();

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(usuarioEjemplo.getUsername(), resultado.get(0).getUsername());
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debería registrar usuario correctamente")
    void deberiaRegistrarUsuarioCorrectamente() {
        // Given
        String password = "password123";
        String verificacionPassword = "password123";
        String passwordEncriptado = "encrypted_password";

        when(usuarioRepository.existsByUsername(usuarioEjemplo.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(passwordEncriptado);
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEjemplo);

        // When
        Usuario resultado =
                usuarioService.registrar(usuarioEjemplo, password, verificacionPassword);

        // Then
        assertNotNull(resultado);
        verify(usuarioRepository, times(1)).existsByUsername(usuarioEjemplo.getUsername());
        verify(passwordEncoder, times(1)).encode(password);
        verify(rolRepository, times(1)).findByNombre("CLIENTE");
        verify(usuarioRepository, times(1)).save(usuarioEjemplo);

        // Verificar que la contraseña fue encriptada
        assertEquals(passwordEncriptado, usuarioEjemplo.getPassword());
        // Verificar que se asignó el rol
        assertTrue(usuarioEjemplo.getRoles().contains(rolCliente));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando las contraseñas no coinciden")
    void deberiaLanzarExcepcionCuandoContraseniasNoCoinciden() {
        // Given
        String password = "password123";
        String verificacionPassword = "password456";

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(usuarioEjemplo, password, verificacionPassword));

        assertEquals("Las contraseñas no coinciden", excepcion.getMessage());

        // Verificar que no se realizaron operaciones adicionales
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el username ya existe")
    void deberiaLanzarExcepcionCuandoUsernameYaExiste() {
        // Given
        String password = "password123";
        String verificacionPassword = "password123";

        when(usuarioRepository.existsByUsername(usuarioEjemplo.getUsername())).thenReturn(true);

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(usuarioEjemplo, password, verificacionPassword));

        assertEquals("El nombre de usuario ya está en uso", excepcion.getMessage());

        // Verificar que se verificó la existencia pero no se continuó
        verify(usuarioRepository, times(1)).existsByUsername(usuarioEjemplo.getUsername());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando no encuentra el rol CLIENTE")
    void deberiaLanzarExcepcionCuandoNoEncuentraRolCliente() {
        // Given
        String password = "password123";
        String verificacionPassword = "password123";

        when(usuarioRepository.existsByUsername(usuarioEjemplo.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encrypted_password");
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(usuarioEjemplo, password, verificacionPassword));

        assertEquals("Rol 'CLIENTE' no encontrado", excepcion.getMessage());

        // Verificar que se realizaron las verificaciones previas
        verify(usuarioRepository, times(1)).existsByUsername(usuarioEjemplo.getUsername());
        verify(passwordEncoder, times(1)).encode(password);
        verify(rolRepository, times(1)).findByNombre("CLIENTE");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería manejar contraseñas vacías correctamente")
    void deberiaManejarContraseniasVacias() {
        // Given
        String passwordVacio = "";
        String verificacionPasswordDiferente = "diferente";

        // When & Then
        IllegalArgumentException excepcion =
                assertThrows(IllegalArgumentException.class, () -> usuarioService
                        .registrar(usuarioEjemplo, passwordVacio, verificacionPasswordDiferente));

        assertEquals("Las contraseñas no coinciden", excepcion.getMessage());
    }

    @Test
    @DisplayName("Debería manejar contraseñas null correctamente")
    void deberiaManejarContraseniasNull() {
        // Given
        String passwordNull = null;
        String verificacionPasswordNull = null;

        // When & Then - Ambas null deberían lanzar excepción porque equals no maneja null
        IllegalArgumentException excepcion =
                assertThrows(IllegalArgumentException.class, () -> usuarioService
                        .registrar(usuarioEjemplo, passwordNull, verificacionPasswordNull));

        assertEquals("Las contraseñas no pueden ser null", excepcion.getMessage());
    }
}
