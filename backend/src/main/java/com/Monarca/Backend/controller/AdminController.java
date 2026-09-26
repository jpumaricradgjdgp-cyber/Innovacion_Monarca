package com.Monarca.Backend.controller;
import com.Monarca.Backend.model.*;
import com.Monarca.Backend.repository.*;
import com.Monarca.Backend.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import jakarta.persistence.*;
import java.util.*;
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {
    @Autowired private ProductoRepository productos;
    @Autowired private VarianteProductoRepository variantes;
    @Autowired private ImagenProductoRepository imagenes;
    @Autowired private InventarioService inventario;
    @PersistenceContext private EntityManager em;
    public record Activacion(boolean activo) {}
    @GetMapping("/productos") @Transactional(readOnly=true)
    public List<Map<String,Object>> productos(@RequestParam(defaultValue="0") int pagina) {
        return productos.findAll(PageRequest.of(Math.max(0,pagina),20,Sort.by("idProducto").descending())).stream().map(p -> {
            Map<String,Object> r = new LinkedHashMap<>(); r.put("idProducto",p.getIdProducto()); r.put("nombre",p.getNombre()); r.put("categoria",p.getCategoria().getNombre());
            r.put("descripcion",p.getDescripcion());
            r.put("imagenes",imagenes.findByProducto_IdProductoOrderByOrdenAscIdImagenAsc(p.getIdProducto()).stream().map(i -> new com.Monarca.Backend.dto.ImagenDto(i.getUrl(),i.getTextoAlternativo())).toList());
            r.put("precioBase",p.getPrecioBase()); r.put("activo",p.getActivo());
            r.put("imagen",imagenes.findFirstByProducto_IdProductoAndPrincipalTrue(p.getIdProducto()).map(ImagenProducto::getUrl).orElse(null));
            r.put("variantes",variantes.findByProducto_IdProducto(p.getIdProducto()).stream().map(v -> Map.<String,Object>of(
                "idVariante",v.getIdVariante(),"sku",v.getSku(),"talla",v.getTalla(),"color",v.getColor(),"precio",v.getPrecio(),"stock",v.getStock(),"activo",v.getActivo())).toList());
            return r;
        }).toList();
    }
    @PostMapping("/productos/{id}/variantes") public void agregar(@PathVariable Long id,@RequestBody InventarioService.Variante datos) { inventario.agregar(id,datos); }
    @PatchMapping("/productos/{id}/activo") public void producto(@PathVariable Long id,@RequestBody Activacion datos) { inventario.activarProducto(id,datos.activo()); }
    @PatchMapping("/variantes/{id}/activo") public void variante(@PathVariable Long id,@RequestBody Activacion datos) { inventario.activarVariante(id,datos.activo()); }
    @PatchMapping("/variantes/{id}/stock") public void stock(@PathVariable Long id,@RequestBody InventarioService.Ajuste datos) { inventario.ajustar(id,datos); }
    @GetMapping("/reportes") @Transactional(readOnly=true)
    public Map<String,Object> reportes() {
        var estados = em.createQuery("select p.estado,count(p),sum(p.total) from Pedido p group by p.estado",Object[].class).getResultList();
        var categorias = em.createQuery("select d.variante.producto.categoria.nombre,sum(d.cantidad),sum(d.subtotal) from DetallePedido d where d.pedido.estado in ('PAGADO','ENVIADO','ENTREGADO') group by d.variante.producto.categoria.nombre",Object[].class).getResultList();
        var meses = em.createQuery("select year(p.fechaPedido),month(p.fechaPedido),count(distinct p.usuario.idUsuario),sum(p.total) from Pedido p where p.estado in ('PAGADO','ENVIADO','ENTREGADO') group by year(p.fechaPedido),month(p.fechaPedido) order by year(p.fechaPedido) desc,month(p.fechaPedido) desc",Object[].class).setMaxResults(12).getResultList();
        return Map.of("estados",estados.stream().map(x->Map.of("estado",x[0],"cantidad",x[1],"total",x[2])).toList(),
                "categorias",categorias.stream().map(x->Map.of("categoria",x[0],"unidades",x[1],"total",x[2])).toList(),
                "meses",meses.stream().map(x->Map.of("anio",x[0],"mes",x[1],"clientes",x[2],"total",x[3])).toList());
    }
}
