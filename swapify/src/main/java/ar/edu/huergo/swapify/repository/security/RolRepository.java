package ar.edu.huergo.swapify.repository.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.huergo.swapify.entity.security.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String nombre);
}


