function renderizarResumen() {
    document.getElementById('checkout-items').innerHTML = carrito.map(item => `<div class="check-item">
        <div class="check-img-contenedor">${imagenProductoHTML(item)}<span class="check-cantidad">${item.cantidad}</span></div>
        <div class="check-info"><h4>${escaparHTML(item.nombre)}</h4><p>${escaparHTML(item.talla)} / ${escaparHTML(item.color)}</p></div>
        <div class="check-precio">S/ ${(item.precio * item.cantidad).toFixed(2)}</div></div>`).join('') || '<p>Tu carrito está vacío.</p>';
    const subtotal = carrito.reduce((suma, item) => suma + item.precio * item.cantidad, 0);
    document.getElementById('check-subtotal').textContent = `S/ ${subtotal.toFixed(2)}`;
    document.getElementById('check-total').textContent = `S/ ${subtotal.toFixed(2)}`;
    document.getElementById('check-envio').textContent = 'S/ 0.00';
    document.getElementById('fila-descuento').style.display = 'none';
}
document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('form-checkout');
    const boton = form.querySelector('button[type="submit"]');
    const pagos = document.querySelector('.opciones-pago');
    boton.disabled = true;
    if (!localStorage.getItem('token_monarca')) { location.href = paginaTienda('login.html'); return; }
    let enviando = false;
    const clavePendiente='monarca_pedido_pendiente';
    const cuentaActual=()=>{try{return JSON.parse(atob(localStorage.getItem('token_monarca').split('.')[1].replace(/-/g,'+').replace(/_/g,'/'))).sub;}catch{return localStorage.getItem('token_monarca');}};
    const pendiente=()=>{try{const p=JSON.parse(sessionStorage.getItem(clavePendiente));return p?.cuenta===cuentaActual()?p:null;}catch{return null;}};
    function mostrarPendiente() {
        boton.textContent='REINTENTAR PEDIDO PENDIENTE';
        form.querySelectorAll('input,select').forEach(c=>c.disabled=true);
        let aviso=document.getElementById('aviso-operacion');
        if(!aviso){aviso=document.createElement('p');aviso.id='aviso-operacion';aviso.setAttribute('role','status');boton.before(aviso);}
        aviso.textContent='Esta operación conserva los datos enviados. Si falla la conexión, reintenta o consulta Mis pedidos antes de iniciar otra compra.';
    }
    if(pendiente()) mostrarPendiente();
    async function confirmar(payload){
        sessionStorage.setItem(clavePendiente,JSON.stringify({cuenta:cuentaActual(),payload}));
        mostrarPendiente();
        let resultado;
        try{resultado=await solicitarTienda('/pedidos/procesar',{method:'POST',body:payload});}
        catch(error){if(error.status && error.status<500){sessionStorage.removeItem(clavePendiente);alert(error.message);location.reload();return;}throw error;}
        sessionStorage.removeItem(clavePendiente);
        carrito=[];localStorage.removeItem('monarca_carrito');
        alert('Pedido '+resultado.codigoPedido+' registrado. Total: S/ '+Number(resultado.total).toFixed(2)+'. Estado: '+resultado.estado+'. El pago se verifica manualmente.');
        location.href=paginaTienda('pedidos.html')+'?id='+resultado.idPedido;
    }
    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (enviando || boton.disabled || !form.reportValidity()) return;
        const token = localStorage.getItem('token_monarca');
        if (!token) { location.href = paginaTienda('login.html'); return; }
        enviando = true; boton.disabled = true;
        try {
            if(pendiente()){await confirmar(pendiente().payload);return;}
            const cambiado = await sincronizarCarrito();
            renderizarResumen();
            if (!carrito.length) throw new Error('Tu carrito está vacío o los artículos ya no tienen stock.');
            if (cambiado) throw new Error('Actualizamos precios o cantidades. Revisa el resumen y confirma nuevamente.');
            const seleccionado = form.querySelector('input[name="pago"]:checked');
            if (!seleccionado) throw new Error('Selecciona un método de pago.');
            const entrega=Object.fromEntries(['nombre','apellidos','correo','telefono','dni','direccion','distrito','referencia'].map(id=>[id,document.getElementById(id).value.trim()]));
            const payload = {
                idMetodoPago: Number(seleccionado.value), entrega, claveOperacion: crypto.randomUUID(),
                items: carrito.map(({idVariante, cantidad}) => ({idVariante, cantidad}))
            };
            await confirmar(payload);
        } catch (error) { alert(error.message); }
        finally { enviando = false; boton.disabled = !carrito.length && !pendiente(); }
    });
    if(pendiente()){boton.disabled=false;pagos.textContent='Se reintentará la operación anterior con sus mismos datos. También puedes revisar Mis pedidos.';return;}
    try {
        const [cambiado, metodos] = await Promise.all([sincronizarCarrito(), obtenerTienda('/metodos-pago', true)]);
        renderizarResumen();
        pagos.innerHTML = metodos.map(m => `<label class="opcion-radio"><input type="radio" name="pago" value="${m.idMetodoPago}" required>
            <span>${escaparHTML(m.nombre)}</span></label>`).join('') || '<p>No hay métodos de pago disponibles.</p>';
        if (cambiado) alert('Actualizamos el carrito con los precios y el stock disponibles. Revisa el resumen.');
        boton.disabled = !carrito.length || !metodos.length;
    } catch (error) { pagos.textContent = error.message + ' Recarga la página para reintentar.'; }
});
