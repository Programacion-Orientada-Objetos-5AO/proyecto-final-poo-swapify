package ar.edu.huergo.swapify.repository.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ar.edu.huergo.swapify.entity.usuario.Usuario;

@DataJpaTest
public class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    public void testExistsByMailIgnoreCase() {
        // Given
        Usuario usuario = new Usuario(1L, "Juan", "juan@example.com", "123456789", "password123");
        entityManager.persistAndFlush(usuario);

        // When & Then
        assertThat(usuarioRepository.existsByMailIgnoreCase("JUAN@EXAMPLE.COM")).isTrue();
        assertThat(usuarioRepository.existsByMailIgnoreCase("juan@example.com")).isTrue();
        assertThat(usuarioRepository.existsByMailIgnoreCase("otro@example.com")).isFalse();
    }

    @Test
    public void testFindByMailIgnoreCase() {
        // Given
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", "987654321", "pass456");
        entityManager.persistAndFlush(usuario);

        // When
        Optional<Usuario> found = usuarioRepository.findByMailIgnoreCase("ANA@EXAMPLE.COM");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Ana");
        assertThat(found.get().getMail()).isEqualTo("ana@example.com");

        // Test case insensitive
        Optional<Usuario> foundLower = usuarioRepository.findByMailIgnoreCase("ana@example.com");
        assertThat(foundLower).isPresent();

        // Test not found
        Optional<Usuario> notFound = usuarioRepository.findByMailIgnoreCase("inexistente@example.com");
        assertThat(notFound).isEmpty();
    }
}
