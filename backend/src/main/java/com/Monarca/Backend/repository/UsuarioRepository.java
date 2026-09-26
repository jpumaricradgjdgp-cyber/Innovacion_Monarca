package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.idUsuario = :id")
    Optional<Usuario> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
        UPDATE Usuario u SET u.password = :nueva, u.fechaActualizacion = :ahora
        WHERE u.idUsuario = :id AND u.password = :anterior AND u.activo = true
        """)
    int restablecerPassword(@org.springframework.data.repository.query.Param("id") Long id,
                           @org.springframework.data.repository.query.Param("anterior") String anterior,
                           @org.springframework.data.repository.query.Param("nueva") String nueva,
                           @org.springframework.data.repository.query.Param("ahora") java.time.OffsetDateTime ahora);
}
