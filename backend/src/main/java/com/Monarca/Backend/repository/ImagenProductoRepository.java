package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImagenProductoRepository
        extends JpaRepository<ImagenProducto, Long> {
    java.util.List<ImagenProducto> findByProducto_IdProductoOrderByOrdenAscIdImagenAsc(Long id);
    java.util.List<ImagenProducto> findByProducto_IdProductoInAndPrincipalTrue(java.util.List<Long> ids);

    Optional<ImagenProducto>
    findFirstByProducto_IdProductoAndPrincipalTrue(
            Long idProducto
    );
}
