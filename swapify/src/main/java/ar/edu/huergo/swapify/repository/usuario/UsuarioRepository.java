package ar.edu.huergo.swapify.repository.usuario;

import ar.edu.huergo.swapify.entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByMailIgnoreCase(String mail);
    Optional<Usuario> findByMailIgnoreCase(String mail);
}
