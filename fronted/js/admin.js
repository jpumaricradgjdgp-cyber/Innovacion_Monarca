let inventario=[], paginaInventario=0, agregandoVariante=false;
const campo=id=>document.getElementById(id);
function prepararFormulario(modo,producto) {
    campo('datos-generales').hidden=modo==='variante';campo('datos-generales').disabled=modo==='variante';
    campo('selector-variante').hidden=modo!=='editar';campo('stock-inicial').hidden=modo==='editar';
    campo('error-producto').hidden=true;campo('prod-talla').setCustomValidity('');
    campo('ayuda-formulario').textContent=modo==='nuevo'?'Crea el producto con su primera talla y color. Después puedes añadir las otras combinaciones.':modo==='variante'?'Nueva talla y color para: '+producto.nombre:'Corrige los datos de '+producto.nombre+'. Selecciona abajo la variante que quieres editar.';
    campo('form-producto').querySelector('button[type="submit"]').textContent=modo==='variante'?'Guardar variante':'Guardar Producto';
    document.querySelector('.modal-content').scrollTop=0;
}
function errorProducto(mensaje) {campo('error-producto').textContent=mensaje;campo('error-producto').hidden=false;campo('error-producto').scrollIntoView({block:'nearest'});}
function cerrarModal(){campo('modalCRUD').style.display='none';}
function abrirModal(){prepararFormulario('nuevo');campo('ajuste-stock').hidden=true;agregandoVariante=false;campo('form-producto').reset();iniciarGaleria([]);campo('editor-fotos').hidden=false;campo('prod-id').value='';campo('prod-variante').hidden=true;
    campo('prod-stock').disabled=false;campo('prod-stock').hidden=false;campo('modal-titulo').textContent='Nuevo producto';campo('modalCRUD').style.display='flex';}
function renderTabla(datos){campo('tabla-body').innerHTML=datos.map(p=>`<tr><td>${imagenProductoHTML(p)}</td><td>${escaparHTML(p.nombre)}<br>${p.activo?'Activo':'Inactivo'}</td>
    <td>${escaparHTML(p.categoria)}</td><td>${p.variantes.map(v=>`<button type="button" class="editar-variante-fila" data-editar-variante="${v.idVariante}" data-producto="${p.idProducto}">Editar ${escaparHTML(v.talla)} / ${escaparHTML(v.color)}</button><small>${v.stock} unidades · ${v.activo?'Activa':'Inactiva'}</small>`).join('')}</td><td>S/ ${Number(p.precioBase).toFixed(2)}</td><td>${p.variantes.reduce((n,v)=>n+v.stock,0)}</td>
    <td><button data-editar="${p.idProducto}">Editar</button><button data-variante="${p.idProducto}">Añadir variante</button><button data-producto-activo="${p.idProducto}" data-activo="${!p.activo}">${p.activo?'Desactivar':'Reactivar'}</button></td></tr>`).join('') || '<tr><td colspan="7">Sin productos en esta página.</td></tr>';}
async function cargarProductos(){try{inventario=await solicitarTienda(`/admin/productos?pagina=${paginaInventario}`);renderTabla(inventario);
    campo('inventario-anterior').disabled=paginaInventario===0;campo('inventario-siguiente').disabled=inventario.length<20;campo('inventario-pagina').textContent=`Página ${paginaInventario+1}`;
}catch(e){campo('mensaje-gestion').textContent=e.message;}}
function editarProducto(id,nueva=false,idVariante=null){const p=inventario.find(p=>p.idProducto===id);agregandoVariante=nueva;prepararFormulario(nueva?'variante':'editar',p);
    campo('prod-id').value=p.idProducto;campo('prod-nombre').value=p.nombre;campo('prod-categoria').value=p.categoria;campo('prod-precio-base').value=p.precioBase;campo('prod-img').value='';campo('prod-descripcion').value=p.descripcion||'';iniciarGaleria(p.imagenes?.length?p.imagenes:(p.imagen?[{url:p.imagen,textoAlternativo:p.nombre}]:[]));campo('editor-fotos').hidden=nueva;
    campo('prod-variante').hidden=nueva;campo('prod-variante').innerHTML=p.variantes.map(v=>`<option value="${v.idVariante}">${escaparHTML(v.talla+' / '+v.color+(v.activo?'':' (inactiva)')+' · '+v.stock+' unidades')}</option>`).join('');
    if(idVariante!==null)campo('prod-variante').value=String(idVariante);
    const seleccionar=()=>{const v=p.variantes.find(v=>v.idVariante===Number(campo('prod-variante').value));campo('prod-talla').setCustomValidity('');campo('prod-talla').value=v?.talla||'';campo('prod-talla').dispatchEvent(new Event('input'));campo('prod-color').value=v?.color||'';campo('prod-precio').value=v?.precio??p.precioBase;campo('stock-actual').textContent=`Stock actual: ${v?.stock??0}`;campo('estado-variante').textContent=v?.activo?'Desactivar variante':'Reactivar variante';};
    campo('prod-variante').onchange=seleccionar;seleccionar();
    campo('prod-stock').disabled=!nueva;campo('prod-stock').hidden=!nueva;campo('ajuste-stock').hidden=nueva;
    if(nueva){campo('prod-talla').value='';campo('prod-color').value='';campo('prod-stock').value=0;}
    campo('modal-titulo').textContent=nueva?'Añadir variante (no modifica el producto)':'Editar producto y variante';campo('modalCRUD').style.display='flex';}
