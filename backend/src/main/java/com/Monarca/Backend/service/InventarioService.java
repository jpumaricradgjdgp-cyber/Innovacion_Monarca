package com.Monarca.Backend.service;
import com.Monarca.Backend.model.*;
import com.Monarca.Backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
@Service
public class InventarioService {
    @PersistenceContext private EntityManager em;
    @Autowired private VarianteProductoRepository variantes;
    @Autowired private ProductoRepository productos;
    public record Variante(String talla, String color, BigDecimal precio, Integer stock) {}
    public record Ajuste(Integer cambio, Integer stockAnterior) {}
    @Transactional
    public void agregar(Long id, Variante datos) {
        ValidacionTalla.comprobar(datos.talla());
        Producto p = em.find(Producto.class,id,LockModeType.PESSIMISTIC_WRITE);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado");
        if (datos.talla()==null || datos.talla().isBlank() || datos.talla().length()>20 || datos.color()==null || datos.color().isBlank() || datos.color().length()>50
                || datos.precio()==null || datos.precio().signum()<0 || datos.precio().compareTo(new BigDecimal("99999999.99"))>0 || datos.precio().stripTrailingZeros().scale()>2
                || datos.stock()==null || datos.stock()<0 || datos.stock()>1000000) throw new IllegalArgumentException("Revisa talla, color, precio y stock");
        if (variantes.findByProducto_IdProducto(id).stream().anyMatch(v -> v.getTalla().equalsIgnoreCase(datos.talla().trim()) && v.getColor().equalsIgnoreCase(datos.color().trim())))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La combinación ya existe; edítala o reactívala");
        VarianteProducto v = new VarianteProducto(); v.setProducto(p); v.setSku("MON-"+UUID.randomUUID()); v.setTalla(datos.talla().trim()); v.setColor(datos.color().trim());
        v.setPrecio(datos.precio()); v.setStock(datos.stock()); variantes.save(v);
    }
    @Transactional
    public void ajustar(Long id, Ajuste ajuste) {
        if (ajuste.cambio()==null || ajuste.cambio()==0 || Math.abs((long)ajuste.cambio())>1000000 || ajuste.stockAnterior()==null)
            throw new IllegalArgumentException("Indica un ajuste distinto de cero y el stock que viste");
        VarianteProducto v = variantes.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Variante no encontrada"));
        if (!v.getStock().equals(ajuste.stockAnterior())) throw new ResponseStatusException(HttpStatus.CONFLICT,"El stock cambió. Recarga antes de ajustar");
        long nuevo = (long)v.getStock()+ajuste.cambio();
        if (nuevo<0 || nuevo>1000000) throw new IllegalArgumentException("El resultado debe estar entre 0 y 1000000 unidades");
        v.setStock((int)nuevo); variantes.save(v);
    }
    @Transactional
    public void activarVariante(Long id, boolean activo) {
        VarianteProducto v = variantes.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Variante no encontrada"));
        v.setActivo(activo); variantes.save(v);
    }
    @Transactional
    public void activarProducto(Long id, boolean activo) {
        Producto p = em.find(Producto.class,id,LockModeType.PESSIMISTIC_WRITE);
        if (p==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado");
        p.setActivo(activo); productos.save(p);
    }
}
