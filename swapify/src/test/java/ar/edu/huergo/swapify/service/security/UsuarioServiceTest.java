package ar.edu.huergo.swapify.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
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
import ar.edu.huergo.swapify.repository.publicacion.OfertaRepository;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import ar.edu.huergo.swapify.repository.security.NotificacionRepository;
import ar.edu.huergo.swapify.repository.security.RolRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import ar.edu.huergo.swapify.service.security.NotificacionService;

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

    @Mock
    private OfertaRepository ofertaRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEjemplo;
    private Usuario adminEjemplo;
    private Rol rolCliente;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1L);
        usuarioEjemplo.setUsername("usuario@test.com");
        usuarioEjemplo.setPassword("Password123");
        rolCliente = new Rol();
        rolCliente.setId(1L);
        rolCliente.setNombre("CLIENTE");
        usuarioEjemplo.getRoles().add(rolCliente);

        adminEjemplo = new Usuario();
        adminEjemplo.setId(2L);
        adminEjemplo.setUsername("admin@test.com");
        adminEjemplo.setPassword("Password123");
        rolAdmin = new Rol();
        rolAdmin.setId(2L);
        rolAdmin.setNombre("ADMIN");
        adminEjemplo.getRoles().add(rolAdmin);
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
        String password = "Password123";
        String verificacionPassword = "Password123";
        String passwordEncriptado = "encrypted_password";

        when(usuarioRepository.existsByUsernameIgnoreCase(usuarioEjemplo.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(passwordEncriptado);
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEjemplo);

        // When
        Usuario resultado =
                usuarioService.registrar(usuarioEjemplo, password, verificacionPassword);

        // Then
        assertNotNull(resultado);
        verify(usuarioRepository, times(1)).existsByUsernameIgnoreCase(usuarioEjemplo.getUsername());
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
        String password = "Password123";
        String verificacionPassword = "Password456";

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(usuarioEjemplo, password, verificacionPassword));

        assertEquals("Las contraseñas no coinciden", excepcion.getMessage());

        // Verificar que no se realizaron operaciones adicionales
        verify(usuarioRepository, never()).existsByUsernameIgnoreCase(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el username ya existe")
    void deberiaLanzarExcepcionCuandoUsernameYaExiste() {
        // Given
        String password = "Password123";
        String verificacionPassword = "Password123";

        when(usuarioRepository.existsByUsernameIgnoreCase(usuarioEjemplo.getUsername())).thenReturn(true);

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(usuarioEjemplo, password, verificacionPassword));

        assertEquals("El email ya está en uso", excepcion.getMessage());

        // Verificar que se verificó la existencia pero no se continuó
        verify(usuarioRepository, times(1)).existsByUsernameIgnoreCase(usuarioEjemplo.getUsername());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería crear el rol CLIENTE cuando no existe")
    void deberiaCrearRolClienteCuandoNoExiste() {
        // Given
        String password = "Password123";
        String verificacionPassword = "Password123";
        Rol nuevoRol = new Rol();
        nuevoRol.setId(3L);
        nuevoRol.setNombre("CLIENTE");

        when(usuarioRepository.existsByUsernameIgnoreCase(usuarioEjemplo.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encrypted_password");
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.empty());
        when(rolRepository.saveAndFlush(any(Rol.class))).thenReturn(nuevoRol);
        when(usuarioRepository.save(usuarioEjemplo)).thenReturn(usuarioEjemplo);

        // When
        Usuario resultado = usuarioService.registrar(usuarioEjemplo, password, verificacionPassword);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.getRoles().stream().anyMatch(rol -> "CLIENTE".equals(rol.getNombre())));
        verify(rolRepository, times(1)).findByNombre("CLIENTE");
        verify(rolRepository, times(1)).saveAndFlush(any(Rol.class));
        verify(usuarioRepository, times(1)).save(usuarioEjemplo);
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

    @Test
    @DisplayName("Debería suspender a un usuario cliente y persistir la sanción")
    void deberiaSuspenderUsuarioCliente() {
        // Given
        LocalDateTime hasta = LocalDateTime.now().plusDays(3);
        String motivo = "Incumplimiento de normas";
        when(usuarioRepository.findById(usuarioEjemplo.getId())).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioRepository.saveAndFlush(usuarioEjemplo)).thenReturn(usuarioEjemplo);

        // When
        Usuario resultado = usuarioService.banearUsuario(usuarioEjemplo.getId(), hasta, motivo);

        // Then
        assertNotNull(resultado);
        assertEquals(hasta, usuarioEjemplo.getBaneadoHasta());
        assertEquals(motivo, usuarioEjemplo.getMotivoBan());
        verify(usuarioRepository, times(1)).saveAndFlush(usuarioEjemplo);
        verify(notificacionService, times(1)).notificarBan(usuarioEjemplo, hasta, motivo);
    }

    @Test
    @DisplayName("No debería permitir suspender cuentas administrativas")
    void noDeberiaSuspenderCuentaAdministrativa() {
        // Given
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);
        when(usuarioRepository.findById(adminEjemplo.getId())).thenReturn(Optional.of(adminEjemplo));

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.banearUsuario(adminEjemplo.getId(), hasta, "Prueba"));

        assertEquals("No se pueden suspender cuentas administrativas", excepcion.getMessage());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).notificarBan(any(), any(), any());
    }

    @Test
    @DisplayName("Debería levantar la suspensión y limpiar el estado")
    void deberiaLevantarSuspension() {
        // Given
        usuarioEjemplo.setBaneadoHasta(LocalDateTime.now().plusDays(2));
        usuarioEjemplo.setMotivoBan("Motivo previo");
        when(usuarioRepository.findById(usuarioEjemplo.getId())).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioRepository.saveAndFlush(usuarioEjemplo)).thenReturn(usuarioEjemplo);

        // When
        Usuario resultado = usuarioService.levantarBan(usuarioEjemplo.getId());

        // Then
        assertNotNull(resultado);
        assertNull(usuarioEjemplo.getBaneadoHasta());
        assertNull(usuarioEjemplo.getMotivoBan());
        verify(usuarioRepository, times(1)).saveAndFlush(usuarioEjemplo);
        verify(notificacionService, times(1)).notificarBan(usuarioEjemplo, null, null);
    }

    @Test
    @DisplayName("No debería permitir eliminar cuentas administrativas")
    void noDeberiaEliminarCuentaAdministrativa() {
        // Given
        when(usuarioRepository.findById(adminEjemplo.getId())).thenReturn(Optional.of(adminEjemplo));

        // When & Then
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.eliminarUsuario(adminEjemplo.getId()));

        assertEquals("No se pueden eliminar cuentas administrativas desde este panel", excepcion.getMessage());
        verify(ofertaRepository, never()).deleteByUsuarioId(any());
        verify(usuarioRepository, never()).delete(any());
    }
}
