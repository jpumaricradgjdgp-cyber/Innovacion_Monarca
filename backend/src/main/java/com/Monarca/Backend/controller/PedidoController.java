package com.Monarca.Backend.controller;
import com.Monarca.Backend.dto.PedidoRequestDto;
import com.Monarca.Backend.repository.*;
import com.Monarca.Backend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.*;
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoService pedidoService;
    private Long usuario(Authentication a) { return usuarioRepository.findByCorreoIgnoreCase(a.getName()).orElseThrow().getIdUsuario(); }
    private boolean admin(Authentication a) { return a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN")); }
    private PageRequest pagina(int p) { return PageRequest.of(Math.max(0, p), 20, Sort.by("idPedido").descending()); }
    @GetMapping @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Map<String,Object>> listar(@RequestParam(defaultValue="0") int pagina) {
        return pedidoRepository.findAll(pagina(pagina)).stream().map(PedidoService::resumen).toList();
    }
    @GetMapping("/mios")
    public List<Map<String,Object>> mios(Authentication a, @RequestParam(defaultValue="0") int pagina) {
        return pedidoRepository.findByUsuario_IdUsuario(usuario(a), pagina(pagina)).stream().map(PedidoService::resumen).toList();
    }
    @GetMapping("/{id}")
    public Map<String,Object> detalle(@PathVariable Long id, Authentication a) { return pedidoService.detalle(id, usuario(a), admin(a)); }
    @PostMapping("/procesar")
    public Map<String,Object> crear(@RequestBody PedidoRequestDto dto, Authentication a) {
        dto.setIdUsuario(usuario(a)); var p = pedidoService.procesarCompra(dto);
        return Map.of("mensaje", "Pedido registrado", "idPedido", p.getIdPedido(), "codigoPedido", p.getCodigoPedido(), "estado", p.getEstado(), "total", p.getTotal());
    }
    public record Estado(String estado, String referencia) {}
    @PatchMapping("/{id}/estado")
    public Map<String,Object> estado(@PathVariable Long id, @RequestBody Estado d, Authentication a) {
        pedidoService.cambiarEstado(id, d.estado(), d.referencia(), usuario(a), admin(a)); return pedidoService.detalle(id, usuario(a), admin(a));
    }
}
