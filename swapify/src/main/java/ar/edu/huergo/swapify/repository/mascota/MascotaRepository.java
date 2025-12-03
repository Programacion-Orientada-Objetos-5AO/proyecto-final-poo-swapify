package ar.edu.huergo.swapify.repository.mascota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.huergo.swapify.entity.mascota.Mascota;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {
}
