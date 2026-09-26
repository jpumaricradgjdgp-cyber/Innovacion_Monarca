// Contrato compartido por catálogo, detalle, carrito y checkout.
const TIENDA_API = window.MONARCA_CONFIG.apiUrl.replace(/\/$/, '');
const TIENDA_RAIZ = new URL('../', document.currentScript.src);
function paginaTienda(nombre) { return new URL(`Paginas/${nombre}`, TIENDA_RAIZ).href; }
function escaparHTML(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
}
function imagenHTTPS(valor) {
    try { const url = new URL(valor); return url.protocol === 'https:' ? url.href : ''; }
    catch { return ''; }
}
function imagenProductoHTML(producto) {
    const url = imagenHTTPS(producto.imagen);
    return url ? `<img src="${escaparHTML(url)}" alt="${escaparHTML(producto.nombre)}">` : '<span class="sin-imagen">Imagen no disponible</span>';
}
function variantesDisponibles(producto) {
    return (producto.variantes || []).filter(v => Number.isSafeInteger(v.idVariante) && v.stock > 0);
}
async function obtenerTienda(ruta, autenticado = false) {
    const headers = autenticado ? {Authorization: `Bearer ${localStorage.getItem('token_monarca') || ''}`} : {};
    const respuesta = await fetch(`${TIENDA_API}${ruta}`, {headers});
    if (!respuesta.ok) throw new Error(`No se pudo cargar la tienda (${respuesta.status}).`);
    return respuesta.json();
}

async function solicitarTienda(ruta, opciones={}) {
    const respuesta=await fetch(TIENDA_API+ruta,{method:opciones.method||'GET',headers:{'Content-Type':'application/json',Authorization:'Bearer '+(localStorage.getItem('token_monarca')||'')},body:opciones.body===undefined?undefined:JSON.stringify(opciones.body)});
    const texto=await respuesta.text();let datos;try{datos=texto?JSON.parse(texto):null;}catch{datos=texto;}
    if(!respuesta.ok){const error=new Error(respuesta.status===401||respuesta.status===403?'Tu sesión no permite esta acción. Inicia sesión con la cuenta adecuada.':datos?.error||'No se pudo completar la operación.');error.status=respuesta.status;throw error;}
    return datos;
}

async function obtenerCatalogo(categoria) {
    let pagina=0, todos=[];
    while(true){const lote=await obtenerTienda('/productos?pagina='+pagina+(categoria?'&categoria='+encodeURIComponent(categoria):''));todos.push(...lote);if(lote.length<60)return todos;pagina++;}
}
