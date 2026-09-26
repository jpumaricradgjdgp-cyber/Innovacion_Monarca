document.addEventListener('DOMContentLoaded', async () => {
    const contenedor = document.getElementById('contenedor-productos');
    const recomendados = document.querySelector('#productos .productos-grid');
    if (!contenedor && !recomendados) return;
    const normalizar = valor => String(valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    document.querySelectorAll('.filtro-item').forEach(boton => boton.addEventListener('click', () => {
        boton.classList.toggle('abierto'); boton.nextElementSibling?.classList.toggle('activo');
    }));
    try {
        const productos = await obtenerCatalogo(contenedor?.dataset.categoria);
        if (recomendados) {
            const destacados = productos.filter(p => p.destacado);
            recomendados.innerHTML = (destacados.length ? destacados : productos).slice(0, 4).map(p => `
                <a class="producto" href="${paginaTienda(`producto.html?id=${p.idProducto}`)}">
                <div class="producto-image">${imagenProductoHTML(p)}</div>
                <div class="producto-info"><h3>${escaparHTML(p.nombre)}</h3>
                <span class="precio">S/ ${Number(p.precioBase).toFixed(2)}</span></div></a>`).join('') || '<p>No hay productos disponibles.</p>';
            return;
        }
        const lista = productos.filter(p => normalizar(p.categoria) === normalizar(contenedor.dataset.categoria));
        const grupos = [...document.querySelectorAll('.filtro-grupo')];
        const seleccion = indice => [...(grupos[indice]?.querySelectorAll('input:checked') || [])].map(c => normalizar(c.value));
        const buscador = document.getElementById('buscador-productos');
        let pagina=0;
        const resultados=document.createElement('section');resultados.className='catalogo-resultados';
        contenedor.before(resultados);resultados.append(contenedor);
        const navegacion=document.createElement('nav');navegacion.className='paginador';navegacion.setAttribute('aria-label','Páginas de productos');contenedor.after(navegacion);
        function renderizar() {
            const tipos = seleccion(1), colores = seleccion(2), tallas = seleccion(3), precios = seleccion(4);
            const texto = normalizar(buscador?.value);
            const visibles = lista.filter(p => {
                const nombre = normalizar(p.nombre).replaceAll('-', ' ');
                const precio = Number(p.precioBase) < Number(contenedor.dataset.limitePrecio) ? 'bajo' : 'alto';
                return nombre.includes(texto) && (!tipos.length || tipos.some(t => nombre.includes(t.replaceAll('-', ' '))))
                    && (!precios.length || precios.includes(precio))
                    && ((!colores.length && !tallas.length) || variantesDisponibles(p).some(v =>
                        (!colores.length || colores.includes(normalizar(v.color))) && (!tallas.length || tallas.includes(normalizar(v.talla)))));
            });
            contenedor.innerHTML = visibles.slice(pagina*24,(pagina+1)*24).map(p => {
                const variantes = variantesDisponibles(p);
                return `<a href="producto.html?id=${p.idProducto}" class="producto-card">
                    <div class="img-container">${imagenProductoHTML(p)}</div>
                    <div class="producto-info"><div class="producto-header"><div>
                    <span class="marca">${escaparHTML(p.marca || 'Monarca')} · ${escaparHTML(p.categoria)}</span>
                    <h3>${escaparHTML(p.nombre)}</h3></div><div class="precio-container">
                    <span class="precio-actual">S/ ${Number(p.precioBase).toFixed(2)}</span></div></div>
                    <div class="producto-hover-info"><hr class="linea-divisora">
                    <div class="tallas">${[...new Set(variantes.map(v => v.talla))].map(t => `<span class="talla-btn">${escaparHTML(t)}</span>`).join('')}</div>
                    <button class="btn-agregar" data-producto="${p.idProducto}" ${variantes.length ? '' : 'disabled'}>
                    ${variantes.length === 1 ? 'AGREGAR AL CARRITO' : variantes.length ? 'ELEGIR TALLA / COLOR' : 'SIN STOCK'}</button></div></div></a>`;
            }).join('') || '<p>No hay productos para estos filtros.</p>';
            navegacion.innerHTML='<button '+(pagina===0?'disabled':'')+'>Anterior</button> <span aria-live="polite">Página '+(pagina+1)+' de '+Math.max(1,Math.ceil(visibles.length/24))+'</span> <button '+((pagina+1)*24>=visibles.length?'disabled':'')+'>Siguiente</button>';
            navegacion.firstElementChild.onclick=()=>{pagina--;renderizar();};navegacion.lastElementChild.onclick=()=>{pagina++;renderizar();};
        }
        document.querySelectorAll('.filtro-opciones input').forEach(c => c.addEventListener('change', ()=>{pagina=0;renderizar();}));
        buscador?.addEventListener('input', ()=>{pagina=0;renderizar();});
        contenedor.addEventListener('click', e => {
            const boton = e.target.closest('button[data-producto]');
            if (!boton) return;
            e.preventDefault();
            const producto = lista.find(p => p.idProducto === Number(boton.dataset.producto));
            const variantes = variantesDisponibles(producto);
            if (variantes.length === 1) agregarAlCarrito(producto, variantes[0]);
            else if (variantes.length) location.href = paginaTienda(`producto.html?id=${producto.idProducto}`);
        });
        renderizar();
    } catch (error) { (contenedor || recomendados).textContent = error.message; }
});
