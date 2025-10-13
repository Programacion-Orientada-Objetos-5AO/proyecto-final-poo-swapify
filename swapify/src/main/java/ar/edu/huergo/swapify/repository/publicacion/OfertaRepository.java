package ar.edu.huergo.swapify.repository.publicacion;

import ar.edu.huergo.swapify.entity.publicacion.EstadoOferta;
import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByPublicacionIdOrderByFechaOfertaDesc(Long publicacionId);

    boolean existsByPublicacionIdAndUsuarioUsernameIgnoreCase(Long publicacionId, String username);

    Optional<Oferta> findByIdAndPublicacionId(Long ofertaId, Long publicacionId);

    List<Oferta> findByPublicacionIdAndIdNot(Long publicacionId, Long ofertaId);

    Optional<Oferta> findFirstByPublicacionIdAndEstadoOrderByFechaRespuestaDesc(Long publicacionId, EstadoOferta estado);

    long countByEstado(EstadoOferta estado);

    long count();

    void deleteByPublicacionId(Long publicacionId);

    void deleteByUsuarioId(Long usuarioId);
}
