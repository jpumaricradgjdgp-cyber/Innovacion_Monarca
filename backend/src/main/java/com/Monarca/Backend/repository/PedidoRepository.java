package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {
    java.util.Optional<Pedido> findByCodigoPedido(String codigo);
    long countByUsuario_IdUsuarioAndEstado(Long usuario, String estado);
    org.springframework.data.domain.Page<Pedido> findByUsuario_IdUsuario(Long usuario, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.idPedido = :id")
    java.util.Optional<Pedido> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Pedido> findByUsuario_IdUsuario(
            Long idUsuario
    );
}
