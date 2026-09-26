// Los carritos antiguos no permiten identificar inequívocamente talla y color.
let carritoIncompatible = false;
function leerCarrito() {
    try {
        const datos = JSON.parse(localStorage.getItem('monarca_carrito') || '[]');
        if (!Array.isArray(datos)) throw new Error('Formato inválido');
        const ids = new Set();
        return datos.filter(item => {
            const valido = item && Number.isSafeInteger(item.idProducto) && item.idProducto > 0
                && Number.isSafeInteger(item.idVariante) && item.idVariante > 0 && !ids.has(item.idVariante)
                && Number.isSafeInteger(item.cantidad) && item.cantidad > 0
                && Number.isSafeInteger(item.stockDisponible) && item.stockDisponible >= item.cantidad
                && Number.isFinite(item.precio) && item.precio >= 0;
            if (!valido) carritoIncompatible = true;
            else ids.add(item.idVariante);
            return valido;
        });
    } catch { carritoIncompatible = true; return []; }
}
let carrito = leerCarrito();
function guardarCarrito() { localStorage.setItem('monarca_carrito', JSON.stringify(carrito)); }
function agregarAlCarrito(producto, variante) {
    if (!variante || !variantesDisponibles(producto).some(v => v.idVariante === variante.idVariante)) {
        alert('Selecciona una talla y un color con stock.'); return false;
    }
    const existente = carrito.find(item => item.idVariante === variante.idVariante);
    const cantidad = (existente?.cantidad || 0) + 1;
    if (cantidad > variante.stock) { alert('No puedes superar el stock disponible.'); return false; }
    const item = {
        idProducto: producto.idProducto, idVariante: variante.idVariante, nombre: producto.nombre,
        sku: variante.sku, talla: variante.talla, color: variante.color, precio: Number(variante.precio),
        cantidad, imagen: imagenHTTPS(producto.imagen), stockDisponible: variante.stock
    };
    if (existente) Object.assign(existente, item); else carrito.push(item);
    guardarCarrito(); renderizarCarrito(); actualizarIconoCarrito();
    alert(`${item.nombre} (${item.talla} / ${item.color}) se agregó a la bolsa.`);
    return true;
}
// Revalidar sin descontar stock; el pedido vuelve a validarlo bajo bloqueo.
async function sincronizarCarrito() {
    const productos = await obtenerCatalogo();
    let cambiado = false;
    const actuales = [];
    for (const item of carrito) {
        const producto = productos.find(p => p.idProducto === item.idProducto);
        const variante = producto && variantesDisponibles(producto).find(v => v.idVariante === item.idVariante);
        if (!variante) { cambiado = true; continue; }
        const nuevo = {...item, nombre: producto.nombre, sku: variante.sku, talla: variante.talla,
            color: variante.color, precio: Number(variante.precio), imagen: imagenHTTPS(producto.imagen),
            stockDisponible: variante.stock, cantidad: Math.min(item.cantidad, variante.stock)};
        if (nuevo.precio !== item.precio || nuevo.cantidad !== item.cantidad) cambiado = true;
        actuales.push(nuevo);
    }
    carrito = actuales; guardarCarrito(); renderizarCarrito(); actualizarIconoCarrito();
    return cambiado;
}
function renderizarCarrito() {
    const contenedor = document.getElementById('carrito-items');
    if (!contenedor) return;
    contenedor.innerHTML = carrito.map(item => `<div class="carrito-item">
        ${imagenProductoHTML(item)}<div class="carrito-item-info"><div class="carrito-item-header"><div>
        <h4>${escaparHTML(item.nombre)}</h4><p>${escaparHTML(item.talla)} / ${escaparHTML(item.color)}</p></div>
        <button data-eliminar="${item.idVariante}" aria-label="Eliminar artículo"><i class="fas fa-trash"></i></button></div>
        <p class="precio-txt">S/ ${item.precio.toFixed(2)}</p><div class="controles-cantidad">
        <button data-variante="${item.idVariante}" data-cambio="-1">-</button><span>${item.cantidad}</span>
        <button data-variante="${item.idVariante}" data-cambio="1" ${item.cantidad >= item.stockDisponible ? 'disabled' : ''}>+</button>
        </div></div></div>`).join('') || '<p>Tu bolsa está vacía.</p>';
    const subtotal = carrito.reduce((suma, item) => suma + item.precio * item.cantidad, 0);
    for (const [id, valor] of [['carrito-subtotal', subtotal], ['carrito-envio', 0], ['carrito-total', subtotal]]) {
        const etiqueta = document.getElementById(id);
        if (etiqueta) etiqueta.textContent = `S/ ${valor.toFixed(2)}`;
    }
}
function cambiarCantidad(idVariante, cambio) {
    const item = carrito.find(i => i.idVariante === idVariante);
    if (!item) return;
    const cantidad = item.cantidad + cambio;
    if (cantidad > item.stockDisponible) { alert('No puedes superar el stock disponible.'); return; }
    if (cantidad <= 0) return eliminarDelCarrito(idVariante);
    item.cantidad = cantidad; guardarCarrito(); renderizarCarrito(); actualizarIconoCarrito();
}
function eliminarDelCarrito(idVariante) {
    carrito = carrito.filter(i => i.idVariante !== idVariante);
    guardarCarrito(); renderizarCarrito(); actualizarIconoCarrito();
}
function actualizarIconoCarrito() {
    const contador = document.getElementById('cart-count');
    if (contador) contador.textContent = carrito.reduce((suma, item) => suma + item.cantidad, 0);
}
document.addEventListener('DOMContentLoaded', () => {
    if (carritoIncompatible) {
        guardarCarrito(); alert('Algunos artículos guardados ya no son compatibles. Vuelve a seleccionar su talla y color en el catálogo.');
    }
    const panel = document.getElementById('panel-carrito'), overlay = document.getElementById('carrito-overlay');
    document.getElementById('btn-abrir-carrito-header')?.addEventListener('click', async e => {
        e.preventDefault(); panel?.classList.add('activo'); overlay?.classList.add('activo'); renderizarCarrito();
        try { if (await sincronizarCarrito()) alert('Tu bolsa se actualizó según los precios y el stock actuales.'); }
        catch { alert('No se pudo actualizar el stock. Inténtalo de nuevo antes de comprar.'); }
    });
    const cerrar = () => { panel?.classList.remove('activo'); overlay?.classList.remove('activo'); };
    document.getElementById('cerrar-carrito')?.addEventListener('click', cerrar);
    overlay?.addEventListener('click', cerrar);
    document.getElementById('carrito-items')?.addEventListener('click', e => {
        const boton = e.target.closest('button');
        if (boton?.dataset.eliminar) eliminarDelCarrito(Number(boton.dataset.eliminar));
        else if (boton?.dataset.variante) cambiarCantidad(Number(boton.dataset.variante), Number(boton.dataset.cambio));
    });
    document.getElementById('btn-finalizar-compra')?.addEventListener('click', e => {
        e.preventDefault();
        if (!carrito.length) { alert('Tu carrito está vacío.'); return; }
        location.href = paginaTienda(localStorage.getItem('token_monarca') ? 'checkout.html' : 'login.html');
    });
    actualizarIconoCarrito(); renderizarCarrito();
});
