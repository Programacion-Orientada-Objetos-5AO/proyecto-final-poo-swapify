package ar.edu.huergo.swapify.repository.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.huergo.swapify.entity.producto.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
