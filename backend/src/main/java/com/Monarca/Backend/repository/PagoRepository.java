package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository
        extends JpaRepository<Pago, Long> {
    java.util.List<Pago> findByPedido_IdPedido(Long id);
}