async function cargarReportes(){try{const r=await solicitarTienda('/admin/reportes');
    const tabla=(filas,cols)=>filas.length?`<table class="report-tabla"><thead><tr>${cols.map(c=>`<th>${escaparHTML(c)}</th>`).join('')}</tr></thead><tbody>${filas.map(f=>`<tr>${cols.map(c=>`<td>${escaparHTML(f[c])}</td>`).join('')}</tr>`).join('')}</tbody></table>`:'<p>Aún no hay pagos confirmados.</p>';
    campo('reporte-meses').innerHTML='<p>Año · mes · clientes · importe (S/). Pedidos pagados, agrupados por fecha del pedido.</p>'+tabla(r.meses,['anio','mes','clientes','total']);
    campo('reporte-categorias').innerHTML='<p>Categoría actual · unidades pagadas · importe (S/)</p>'+tabla(r.categorias,['categoria','unidades','total']);
    campo('reporte-estados').innerHTML='<h3>Pedidos por estado</h3>'+tabla(r.estados,['estado','cantidad','total']);
}catch(e){campo('reporte-estados').textContent=e.message;}}
document.addEventListener('DOMContentLoaded',async()=>{
    if(!localStorage.getItem('token_monarca')){location.href=paginaTienda('login.html');return;}
    campo('prod-talla').addEventListener('input',()=>{
        const valor=campo('prod-talla').value.trim();
        campo('prod-talla').setCustomValidity(/[-–—,;/+|]/.test(valor)||/^(?:XXS|XS|S|M|L|XL|XXL|XXXL|[0-9]+)(?:\s+(?:XXS|XS|S|M|L|XL|XXL|XXXL|[0-9]+))+$/i.test(valor)?'Escribe una sola talla, por ejemplo S. Añade M y L como variantes separadas.':'');
    });
    campo('info-admin').textContent='Administración';
    campo('salir-admin').onclick=()=>{localStorage.removeItem('token_monarca');localStorage.removeItem('rol_monarca');location.href=paginaTienda('login.html');};
    campo('buscador').oninput=e=>renderTabla(inventario.filter(p=>p.nombre.toLowerCase().includes(e.target.value.toLowerCase())));
    campo('inventario-anterior').onclick=()=>{paginaInventario--;cargarProductos();};campo('inventario-siguiente').onclick=()=>{paginaInventario++;cargarProductos();};
    campo('tabla-body').addEventListener('click',async e=>{const b=e.target.closest('button');if(!b)return;
        if(b.dataset.editarVariante)return editarProducto(Number(b.dataset.producto),false,Number(b.dataset.editarVariante));if(b.dataset.editar)return editarProducto(Number(b.dataset.editar));if(b.dataset.variante)return editarProducto(Number(b.dataset.variante),true);
        if(b.dataset.productoActivo && confirm('¿Cambiar la disponibilidad del producto?')){b.disabled=true;try{await solicitarTienda(`/admin/productos/${b.dataset.productoActivo}/activo`,{method:'PATCH',body:{activo:b.dataset.activo==='true'}});await cargarProductos();}catch(error){alert(error.message);}finally{b.disabled=false;}}});
    campo('form-producto').onsubmit=async e=>{e.preventDefault();const b=e.submitter;if(b.disabled)return;b.disabled=true;campo('error-producto').hidden=true;
        const id=campo('prod-id').value;const datos={nombre:campo('prod-nombre').value.trim(),categoria:campo('prod-categoria').value,precioBase:Number(campo('prod-precio-base').value),
            descripcion:campo('prod-descripcion').value,talla:campo('prod-talla').value.trim(),color:campo('prod-color').value.trim(),precio:Number(campo('prod-precio').value)};
        if(!id || agregandoVariante)datos.stock=Number(campo('prod-stock').value);else datos.idVariante=Number(campo('prod-variante').value);
        try{campo('form-producto').inert=true;if(!agregandoVariante)datos.imagenes=await guardarFotos();await solicitarTienda(agregandoVariante?`/admin/productos/${id}/variantes`:`/productos${id?'/'+id:''}`,{method:id&&!agregandoVariante?'PUT':'POST',body:datos});cerrarModal();await cargarProductos();}catch(error){errorProducto(error.message);}finally{b.disabled=false;campo('form-producto').inert=false;}};
    campo('guardar-ajuste').onclick=async e=>{const id=Number(campo('prod-variante').value),p=inventario.find(p=>p.idProducto===Number(campo('prod-id').value)),v=p.variantes.find(v=>v.idVariante===id),cambio=Number(campo('stock-cambio').value);
        if(!Number.isInteger(cambio)||cambio===0){alert('Indica unidades a añadir (positivo) o retirar (negativo).');return;}if(!confirm(`¿Aplicar ${cambio} unidades al stock ${v.stock}?`))return;
        e.target.disabled=true;try{await solicitarTienda(`/admin/variantes/${id}/stock`,{method:'PATCH',body:{cambio,stockAnterior:v.stock}});cerrarModal();await cargarProductos();}catch(error){alert(error.message);}finally{e.target.disabled=false;}};
    campo('estado-variante').onclick=async e=>{const id=Number(campo('prod-variante').value),p=inventario.find(p=>p.idProducto===Number(campo('prod-id').value)),v=p.variantes.find(v=>v.idVariante===id);
        e.target.disabled=true;try{await solicitarTienda(`/admin/variantes/${id}/activo`,{method:'PATCH',body:{activo:!v.activo}});cerrarModal();await cargarProductos();}catch(error){alert(error.message);}finally{e.target.disabled=false;}};
    try{const cats=await obtenerTienda('/categorias');campo('prod-categoria').innerHTML=cats.filter(c=>c.activo).map(c=>`<option>${escaparHTML(c.nombre)}</option>`).join('');}catch(e){campo('mensaje-gestion').textContent=e.message;}
    await cargarProductos();await cargarReportes();
});
