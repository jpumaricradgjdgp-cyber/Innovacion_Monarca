let paginaPedidos = 0;
const esAdminPedidos = document.body.dataset.admin === 'true';
async function cargarPedidos() {
    const destino = document.getElementById('tabla-ventas');
    try {
        const pedidos = await solicitarTienda(`/pedidos${esAdminPedidos ? '' : '/mios'}?pagina=${paginaPedidos}`);
        destino.innerHTML = pedidos.map(p => `<tr><td>${escaparHTML(p.codigoPedido)}</td><td>${new Date(p.fechaPedido).toLocaleDateString()}</td>
            <td>${escaparHTML(p.cliente)}</td><td>S/ ${Number(p.total).toFixed(2)}</td><td>${escaparHTML(p.estado)}</td>
            <td><button class="btn-accion" data-pedido="${p.idPedido}">Ver detalle</button></td></tr>`).join('') || '<tr><td colspan="6">No hay pedidos en esta página.</td></tr>';
        document.getElementById('pedidos-anterior').disabled = paginaPedidos === 0;
        document.getElementById('pedidos-siguiente').disabled = pedidos.length < 20;
        document.getElementById('pedidos-pagina').textContent = `Página ${paginaPedidos+1}`;
    } catch(e) { destino.textContent = e.message; }
}
async function verPedido(id) {
    try {
        const p = await solicitarTienda(`/pedidos/${id}`);
        const panel = document.getElementById('detalle-pedido');
        panel.hidden = false;
        panel.innerHTML = `<h3>${escaparHTML(p.codigoPedido)}</h3><p>Estado: ${escaparHTML(p.estado)} · Total: S/ ${Number(p.total).toFixed(2)}</p>
            <h4>Entrega</h4><pre class="entrega">${escaparHTML(p.entrega || 'Sin datos de entrega')}</pre>
            <h4>Artículos</h4><ul>${p.items.map(i=>`<li>${escaparHTML(i.nombre)} — ${escaparHTML(i.talla)} / ${escaparHTML(i.color)} — ${i.cantidad} unidades · S/ ${Number(i.subtotal).toFixed(2)}</li>`).join('')}</ul>
            <h4>Pago</h4>${p.pagos.map(pago=>`<p>${escaparHTML(pago.metodo)}: ${escaparHTML(pago.estado)} · Referencia: ${escaparHTML(pago.referencia || 'Pendiente')}</p>`).join('')}
            ${p.estado==='PENDIENTE_PAGO' ? `<p>El pago se verifica manualmente. Si no continuarás con la compra, cancélala para liberar el stock.</p><button data-estado="CANCELADO" data-id="${id}">Cancelar pedido</button>` : ''}
            ${esAdminPedidos && p.estado==='PENDIENTE_PAGO' ? `<label>Referencia del pago verificado (obligatoria) <input id="referencia-pago" maxlength="100" required aria-describedby="ayuda-referencia"></label><p id="ayuda-referencia">Escribe el número de operación del pago que comprobaste. Para efectivo, usa el número de recibo. Confirma únicamente después de verificar que recibiste el dinero.</p><button data-estado="PAGADO" data-id="${id}">Confirmar pago recibido</button>` : ''}
            ${esAdminPedidos && p.estado==='PAGADO' ? `<button data-estado="ENVIADO" data-id="${id}">Marcar enviado</button>` : ''}
            ${esAdminPedidos && p.estado==='ENVIADO' ? `<button data-estado="ENTREGADO" data-id="${id}">Marcar entregado</button>` : ''}
            <p id="error-estado-pedido" role="alert" hidden></p><button id="cerrar-detalle">Cerrar detalle</button>`;
        panel.scrollIntoView({behavior:'smooth',block:'start'});
    } catch(e) { alert(e.message); }
}
document.addEventListener('DOMContentLoaded',()=>{
    if (!localStorage.getItem('token_monarca')) { location.href=paginaTienda('login.html'); return; }
    document.getElementById('pedidos-anterior').onclick=()=>{paginaPedidos--;cargarPedidos();};
    document.getElementById('pedidos-siguiente').onclick=()=>{paginaPedidos++;cargarPedidos();};
    document.getElementById('tabla-ventas').addEventListener('click',e=>{const b=e.target.closest('[data-pedido]');if(b)verPedido(b.dataset.pedido);});
    document.getElementById('detalle-pedido').addEventListener('click',async e=>{
        if(e.target.id==='cerrar-detalle'){e.currentTarget.hidden=true;return;}
        const b=e.target.closest('[data-estado]');if(!b || b.disabled)return;
        const aviso=document.getElementById('error-estado-pedido');
        aviso.hidden=true;aviso.textContent='';
        const referencia=document.getElementById('referencia-pago')?.value.trim();
        if(b.dataset.estado==='PAGADO' && (!referencia || referencia.length>100)){
            aviso.textContent='Indica la referencia del pago verificado (máximo 100 caracteres).';aviso.hidden=false;
            document.getElementById('referencia-pago').focus();return;
        }
        if(!confirm(b.dataset.estado==='CANCELADO'?'¿Cancelar el pedido y liberar sus existencias?':'¿Confirmas este cambio de estado?'))return;
        b.disabled=true;
        try { await solicitarTienda(`/pedidos/${b.dataset.id}/estado`,{method:'PATCH',body:{estado:b.dataset.estado,referencia}});
            await verPedido(b.dataset.id);await cargarPedidos();if(esAdminPedidos)await cargarReportes();
        }catch(error){aviso.textContent=error.message;aviso.hidden=false;}finally{b.disabled=false;}
    });
    cargarPedidos();
    const id=new URLSearchParams(location.search).get('id');if(id && /^\d+$/.test(id))verPedido(id);
});
