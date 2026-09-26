package com.Monarca.Backend.service;
import com.Monarca.Backend.dto.*;
import com.Monarca.Backend.model.*;
import com.Monarca.Backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PedidoService {
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private DetallePedidoRepository detallePedidoRepository;
    @Autowired private VarianteProductoRepository varianteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MetodoPagoRepository metodoPagoRepository;
    @Autowired private PagoRepository pagoRepository;
    @Transactional
    public Pedido procesarCompra(PedidoRequestDto dto) {
        if (dto == null || dto.getIdUsuario() == null || dto.getIdMetodoPago() == null || dto.getEntrega() == null)
            throw new IllegalArgumentException("Usuario, método de pago y entrega requeridos");
        String entrega = dto.getEntrega().validarYSerializar();
        String codigo;
        try { codigo = "MON-" + UUID.fromString(dto.getClaveOperacion()).toString(); }
        catch (Exception e) { throw new IllegalArgumentException("La operación necesita una clave UUID válida"); }
        SortedMap<Long,Integer> cantidades = cantidades(dto.getItems());
        // El bloqueo de usuario serializa reintentos. codigo_pedido ya es único en la BD.
        Usuario usuario = usuarioRepository.findByIdForUpdate(dto.getIdUsuario()).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no disponible"));
        Optional<Pedido> previo = pedidoRepository.findByCodigoPedido(codigo);
        if (previo.isPresent()) {
            Pedido p = previo.get();
            if (!p.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) throw conflicto("Operación no disponible");
            SortedMap<Long,Integer> anteriores = new TreeMap<>();
            detallePedidoRepository.findByPedido_IdPedido(p.getIdPedido()).forEach(d -> anteriores.merge(d.getVariante().getIdVariante(), d.getCantidad(), Math::addExact));
            List<Pago> pagos = pagoRepository.findByPedido_IdPedido(p.getIdPedido());
            if (!anteriores.equals(cantidades) || !Objects.equals(p.getObservacion(), entrega) || pagos.size() != 1
                    || !pagos.getFirst().getMetodoPago().getIdMetodoPago().equals(dto.getIdMetodoPago()))
                throw conflicto("Esta operación ya se utilizó con otros datos. Consulta Mis pedidos.");
            return p;
        }
        MetodoPago metodo = metodoPagoRepository.findById(dto.getIdMetodoPago()).filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no disponible"));
        if (pedidoRepository.countByUsuario_IdUsuarioAndEstado(usuario.getIdUsuario(), "PENDIENTE_PAGO") >= 5)
            throw conflicto("Tienes cinco pedidos pendientes. Paga o cancela uno desde Mis pedidos.");
        List<DetallePedido> lineas = new ArrayList<>(); BigDecimal total = BigDecimal.ZERO;
        for (var item : cantidades.entrySet()) {
            VarianteProducto v = varianteRepository.findByIdForUpdate(item.getKey()).orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));
            if (!Boolean.TRUE.equals(v.getActivo()) || !Boolean.TRUE.equals(v.getProducto().getActivo())) throw conflicto("El producto o variante ya no está disponible");
            if (v.getStock() < item.getValue()) throw conflicto("Stock insuficiente para " + v.getSku());
            BigDecimal subtotal = v.getPrecio().multiply(BigDecimal.valueOf(item.getValue())); total = total.add(subtotal);
            if (total.compareTo(new BigDecimal("99999999.99")) > 0) throw new IllegalArgumentException("El total supera el límite admitido");
            v.setStock(v.getStock() - item.getValue()); varianteRepository.save(v);
            DetallePedido d = new DetallePedido(); d.setVariante(v); d.setCantidad(item.getValue()); d.setPrecioUnitario(v.getPrecio()); d.setSubtotal(subtotal);
            d.setNombreProducto(v.getProducto().getNombre()); d.setSku(v.getSku()); d.setTalla(v.getTalla()); d.setColor(v.getColor()); lineas.add(d);
        }
        Pedido pedido = new Pedido(); pedido.setUsuario(usuario); pedido.setCodigoPedido(codigo); pedido.setObservacion(entrega); pedido.setEstado("PENDIENTE_PAGO");
        pedido.setSubtotal(total); pedido.setDescuento(BigDecimal.ZERO); pedido.setCostoEnvio(BigDecimal.ZERO); pedido.setTotal(total);
        pedido = pedidoRepository.save(pedido);
        for (DetallePedido d : lineas) { d.setPedido(pedido); detallePedidoRepository.save(d); }
        Pago pago = new Pago(); pago.setPedido(pedido); pago.setMetodoPago(metodo); pago.setMonto(total); pago.setEstado("PENDIENTE"); pagoRepository.save(pago);
        return pedido;
    }
    private SortedMap<Long,Integer> cantidades(List<ItemCarritoDto> items) {
        if (items == null || items.isEmpty() || items.size() > 100) throw new IllegalArgumentException("Carrito requerido, máximo 100 líneas");
        SortedMap<Long,Integer> resultado = new TreeMap<>();
        for (ItemCarritoDto i : items) {
            if (i == null || i.getIdVariante() == null || i.getIdVariante() <= 0 || i.getCantidad() == null || i.getCantidad() < 1 || i.getCantidad() > 1000)
                throw new IllegalArgumentException("Variante y cantidad válidas requeridas (1 a 1000)");
            resultado.merge(i.getIdVariante(), i.getCantidad(), Integer::sum);
            if (resultado.get(i.getIdVariante()) > 1000) throw new IllegalArgumentException("Máximo 1000 unidades por variante");
        }
        return resultado;
    }
    @Transactional
    public Pedido cambiarEstado(Long id, String destino, String referencia, Long usuario, boolean admin) {
        Pedido p = pedidoRepository.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        autorizar(p, usuario, admin);
        if (!admin && !"CANCELADO".equals(destino)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acción exclusiva del administrador");
        if (Objects.equals(p.getEstado(), destino)) return p;
        String actual = p.getEstado();
        boolean permitido = switch (actual) {
            case "PENDIENTE_PAGO" -> "CANCELADO".equals(destino) || (admin && "PAGADO".equals(destino));
            case "PAGADO" -> admin && "ENVIADO".equals(destino);
            case "ENVIADO" -> admin && "ENTREGADO".equals(destino);
            default -> false;
        };
        if (!permitido) throw conflicto("No se puede pasar de " + actual + " a " + destino);
        List<Pago> pagos = pagoRepository.findByPedido_IdPedido(id);
        if ("PAGADO".equals(destino)) {
            if (referencia == null || referencia.isBlank() || referencia.length() > 100) throw new IllegalArgumentException("Indica la referencia del pago verificado (máximo 100 caracteres)");
            if (pagos.size() != 1) throw conflicto("Revisa los registros de pago de este pedido");
            Pago pago = pagos.getFirst(); pago.setEstado("PAGADO"); pago.setNumeroOperacion(referencia.trim()); pago.setProveedorPago("VERIFICACION_MANUAL");
            pago.setFechaPago(OffsetDateTime.now()); pago.setFechaConfirmacion(OffsetDateTime.now()); pago.setObservacion("Confirmado por usuario " + usuario); pagoRepository.save(pago);
        }
        if ("CANCELADO".equals(destino)) {
            List<DetallePedido> lineas = new ArrayList<>(detallePedidoRepository.findByPedido_IdPedido(id));
            lineas.sort(Comparator.comparing(d -> d.getVariante().getIdVariante()));
            for (DetallePedido d : lineas) {
                VarianteProducto v = varianteRepository.findByIdForUpdate(d.getVariante().getIdVariante()).orElseThrow();
                v.setStock(Math.addExact(v.getStock(), d.getCantidad())); varianteRepository.save(v);
            }
            for (Pago pago : pagos) { pago.setEstado("CANCELADO"); pagoRepository.save(pago); }
        }
        p.setEstado(destino); return pedidoRepository.save(p);
    }
    @Transactional(readOnly = true)
    public Map<String,Object> detalle(Long id, Long usuario, boolean admin) {
        Pedido p = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        autorizar(p, usuario, admin); Map<String,Object> r = new LinkedHashMap<>(resumen(p)); r.put("entrega", p.getObservacion());
        r.put("items", detallePedidoRepository.findByPedido_IdPedido(id).stream().map(d -> {
            Map<String,Object> l = new LinkedHashMap<>(); l.put("nombre", d.getNombreProducto()); l.put("sku", d.getSku()); l.put("talla", d.getTalla());
            l.put("color", d.getColor()); l.put("cantidad", d.getCantidad()); l.put("precio", d.getPrecioUnitario()); l.put("subtotal", d.getSubtotal()); return l;
        }).toList());
        r.put("pagos", pagoRepository.findByPedido_IdPedido(id).stream().map(pago -> {
            Map<String,Object> v = new LinkedHashMap<>(); v.put("metodo", pago.getMetodoPago().getNombre()); v.put("estado", pago.getEstado());
            v.put("referencia", pago.getNumeroOperacion()); v.put("monto", pago.getMonto()); return v;
        }).toList()); return r;
    }
    private void autorizar(Pedido p, Long usuario, boolean admin) {
        if (!admin && !p.getUsuario().getIdUsuario().equals(usuario)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
    }
    public static Map<String,Object> resumen(Pedido p) {
        Map<String,Object> r = new LinkedHashMap<>(); r.put("idPedido", p.getIdPedido()); r.put("codigoPedido", p.getCodigoPedido()); r.put("estado", p.getEstado());
        r.put("subtotal", p.getSubtotal()); r.put("total", p.getTotal()); r.put("cliente", p.getUsuario().getCorreo()); r.put("fechaPedido", p.getFechaPedido()); return r;
    }
    private ResponseStatusException conflicto(String texto) { return new ResponseStatusException(HttpStatus.CONFLICT, texto); }
}
