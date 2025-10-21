package ar.edu.huergo.swapify.repository.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;

@DataJpaTest
public class PublicacionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Test
    public void testFindByFechaPublicacionBetween() {
        // Given
        LocalDateTime fecha1 = LocalDateTime.of(2023, 1, 1, 12, 0);
        LocalDateTime fecha2 = LocalDateTime.of(2023, 1, 2, 12, 0);
        LocalDateTime fecha3 = LocalDateTime.of(2023, 1, 3, 12, 0);

        Usuario usuario = new Usuario("test@example.com", "password");
        entityManager.persistAndFlush(usuario);

        Publicacion pub1 = new Publicacion(null, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                fecha1, usuario, List.of(), null, null);
        Publicacion pub2 = new Publicacion(null, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2",
                fecha2, usuario, List.of(), null, null);
        Publicacion pub3 = new Publicacion(null, "Libro3", new BigDecimal("300.00"), "Desc3", "Obj3",
                fecha3, usuario, List.of(), null, null);

        entityManager.persistAndFlush(pub1);
        entityManager.persistAndFlush(pub2);
        entityManager.persistAndFlush(pub3);

        // When
        List<Publicacion> result = publicacionRepository.findByFechaPublicacionBetween(
            LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 2, 23, 59));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Publicacion::getNombre).contains("Libro1", "Libro2");
        assertThat(result).allSatisfy(pub -> assertThat(pub.getArticulo()).isNotNull());
    }

    @Test
    public void testSumaPreciosEntre() {
        // Given
        LocalDateTime fecha1 = LocalDateTime.of(2023, 1, 1, 12, 0);
        LocalDateTime fecha2 = LocalDateTime.of(2023, 1, 2, 12, 0);
        LocalDateTime fecha3 = LocalDateTime.of(2023, 1, 3, 12, 0);

        Usuario usuario = new Usuario("test@example.com", "password");
        entityManager.persistAndFlush(usuario);

        Publicacion pub1 = new Publicacion(null, "Libro1", new BigDecimal("100.00"), "Desc1", "Obj1",
                fecha1, usuario, List.of(), null, null);
        Publicacion pub2 = new Publicacion(null, "Libro2", new BigDecimal("200.00"), "Desc2", "Obj2",
                fecha2, usuario, List.of(), null, null);
        Publicacion pub3 = new Publicacion(null, "Libro3", new BigDecimal("300.00"), "Desc3", "Obj3",
                fecha3, usuario, List.of(), null, null);

        entityManager.persistAndFlush(pub1);
        entityManager.persistAndFlush(pub2);
        entityManager.persistAndFlush(pub3);

        // When
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(
            LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 2, 23, 59));

        // Then
        assertThat(suma).isEqualTo(new BigDecimal("300.00"));
    }

    @Test
    public void testSumaPreciosEntre_NoResults() {
        // When
        BigDecimal suma = publicacionRepository.sumaPreciosEntre(
            LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 2, 23, 59));

        // Then
        assertThat(suma).isEqualTo(BigDecimal.ZERO);
    }
}
