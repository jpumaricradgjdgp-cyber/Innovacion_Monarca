package com.Monarca.Backend.service;

import com.Monarca.Backend.dto.ProductoDto;
import com.Monarca.Backend.dto.ProductoResponseDto;
import com.Monarca.Backend.dto.VarianteProductoResponseDto;

import com.Monarca.Backend.model.Categoria;
import com.Monarca.Backend.model.ImagenProducto;
import com.Monarca.Backend.model.Producto;
import com.Monarca.Backend.model.VarianteProducto;

import com.Monarca.Backend.repository.CategoriaRepository;
import com.Monarca.Backend.repository.ImagenProductoRepository;
import com.Monarca.Backend.repository.ProductoRepository;
import com.Monarca.Backend.repository.VarianteProductoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
public class ProductoService {


    // =========================================================
    // REPOSITORIOS
    // =========================================================

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private VarianteProductoRepository varianteRepository;

    @Autowired
    private ImagenProductoRepository imagenRepository;


    // =========================================================
    // LISTAR PRODUCTOS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {

        return productoRepository.findAll();
    }


    // =========================================================
    // BUSCAR PRODUCTO POR ID
    // =========================================================

    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id) {

        return productoRepository.findById(id);
    }




    // =========================================================
    // GUARDAR PRODUCTO
    // =========================================================

    @Transactional
    public Producto guardar(ProductoDto dto) {

        validarDto(dto);
        validarLimites(dto);
        validarImagen(dto.getImg());
        if (dto.getPrecioBase() != null && dto.getPrecioBase().signum() < 0) throw new IllegalArgumentException("Precio base inválido");


        // -----------------------------------------------------
        // BUSCAR CATEGORÍA
        // -----------------------------------------------------

        Categoria categoria = categoriaRepository
                .findByNombreIgnoreCase(dto.getCategoria())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Categoría no encontrada: "
                                        + dto.getCategoria()
                        )
                );


        BigDecimal precio = dto.getPrecio();


        // -----------------------------------------------------
        // CREAR PRODUCTO
        // -----------------------------------------------------

        Producto producto = new Producto();

        producto.setNombre(
                dto.getNombre().trim()
        );

        producto.setCategoria(
                categoria
        );

        producto.setSlug(
                generarSlugUnico(
                        dto.getNombre(),
                        null
                )
        );

        producto.setDescripcion(
                valorODefecto(
                        dto.getDescripcion(),
                        "Sin descripción"
                )
        );

        producto.setMarca(
                valorODefecto(
                        dto.getMarca(),
                        "Monarca"
                )
        );

        producto.setPrecioBase(dto.getPrecioBase() != null ? dto.getPrecioBase() : precio);

        producto.setActivo(
                true
        );

        producto.setDestacado(
                false
        );


        producto = productoRepository.save(
                producto
        );


        // =====================================================
        // CREAR PRIMERA VARIANTE
        // =====================================================

        VarianteProducto variante =
                new VarianteProducto();


        variante.setProducto(
                producto
        );


        variante.setSku(
                generarSku(producto)
        );


        variante.setTalla(
                valorODefecto(
                        dto.getTalla(),
                        "ÚNICA"
                )
        );


        variante.setColor(
                valorODefecto(
                        dto.getColor(),
                        "N/A"
                )
        );


        variante.setColorHex(
                dto.getColorHex()
        );


        variante.setPrecio(
                precio
        );


        variante.setStock(
                dto.getStock() != null
                        ? dto.getStock()
                        : 0
        );


        variante.setStockMinimo(
                dto.getStockMinimo() != null
                        ? dto.getStockMinimo()
                        : 0
        );


        variante.setActivo(
                true
        );


        varianteRepository.save(
                variante
        );


        // =====================================================
        // GUARDAR IMAGEN PRINCIPAL
        // =====================================================

        if (
                dto.getImagenes() == null && dto.getImg() != null
                        && !dto.getImg().isBlank()
        ) {

            ImagenProducto imagen =
                    new ImagenProducto();


            imagen.setProducto(
                    producto
            );


            /*
             * La imagen principal pertenece al producto.
             *
             * No la asociamos a una talla específica.
             *
             * Una imagen podría asociarse a una variante
             * posteriormente si, por ejemplo:
             *
             * Negro -> foto negra
             * Blanco -> foto blanca
             */
            imagen.setVariante(
                    null
            );


            imagen.setUrl(
                    dto.getImg().trim()
            );


            imagen.setTextoAlternativo(
                    producto.getNombre()
            );


            imagen.setPrincipal(
                    true
            );


            imagen.setOrden(
                    0
            );


            imagenRepository.save(
                    imagen
            );
        }


        guardarGaleria(producto, dto.getImagenes());
        return producto;
    }


    // =========================================================
    // ACTUALIZAR PRODUCTO
    // =========================================================

    @Transactional
    public Producto actualizar(
            Long id,
            ProductoDto dto
    ) {
        validarLimites(dto);
        if (dto.getStock() != null) throw new IllegalArgumentException("Usa el ajuste de stock; editar un producto no cambia existencias");

        validarImagen(dto.getImg());
        if (dto.getPrecioBase() != null && dto.getPrecioBase().signum() < 0) throw new IllegalArgumentException("Precio base inválido");
        if (dto.getPrecio() != null && dto.getPrecio().signum() < 0) throw new IllegalArgumentException("Precio inválido");
        Producto producto = productoRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Producto no encontrado"
                        )
                );


        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------

        if (
                dto.getNombre() != null
                        && !dto.getNombre().isBlank()
        ) {

            producto.setNombre(
                    dto.getNombre().trim()
            );


            producto.setSlug(
                    generarSlugUnico(
                            dto.getNombre(),
                            producto.getIdProducto()
                    )
            );
        }


        // -----------------------------------------------------
        // CATEGORÍA
        // -----------------------------------------------------

        if (
                dto.getCategoria() != null
                        && !dto.getCategoria().isBlank()
        ) {

            Categoria categoria =
                    categoriaRepository
                            .findByNombreIgnoreCase(
                                    dto.getCategoria()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Categoría no encontrada: "
                                                    + dto.getCategoria()
                                    )
                            );


            producto.setCategoria(
                    categoria
            );
        }


        // -----------------------------------------------------
        // DESCRIPCIÓN
        // -----------------------------------------------------

        if (dto.getDescripcion() != null) {

            producto.setDescripcion(
                    dto.getDescripcion()
            );
        }


        // -----------------------------------------------------
        // MARCA
        // -----------------------------------------------------

        if (
                dto.getMarca() != null
                        && !dto.getMarca().isBlank()
        ) {

            producto.setMarca(
                    dto.getMarca().trim()
            );
        }


        // -----------------------------------------------------
        // PRECIO BASE
        // -----------------------------------------------------

        if (dto.getPrecioBase() != null) {
            producto.setPrecioBase(dto.getPrecioBase());
        }


        producto = productoRepository.save(
                producto
        );


        // =====================================================
        // ACTUALIZAR VARIANTE
        // =====================================================

        List<VarianteProducto> variantes =
                varianteRepository
                        .findByProducto_IdProducto(
                                producto.getIdProducto()
                        );


        VarianteProducto variante;


        // -----------------------------------------------------
        // NO EXISTEN VARIANTES
        // -----------------------------------------------------

        if (dto.getIdVariante() != null) {
            variante = varianteRepository.findByIdForUpdate(dto.getIdVariante())
                    .filter(v -> v.getProducto().getIdProducto().equals(id))
                    .orElseThrow(() -> new IllegalArgumentException("La variante no pertenece al producto"));
        }
        else if (variantes.isEmpty()) {

            variante =
                    new VarianteProducto();


            variante.setProducto(
                    producto
            );


            variante.setSku(
                    generarSku(producto)
            );


            variante.setActivo(
                    true
            );


        }

        // -----------------------------------------------------
        // EXISTE SOLO UNA VARIANTE
        // -----------------------------------------------------

        else if (variantes.size() == 1) {

            variante =
                    varianteRepository.findByIdForUpdate(variantes.get(0).getIdVariante()).orElseThrow();

        }

        // -----------------------------------------------------
        // EXISTEN VARIAS VARIANTES
        // -----------------------------------------------------

        else {

            variante =
                    buscarVarianteCoincidente(
                            variantes,
                            dto
                    );
            variante = varianteRepository.findByIdForUpdate(variante.getIdVariante()).orElseThrow();
        }


        // -----------------------------------------------------
        // TALLA
        // -----------------------------------------------------

        if (
                dto.getTalla() != null
                        && !dto.getTalla().isBlank()
        ) {

            variante.setTalla(
                    dto.getTalla().trim()
            );

        } else if (
                variante.getTalla() == null
        ) {

            variante.setTalla(
                    "ÚNICA"
            );
        }


        // -----------------------------------------------------
        // COLOR
        // -----------------------------------------------------

        if (
                dto.getColor() != null
                        && !dto.getColor().isBlank()
        ) {

            variante.setColor(
                    dto.getColor().trim()
            );

        } else if (
                variante.getColor() == null
        ) {

            variante.setColor(
                    "N/A"
            );
        }


        // -----------------------------------------------------
        // COLOR HEX
        // -----------------------------------------------------

        if (dto.getColorHex() != null) {

            variante.setColorHex(
                    dto.getColorHex()
            );
        }


        // -----------------------------------------------------
        // PRECIO DE VARIANTE
        // -----------------------------------------------------

        if (dto.getPrecio() != null) {

            variante.setPrecio(
                    dto.getPrecio()
            );

        } else if (
                variante.getPrecio() == null
        ) {

            variante.setPrecio(
                    producto.getPrecioBase()
            );
        }


        // -----------------------------------------------------
        // STOCK
        // -----------------------------------------------------

        if (dto.getStock() != null) {

            if (dto.getStock() < 0) {

                throw new IllegalArgumentException(
                        "El stock no puede ser negativo"
                );
            }


            variante.setStock(
                    dto.getStock()
            );

        } else if (
                variante.getStock() == null
        ) {

            variante.setStock(
                    0
            );
        }


        // -----------------------------------------------------
        // STOCK MÍNIMO
        // -----------------------------------------------------

        if (dto.getStockMinimo() != null) {

            if (dto.getStockMinimo() < 0) {

                throw new IllegalArgumentException(
                        "El stock mínimo no puede ser negativo"
                );
            }


            variante.setStockMinimo(
                    dto.getStockMinimo()
            );

        } else if (
                variante.getStockMinimo() == null
        ) {

            variante.setStockMinimo(
                    0
            );
        }


        final VarianteProducto seleccionada = variante;
        if (variantes.stream().anyMatch(v -> !java.util.Objects.equals(v.getIdVariante(), seleccionada.getIdVariante())
                && v.getTalla().equalsIgnoreCase(seleccionada.getTalla()) && v.getColor().equalsIgnoreCase(seleccionada.getColor())))
            throw new IllegalArgumentException("La combinación de talla y color ya existe");
        varianteRepository.save(variante);

        // =====================================================
        // ACTUALIZAR IMAGEN PRINCIPAL
        // =====================================================

        if (
                dto.getImagenes() == null && dto.getImg() != null
                        && !dto.getImg().isBlank()
        ) {

            ImagenProducto imagen =
                    imagenRepository
                            .findFirstByProducto_IdProductoAndPrincipalTrue(
                                    producto.getIdProducto()
                            )
                            .orElseGet(
                                    ImagenProducto::new
                            );


            imagen.setProducto(
                    producto
            );


            /*
             * Imagen principal general del producto.
             */
            imagen.setVariante(
                    null
            );


            imagen.setUrl(
                    dto.getImg().trim()
            );


            imagen.setTextoAlternativo(
                    producto.getNombre()
            );


            imagen.setPrincipal(
                    true
            );


            imagen.setOrden(
                    0
            );


            imagenRepository.save(
                    imagen
            );
        }


        guardarGaleria(producto, dto.getImagenes());
        return producto;
    }


    // =========================================================
    // COMPATIBILIDAD TEMPORAL
    // =========================================================



    // =========================================================
    // ELIMINAR PRODUCTO
    // =========================================================

    @Transactional
    public void eliminar(
            Long id
    ) {

        if (
                !productoRepository.existsById(id)
        ) {

            throw new IllegalArgumentException(
                    "Producto no encontrado"
            );
        }


        Producto producto = productoRepository.findById(id).orElseThrow();
        producto.setActivo(false);
        productoRepository.save(producto);
    }




    // =========================================================
    // CATÁLOGO PARA EL FRONTEND
    // =========================================================

    /*
     * Este método NO devuelve directamente las entidades JPA.
     *
     * Devuelve DTOs preparados para el frontend.
     *
     * Así evitamos:
     * - errores de serialización
     * - proxies LAZY de Hibernate
     * - relaciones innecesarias
     * - ciclos JSON
     */
    @Transactional(readOnly = true)
    public List<ProductoResponseDto> listarCatalogo(int numero, String categoria) {
        var pagina = org.springframework.data.domain.PageRequest.of(Math.max(0, numero),60,org.springframework.data.domain.Sort.by("idProducto"));
        List<Producto> productos = (categoria == null || categoria.isBlank() ? productoRepository.findByActivoTrue(pagina)
                : productoRepository.findByActivoTrueAndCategoria_NombreIgnoreCase(categoria.trim(),pagina)).getContent();
        if (productos.isEmpty()) return List.of();
        List<Long> ids = productos.stream().map(Producto::getIdProducto).toList();
        var porProducto = varianteRepository.findByProducto_IdProductoInAndActivoTrue(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(v -> v.getProducto().getIdProducto()));
        var fotos = imagenRepository.findByProducto_IdProductoInAndPrincipalTrue(ids).stream()
                .collect(java.util.stream.Collectors.toMap(i -> i.getProducto().getIdProducto(), ImagenProducto::getUrl,(a,b)->a));
        return productos.stream().map(p -> respuesta(p,porProducto.getOrDefault(p.getIdProducto(),List.of()),fotos.get(p.getIdProducto()))).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductoResponseDto> buscarCatalogoPorId(Long id) {
        return productoRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(this::convertirRespuesta);
    }

    private ProductoResponseDto convertirRespuesta(Producto producto) {
        var detalle = respuesta(producto, varianteRepository.findByProducto_IdProductoAndActivoTrue(producto.getIdProducto()),
                imagenRepository.findFirstByProducto_IdProductoAndPrincipalTrue(producto.getIdProducto()).map(ImagenProducto::getUrl).orElse(null));
        detalle.setImagenes(imagenRepository.findByProducto_IdProductoOrderByOrdenAscIdImagenAsc(producto.getIdProducto()).stream()
                .map(i -> new com.Monarca.Backend.dto.ImagenDto(i.getUrl(),i.getTextoAlternativo())).toList());
        return detalle;
    }
    private ProductoResponseDto respuesta(Producto producto, List<VarianteProducto> disponibles, String imagen) {
        List<VarianteProductoResponseDto> variantes = disponibles.stream().map(v -> new VarianteProductoResponseDto(v.getIdVariante(),v.getSku(),v.getTalla(),v.getColor(),v.getColorHex(),v.getPrecio(),v.getStock())).toList();
        return new ProductoResponseDto(producto.getIdProducto(),producto.getNombre(),producto.getSlug(),producto.getDescripcion(),producto.getMarca(),producto.getPrecioBase(),producto.getDestacado(),producto.getCategoria()==null?null:producto.getCategoria().getNombre(),imagen,variantes,List.of());
    }

    // =========================================================
    // BUSCAR VARIANTE
    // =========================================================

    private VarianteProducto buscarVarianteCoincidente(
            List<VarianteProducto> variantes,
            ProductoDto dto
    ) {

        if (
                dto.getTalla() == null
                        || dto.getColor() == null
        ) {

            throw new IllegalArgumentException(
                    "El producto tiene varias variantes. "
                            + "Debes indicar talla y color."
            );
        }


        return variantes
                .stream()

                .filter(variante ->

                        variante.getTalla() != null

                                && variante.getColor() != null

                                && variante
                                .getTalla()
                                .equalsIgnoreCase(
                                        dto.getTalla()
                                )

                                && variante
                                .getColor()
                                .equalsIgnoreCase(
                                        dto.getColor()
                                )
                )

                .findFirst()

                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró la variante indicada"
                        )
                );
    }


    // =========================================================
    // VALIDAR DTO
    // =========================================================

    private void validarDto(
            ProductoDto dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos del producto requeridos"
            );
        }


        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------

        if (
                dto.getNombre() == null
                        || dto.getNombre().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio"
            );
        }


        // -----------------------------------------------------
        // CATEGORÍA
        // -----------------------------------------------------

        if (
                dto.getCategoria() == null
                        || dto.getCategoria().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "La categoría es obligatoria"
            );
        }


        // -----------------------------------------------------
        // PRECIO
        // -----------------------------------------------------

        if (
                dto.getPrecio() == null
                        || dto.getPrecio()
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        ) {

            throw new IllegalArgumentException(
                    "El precio no es válido"
            );
        }


        // -----------------------------------------------------
        // STOCK
        // -----------------------------------------------------

        if (
                dto.getStock() != null
                        && dto.getStock() < 0
        ) {

            throw new IllegalArgumentException(
                    "El stock no puede ser negativo"
            );
        }


        // -----------------------------------------------------
        // STOCK MÍNIMO
        // -----------------------------------------------------

        if (
                dto.getStockMinimo() != null
                        && dto.getStockMinimo() < 0
        ) {

            throw new IllegalArgumentException(
                    "El stock mínimo no puede ser negativo"
            );
        }
    }


    // =========================================================
    // GENERAR SKU
    // =========================================================

    private String generarSku(
            Producto producto
    ) {

        return "MON-"
                + producto.getIdProducto()
                + "-"
                + System.currentTimeMillis();
    }


    // =========================================================
    // GENERAR SLUG ÚNICO
    // =========================================================

    private String generarSlugUnico(
            String nombre,
            Long idActual
    ) {

        String base =
                Normalizer
                        .normalize(
                                nombre,
                                Normalizer.Form.NFD
                        )

                        .replaceAll(
                                "\\p{M}",
                                ""
                        )

                        .toLowerCase(
                                Locale.ROOT
                        )

                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        )

                        .replaceAll(
                                "^-|-$",
                                ""
                        );


        if (base.isBlank()) {

            base =
                    "producto";
        }


        String slug =
                base;

        int contador =
                2;


        while (
                idActual == null
                        ? productoRepository
                        .existsBySlug(
                                slug
                        )

                        : productoRepository
                        .existsBySlugAndIdProductoNot(
                                slug,
                                idActual
                        )
        ) {

            slug =
                    base
                            + "-"
                            + contador;

            contador++;
        }


        return slug;
    }


    // =========================================================
    // VALOR POR DEFECTO
    // =========================================================

    private String valorODefecto(
            String valor,
            String defecto
    ) {

        if (
                valor == null
                        || valor.isBlank()
        ) {

            return defecto;
        }


        return valor.trim();
    }
    private void validarLimites(ProductoDto d) {
        if (d == null) throw new IllegalArgumentException("Datos requeridos");
        ValidacionTalla.comprobar(d.getTalla());
        if (d.getDescripcion()!=null && d.getDescripcion().length()>5000) throw new IllegalArgumentException("La descripción admite hasta 5000 caracteres");
        String[] textos = {d.getNombre(), d.getCategoria(), d.getMarca(), d.getTalla(), d.getColor(), d.getColorHex()};
        int[] largos = {150,100,100,20,50,10};
        for (int i=0;i<textos.length;i++) if (textos[i]!=null && textos[i].length()>largos[i]) throw new IllegalArgumentException("Un campo supera la longitud permitida");
        for (BigDecimal precio : new BigDecimal[]{d.getPrecio(),d.getPrecioBase()})
            if (precio!=null && (precio.signum()<0 || precio.compareTo(new BigDecimal("99999999.99"))>0 || precio.stripTrailingZeros().scale()>2))
                throw new IllegalArgumentException("Precio inválido; usa hasta dos decimales");
    }
    private void guardarGaleria(Producto producto, List<com.Monarca.Backend.dto.ImagenDto> fotos) {
        if (fotos == null) return; // Clientes antiguos conservan las imágenes existentes.
        if (fotos.size()>8) throw new IllegalArgumentException("Admite hasta 8 fotos por producto");
        for (var foto : fotos) {
            if (foto == null || foto.url()==null || foto.url().isBlank() || foto.url().length()>2048)
                throw new IllegalArgumentException("Cada foto necesita una URL válida");
            validarImagen(foto.url());
            if (foto.textoAlternativo()!=null && foto.textoAlternativo().length()>200)
                throw new IllegalArgumentException("La descripción de la foto admite 200 caracteres");
        }
        var anteriores = imagenRepository.findByProducto_IdProductoOrderByOrdenAscIdImagenAsc(producto.getIdProducto());
        imagenRepository.deleteAll(anteriores);
        imagenRepository.flush();
        for (int n=0;n<fotos.size();n++) {
            var foto=fotos.get(n); var imagen=new ImagenProducto();
            imagen.setProducto(producto); imagen.setUrl(foto.url().trim());
            imagen.setTextoAlternativo(valorODefecto(foto.textoAlternativo(),producto.getNombre()));
            imagen.setPrincipal(n==0); imagen.setOrden(n); imagenRepository.save(imagen);
        }
    }
    private void validarImagen(String imagen) {
        if (imagen == null || imagen.isBlank()) return;
        try {
            java.net.URI uri = java.net.URI.create(imagen.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La imagen debe ser una URL HTTPS válida");
        }
    }

}
