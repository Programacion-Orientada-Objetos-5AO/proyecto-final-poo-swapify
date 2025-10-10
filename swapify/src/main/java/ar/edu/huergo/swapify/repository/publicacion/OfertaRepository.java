package ar.edu.huergo.swapify.repository.publicacion;

import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByPublicacionIdOrderByFechaOfertaDesc(Long publicacionId);

    Optional<Oferta> findByIdAndPublicacionId(Long id, Long publicacionId);

    List<Oferta> findByPublicacionIdAndIdNot(Long publicacionId, Long ofertaId);

    boolean existsByPublicacionIdAndUsuarioUsernameIgnoreCase(Long publicacionId, String username);

    Optional<Oferta> findFirstByPublicacionIdAndEstadoOrderByFechaRespuestaDesc(Long publicacionId, EstadoOferta estado);

    long countByEstado(EstadoOferta estado);

    void deleteByPublicacionId(Long publicacionId);
}
