package ar.edu.huergo.swapify.repository.publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.edu.huergo.swapify.entity.publicacion.Publicacion;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    List<Publicacion> findByFechaPublicacionBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("""
           select coalesce(sum(p.precio), 0)
           from Publicacion p
           where p.fechaPublicacion between :inicio and :fin
           """)
    BigDecimal sumaPreciosEntre(@Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin);

    List<Publicacion> findByUsuarioId(Long usuarioId);

    List<Publicacion> findByUsuarioUsernameOrderByFechaPublicacionDesc(String username);

    List<Publicacion> findAllByOrderByFechaPublicacionDesc();
}
