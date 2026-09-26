package com.Monarca.Backend.service;
import com.Monarca.Backend.dto.*;
import com.Monarca.Backend.model.*;
import com.Monarca.Backend.repository.*;
import com.Monarca.Backend.controller.AdminController;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class SistemaIntegracionTest {
    @Autowired PedidoService compras; @Autowired InventarioService inventario; @Autowired ProductoService productosService;
    @Autowired PedidoRepository pedidos; @Autowired PagoRepository pagos; @Autowired DetallePedidoRepository detalles;
    @Autowired VarianteProductoRepository variantes; @Autowired ProductoRepository productos; @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles; @Autowired CategoriaRepository categorias; @Autowired MetodoPagoRepository metodos;
    @Autowired AdminController admin;
    @Autowired com.Monarca.Backend.controller.AuthController registro;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.Monarca.Backend.security.JwtUtil jwt;
    @Autowired com.Monarca.Backend.security.CustomUserDetailsService userDetails;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    Usuario cliente, otro; VarianteProducto variante; MetodoPago metodo; Producto producto;
    @Autowired ImagenProductoRepository imagenes;
    @BeforeEach void datos() {
        imagenes.deleteAll();
        pagos.deleteAll();detalles.deleteAll();pedidos.deleteAll();variantes.deleteAll();productos.deleteAll();usuarios.deleteAll();roles.deleteAll();categorias.deleteAll();metodos.deleteAll();
        Rol rol=new Rol();rol.setNombre("CLIENTE");rol=roles.save(rol);
        cliente=usuario("ana@example.test",rol);otro=usuario("otro@example.test",rol);
        Categoria c=new Categoria();c.setNombre("Tops");c.setSlug("tops");c=categorias.save(c);
        producto=new Producto();producto.setNombre("Top");producto.setSlug("top");producto.setCategoria(c);producto.setPrecioBase(new BigDecimal("25.00"));producto=productos.save(producto);
        variante=new VarianteProducto();variante.setProducto(producto);variante.setSku("TOP-S-N");variante.setTalla("S");variante.setColor("Negro");variante.setPrecio(new BigDecimal("25.00"));variante.setStock(5);variante=variantes.save(variante);
        metodo=new MetodoPago();metodo.setCodigo("MANUAL");metodo.setNombre("Pago manual");metodo=metodos.save(metodo);
    }
    @AfterEach void limpiarSeguridad(){SecurityContextHolder.clearContext();}
    String iniciarSesionReal(Usuario usuario) throws Exception {
        usuario.setPassword(encoder.encode("ClaveParaPruebas123!"));usuarios.save(usuario);
        var respuesta=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType("application/json").content(json.writeValueAsString(Map.of("email",usuario.getCorreo(),"password","ClaveParaPruebas123!"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        return "Bearer "+json.readTree(respuesta.getResponse().getContentAsString()).get("token").asText();
    }
    long comprarPorApi(String token, PedidoRequestDto solicitud) throws Exception {
        var respuesta=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pedidos/procesar")
                .header("Authorization",token).contentType("application/json").content(json.writeValueAsString(solicitud)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).get("idPedido").asLong();
    }
    void estadoPorApi(String token,long id,String estado,String referencia,int esperado) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/pedidos/"+id+"/estado")
                .header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("estado",estado,"referencia",referencia))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(esperado));
    }
    @Test void compraCompletaConLoginRealPagoEnvioYEntrega() throws Exception {
        String tokenCliente=iniciarSesionReal(cliente);
        Rol rolAdmin=new Rol();rolAdmin.setNombre("ADMIN");rolAdmin=roles.save(rolAdmin);
        Usuario administrador=usuario("admin@example.test",rolAdmin);String tokenAdmin=iniciarSesionReal(administrador);
        var solicitud=solicitud(otro,2); // La API debe ignorar la identidad enviada y usar el JWT.
        long id=comprarPorApi(tokenCliente,solicitud);
        assertEquals(cliente.getIdUsuario(),pedidos.findById(id).orElseThrow().getUsuario().getIdUsuario());
        assertEquals(3,stock());assertEquals(id,comprarPorApi(tokenCliente,solicitud));assertEquals(3,stock());assertEquals(1,pedidos.count());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pedidos/mios").header("Authorization",tokenCliente))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].idPedido").value(id));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pedidos").header("Authorization",tokenAdmin))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        estadoPorApi(tokenCliente,id,"PAGADO","OP-TEST",403);
        estadoPorApi(tokenAdmin,id,"ENVIADO","",409);
        estadoPorApi(tokenAdmin,id,"PAGADO","",400);
        estadoPorApi(tokenAdmin,id,"PAGADO","OP-TEST",200);
        assertEquals("PAGADO",pagos.findByPedido_IdPedido(id).getFirst().getEstado());
        assertEquals("OP-TEST",pagos.findByPedido_IdPedido(id).getFirst().getNumeroOperacion());
        estadoPorApi(tokenCliente,id,"CANCELADO","",409);
        estadoPorApi(tokenAdmin,id,"ENVIADO","",200);
        estadoPorApi(tokenAdmin,id,"ENTREGADO","",200);
        estadoPorApi(tokenAdmin,id,"ENTREGADO","",200);
        assertEquals(3,stock());assertEquals("ENTREGADO",pedidos.findById(id).orElseThrow().getEstado());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/reportes").header("Authorization",tokenAdmin))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.meses[0].total").value(50));
    }
    @Test void cancelacionPorApiDevuelveStockUnaVezYProtegePedidosAjenos() throws Exception {
        String token=iniciarSesionReal(cliente),ajeno=iniciarSesionReal(otro);
        long id=comprarPorApi(token,solicitud(cliente,2));assertEquals(3,stock());
        estadoPorApi(ajeno,id,"CANCELADO","",404);assertEquals(3,stock());
        estadoPorApi(token,id,"CANCELADO","",200);assertEquals(5,stock());
        estadoPorApi(token,id,"CANCELADO","",200);assertEquals(5,stock());
        assertEquals("CANCELADO",pagos.findByPedido_IdPedido(id).getFirst().getEstado());
    }
    @Test void corrigeTallaAntiguaSinDuplicarExistencias() {
        variante.setTalla("S-M-L");variantes.save(variante);
        ProductoDto cambio=new ProductoDto();cambio.setIdVariante(variante.getIdVariante());cambio.setTalla("S");
        productosService.actualizar(producto.getIdProducto(),cambio);
        assertEquals("S",variantes.findById(variante.getIdVariante()).orElseThrow().getTalla());
        assertEquals(5,stock());assertEquals(1,variantes.count());
        assertThrows(IllegalArgumentException.class,()->inventario.agregar(producto.getIdProducto(),new InventarioService.Variante("S-M-L","Negro",new BigDecimal("25.00"),5)));
        cambio.setTalla("S, M, L");assertThrows(IllegalArgumentException.class,()->productosService.actualizar(producto.getIdProducto(),cambio));
        assertEquals("S",variantes.findById(variante.getIdVariante()).orElseThrow().getTalla());
        inventario.agregar(producto.getIdProducto(),new InventarioService.Variante("M","Negro",new BigDecimal("25.00"),2));
        assertEquals(2,variantes.count());assertEquals(5,stock());
    }
    @Test void descripcionYGaleriaSeGuardanConPortadaYOrden() {
        ProductoDto d=new ProductoDto();d.setIdVariante(variante.getIdVariante());
        d.setDescripcion("Algodón suave\nLavado a mano");
        d.setImagenes(List.of(new ImagenDto("https://example.test/frontal.jpg","Frontal"),new ImagenDto("https://example.test/posterior.jpg","Posterior")));
        productosService.actualizar(producto.getIdProducto(),d);
        var detalle=productosService.buscarCatalogoPorId(producto.getIdProducto()).orElseThrow();
        assertEquals(d.getDescripcion(),detalle.getDescripcion());assertEquals(2,detalle.getImagenes().size());
        assertEquals("Posterior",detalle.getImagenes().get(1).textoAlternativo());assertEquals(d.getImagenes().get(0).url(),detalle.getImagen());
        d.setImagenes(null);d.setDescripcion("Nueva descripción");productosService.actualizar(producto.getIdProducto(),d);
        assertEquals(2,imagenes.count());
        d.setImagenes(List.of());productosService.actualizar(producto.getIdProducto(),d);assertEquals(0,imagenes.count());
    }
    @Test void galeriaInvalidaRevierteLaEdicion() {
        ProductoDto d=new ProductoDto();d.setIdVariante(variante.getIdVariante());d.setNombre("No guardar");
        d.setImagenes(List.of(new ImagenDto("javascript:alert(1)","Inválida")));
        assertThrows(IllegalArgumentException.class,()->productosService.actualizar(producto.getIdProducto(),d));
        assertEquals("Top",productos.findById(producto.getIdProducto()).orElseThrow().getNombre());
    }
    @Test void clienteNoPuedeSubirFotos() throws Exception {
        String token=jwt.generateToken(userDetails.loadUserByUsername(cliente.getCorreo()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/admin/imagenes")
                .file("archivo",new byte[]{1,2,3}).header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
    Usuario usuario(String email,Rol rol){Usuario u=new Usuario();u.setCorreo(email);u.setNombres("Ana");u.setApellidos("Prueba");u.setPassword("hash-de-prueba");u.setRol(rol);return usuarios.save(u);}
    ItemCarritoDto item(Long id,int cantidad){ItemCarritoDto i=new ItemCarritoDto();i.setIdVariante(id);i.setCantidad(cantidad);return i;}
    PedidoRequestDto solicitud(Usuario u,int cantidad){PedidoRequestDto d=new PedidoRequestDto();d.setIdUsuario(u.getIdUsuario());d.setIdMetodoPago(metodo.getIdMetodoPago());d.setClaveOperacion(UUID.randomUUID().toString());
        d.setEntrega(new EntregaDto("Ana","Prueba",u.getCorreo(),"999999999","12345678","Calle 123","Surco",""));d.setItems(List.of(item(variante.getIdVariante(),cantidad)));return d;}
    int stock(){return variantes.findById(variante.getIdVariante()).orElseThrow().getStock();}
    @Test void reintentoDevuelveMismoPedidoSinOtroDescuento(){var d=solicitud(cliente,2);var primero=compras.procesarCompra(d);var segundo=compras.procesarCompra(d);assertEquals(primero.getIdPedido(),segundo.getIdPedido());assertEquals(1,pedidos.count());assertEquals(3,stock());}
    @Test void mismaClaveConOtroContenidoSeRechaza(){var d=solicitud(cliente,2);compras.procesarCompra(d);d.setItems(List.of(item(variante.getIdVariante(),1)));assertThrows(ResponseStatusException.class,()->compras.procesarCompra(d));assertEquals(3,stock());}
    @Test void rollbackDevuelveStockSiFallaOtraLinea(){var d=solicitud(cliente,2);d.setItems(List.of(item(variante.getIdVariante(),2),item(Long.MAX_VALUE,1)));assertThrows(IllegalArgumentException.class,()->compras.procesarCompra(d));assertEquals(5,stock());assertEquals(0,pedidos.count());assertEquals(0,pagos.count());}
    @Test void cancelarDosVecesDevuelveStockSoloUnaVez(){var p=compras.procesarCompra(solicitud(cliente,2));compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,cliente.getIdUsuario(),false);compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,cliente.getIdUsuario(),false);assertEquals(5,stock());assertEquals("CANCELADO",pagos.findByPedido_IdPedido(p.getIdPedido()).getFirst().getEstado());}
    @Test void soloPropietarioOAdminVenDetalle(){var p=compras.procesarCompra(solicitud(cliente,1));assertThrows(ResponseStatusException.class,()->compras.detalle(p.getIdPedido(),otro.getIdUsuario(),false));assertNotNull(compras.detalle(p.getIdPedido(),cliente.getIdUsuario(),false).get("entrega"));assertThrows(ResponseStatusException.class,()->compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,otro.getIdUsuario(),false));}
    @Test void pagoYEnvioRespetanPermisosYEstados(){var p=compras.procesarCompra(solicitud(cliente,1));assertThrows(ResponseStatusException.class,()->compras.cambiarEstado(p.getIdPedido(),"PAGADO","OP",cliente.getIdUsuario(),false));
        assertThrows(ResponseStatusException.class,()->compras.cambiarEstado(p.getIdPedido(),"ENTREGADO",null,otro.getIdUsuario(),true));
        compras.cambiarEstado(p.getIdPedido(),"PAGADO","OP-123",otro.getIdUsuario(),true);assertThrows(ResponseStatusException.class,()->compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,cliente.getIdUsuario(),false));
        compras.cambiarEstado(p.getIdPedido(),"ENVIADO",null,otro.getIdUsuario(),true);compras.cambiarEstado(p.getIdPedido(),"ENTREGADO",null,otro.getIdUsuario(),true);assertEquals("PAGADO",pagos.findByPedido_IdPedido(p.getIdPedido()).getFirst().getEstado());assertEquals(4,stock());}
    @Test void ajusteViejoNoSobrescribeCompra(){compras.procesarCompra(solicitud(cliente,2));assertThrows(ResponseStatusException.class,()->inventario.ajustar(variante.getIdVariante(),new InventarioService.Ajuste(2,5)));assertEquals(3,stock());inventario.ajustar(variante.getIdVariante(),new InventarioService.Ajuste(2,3));assertEquals(5,stock());}
    @Test void editorNoPuedeCambiarStock(){ProductoDto d=new ProductoDto();d.setStock(100);assertThrows(IllegalArgumentException.class,()->productosService.actualizar(producto.getIdProducto(),d));assertEquals(5,stock());}
    @Test void nuevaVarianteYDuplicadoNormalizado(){inventario.agregar(producto.getIdProducto(),new InventarioService.Variante("M","Blanco",new BigDecimal("27.00"),3));assertThrows(ResponseStatusException.class,()->inventario.agregar(producto.getIdProducto(),new InventarioService.Variante(" m ","blanco",new BigDecimal("27.00"),3)));assertEquals(2,variantes.count());}
    @Test void entregaInvalidaNoReservaStock(){var d=solicitud(cliente,1);d.setEntrega(null);assertThrows(IllegalArgumentException.class,()->compras.procesarCompra(d));assertEquals(5,stock());}
    @Test void reportesExcluyenPedidosPendientes(){var p=compras.procesarCompra(solicitud(cliente,2));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertTrue(((List<?>)admin.reportes().get("meses")).isEmpty());compras.cambiarEstado(p.getIdPedido(),"PAGADO","OP",otro.getIdUsuario(),true);assertEquals(1,((List<?>)admin.reportes().get("meses")).size());}
    @Test void dosComprasSimultaneasNoSobrevenden() throws Exception {
        var a=solicitud(cliente,4);var b=solicitud(otro,4);List<Object> resultados=concurrentes(()->compras.procesarCompra(a),()->compras.procesarCompra(b));
        assertEquals(1,resultados.stream().filter(Pedido.class::isInstance).count());assertEquals(1,stock());assertEquals(1,pedidos.count());
    }
    @Test void reintentosSimultaneosCreanUnSoloPedido() throws Exception {
        var d=solicitud(cliente,2);List<Object> r=concurrentes(()->compras.procesarCompra(d),()->compras.procesarCompra(d));assertEquals(2,r.stream().filter(Pedido.class::isInstance).count());assertEquals(1,pedidos.count());assertEquals(3,stock());
    }
    @Test void cancelacionesSimultaneasNoDuplicanDevolucion() throws Exception {
        var p=compras.procesarCompra(solicitud(cliente,2));
        var r=concurrentes(()->compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,cliente.getIdUsuario(),false),()->compras.cambiarEstado(p.getIdPedido(),"CANCELADO",null,cliente.getIdUsuario(),false));
        assertEquals(2,r.stream().filter(Pedido.class::isInstance).count());assertEquals(5,stock());
    }
    @Test void apiImpideAccesoAjenoYAdministracionDeCliente() throws Exception {
        var p=compras.procesarCompra(solicitud(cliente,1));
        String token="Bearer "+jwt.generateToken(userDetails.loadUserByUsername(otro.getCorreo()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pedidos/"+p.getIdPedido()).header("Authorization",token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/productos").header("Authorization",token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/pedidos/"+p.getIdPedido()+"/estado").header("Authorization",token).contentType("application/json").content("{\"estado\":\"CANCELADO\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }
    @Test void loginIncompletoYOrigenDesconocidoSeRechazan() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login").contentType("application/json").content("{}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/pedidos").header("Origin","https://no-permitido.example").header("Access-Control-Request-Method","GET")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
    @Test void registrosSimultaneosNoDuplicanCorreo() throws Exception {
        RegistroDto dto=new RegistroDto();dto.setNombre("Nueva");dto.setApellido("Cuenta");dto.setEmail("nueva@example.test");dto.setPassword("ClaveDePrueba123!");
        var r=concurrentes(()->registro.registrarCliente(dto),()->registro.registrarCliente(dto));
        assertEquals(1,r.stream().filter(x->x instanceof org.springframework.http.ResponseEntity<?> e && e.getStatusCode().value()==201).count());
        assertEquals(1,usuarios.findAll().stream().filter(u->u.getCorreo().equals("nueva@example.test")).count());
    }
    @Test void catalogoPaginadoConservaVariantesYFiltraCategoria() {
        var lista=productosService.listarCatalogo(0,"Tops");assertEquals(1,lista.size());assertEquals(variante.getIdVariante(),lista.getFirst().getVariantes().getFirst().getIdVariante());
        assertTrue(productosService.listarCatalogo(1,"Tops").isEmpty());assertTrue(productosService.listarCatalogo(0,"Calzado").isEmpty());
    }
    List<Object> concurrentes(Callable<Object> a,Callable<Object> b) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier inicio=new CyclicBarrier(2);
        try {List<Future<Object>> tareas=new ArrayList<>();for(Callable<Object> c:List.of(a,b))tareas.add(pool.submit(()->{inicio.await(10,TimeUnit.SECONDS);try{return c.call();}catch(Exception e){return e;}}));
            return List.of(tareas.get(0).get(20,TimeUnit.SECONDS),tareas.get(1).get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
}
