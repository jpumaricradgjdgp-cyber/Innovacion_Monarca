package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Rol r where r.nombre = :nombre")
    Optional<Rol> findByNombreForUpdate(@org.springframework.data.repository.query.Param("nombre") String nombre);

    Optional<Rol> findByNombre(String nombre);
}
