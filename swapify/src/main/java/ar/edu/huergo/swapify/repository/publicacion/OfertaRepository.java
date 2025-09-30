package ar.edu.huergo.swapify.repository.publicacion;

import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByPublicacionIdOrderByFechaOfertaDesc(Long publicacionId);
}
