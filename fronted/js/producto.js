document.addEventListener('DOMContentLoaded', async () => {
    const boton = document.querySelector('.btn-agregar');
    const opciones = document.getElementById('contenedor-tallas');
    boton.disabled = true;
    let producto, varianteSeleccionada;
    boton.addEventListener('click', e => {
        e.preventDefault();
        if (varianteSeleccionada) agregarAlCarrito(producto, varianteSeleccionada);
    });
    try {
        const id = new URLSearchParams(location.search).get('id');
        if (!/^\d+$/.test(id || '')) throw new Error('Producto no disponible');
        producto = await obtenerTienda(`/productos/${id}`);
        document.getElementById('prod-titulo').textContent = producto.nombre;
        document.getElementById('prod-categoria').textContent = producto.categoria;
        document.getElementById('prod-descripcion').textContent = producto.descripcion || '';
        document.getElementById('prod-precio').textContent = `S/ ${Number(producto.precioBase).toFixed(2)}`;
        const imagen = document.getElementById('img-principal');
        const url = imagenHTTPS(producto.imagen);
        if (url) imagen.src = url;
        else imagen.removeAttribute('src');
        imagen.alt = url ? producto.nombre : 'Imagen no disponible';
        const fotos=(producto.imagenes?.length?producto.imagenes:[{url:producto.imagen,textoAlternativo:producto.nombre}]).filter(f=>imagenHTTPS(f.url));
        const miniaturas=document.createElement('div');miniaturas.className='galeria-miniaturas';miniaturas.setAttribute('aria-label','Vistas del producto');
        fotos.forEach((foto,i)=>{
            const elegir=document.createElement('button');elegir.type='button';elegir.setAttribute('aria-label',foto.textoAlternativo||'Foto '+(i+1));elegir.setAttribute('aria-pressed',String(i===0));
            const mini=document.createElement('img');mini.src=imagenHTTPS(foto.url);mini.alt=foto.textoAlternativo||producto.nombre;elegir.append(mini);
            elegir.onclick=()=>{imagen.src=mini.src;imagen.alt=mini.alt;miniaturas.querySelectorAll('button').forEach(b=>b.setAttribute('aria-pressed',String(b===elegir)));};
            miniaturas.append(elegir);if(i===0){imagen.src=mini.src;imagen.alt=mini.alt;}
        });
        imagen.after(miniaturas);
        const variantes = variantesDisponibles(producto);
        opciones.replaceChildren();
        if (!variantes.length) opciones.textContent = 'Sin stock disponible.';
        variantes.forEach(variante => {
            const opcion = document.createElement('button');
            opcion.type = 'button';
            opcion.textContent = `${variante.talla} / ${variante.color}`;
            opcion.dataset.idVariante = variante.idVariante;
            opcion.setAttribute('aria-pressed', 'false');
            opcion.style.cssText = 'padding:10px 15px;border:1px solid #ccc;background:#fff;cursor:pointer;font-weight:bold';
            opcion.addEventListener('click', () => {
                opciones.querySelectorAll('button').forEach(b => {
                    b.style.background = '#fff'; b.style.color = '#000'; b.setAttribute('aria-pressed', 'false');
                });
                opcion.style.background = '#1a1a1a'; opcion.style.color = '#fff'; opcion.setAttribute('aria-pressed', 'true');
                varianteSeleccionada = variante;
                document.getElementById('prod-precio').textContent = `S/ ${Number(variante.precio).toFixed(2)}`;
                boton.disabled = false;
            });
            opciones.appendChild(opcion);
        });
    } catch (error) {
        document.getElementById('prod-titulo').textContent = 'Producto no disponible';
        opciones.textContent = error.message;
    }
});
