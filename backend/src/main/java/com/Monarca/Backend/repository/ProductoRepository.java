package com.Monarca.Backend.repository;

import com.Monarca.Backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="categoria")
    org.springframework.data.domain.Page<Producto> findByActivoTrue(org.springframework.data.domain.Pageable pagina);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="categoria")
    org.springframework.data.domain.Page<Producto> findByActivoTrueAndCategoria_NombreIgnoreCase(String categoria, org.springframework.data.domain.Pageable pagina);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdProductoNot(
            String slug,
            Long idProducto
    );
}
