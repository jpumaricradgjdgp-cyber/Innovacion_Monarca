// Ejecutar con Node y Playwright disponible (NODE_PATH o instalación local).
const { chromium } = require('playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const raiz = path.resolve(__dirname, '..');
const producto = {idProducto:1,nombre:'Crop Top Ana',categoria:'Tops',descripcion:'Top de prueba',marca:'Monarca',precioBase:59,destacado:true,
    imagen:'https://example.supabase.co/storage/v1/object/public/productos/ana.jpg', variantes:[
        {idVariante:10,sku:'ANA-S-NEG',talla:'S',color:'Negro',precio:65,stock:2},
        {idVariante:11,sku:'ANA-S-BLA',talla:'S',color:'Blanco',precio:70,stock:3},
        {idVariante:12,sku:'ANA-M-NEG',talla:'M',color:'Negro',precio:65,stock:0}]};
(async () => {
    const server = http.createServer((req,res) => {
        const archivo = path.resolve(raiz, '.' + new URL(req.url,'http://localhost').pathname);
        if (!archivo.startsWith(raiz + path.sep)) {res.writeHead(403).end();return;}
        fs.readFile(archivo,(err,data) => {
            if(err){res.writeHead(404).end();return;}
            res.setHeader('Content-Type', {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css'}[path.extname(archivo)] || 'application/octet-stream');
            res.end(data);
        });
    });
    await new Promise(r=>server.listen(0,'127.0.0.1',r));
    const browser = await chromium.launch({headless:true, ...(process.env.BROWSER_EXECUTABLE ? {executablePath:process.env.BROWSER_EXECUTABLE} : {})});
    try {
        const page = await browser.newPage();
        const errores = [], dialogs = [], pedidos = [];
        page.on('pageerror',e=>errores.push(e.message));
        page.on('dialog', async d=>{dialogs.push(d.message());await d.accept();});
        await page.route('http://localhost:8080/api/**', async route => {
            const req=route.request(); const ruta=new URL(req.url()).pathname;
            let data;
            if(ruta==='/api/productos') data=[producto];
            else if(ruta==='/api/productos/1') data=producto;
            else if(ruta==='/api/metodos-pago') data=[{idMetodoPago:37,nombre:'Yape',codigo:'YAPE'}];
            else if(ruta==='/api/pedidos/mios') data=[];
            else if(ruta==='/api/pedidos/1') data={idPedido:1,codigoPedido:'MON-TEST',total:200,estado:'PENDIENTE_PAGO',items:[],pagos:[]};
            else if(ruta==='/api/pedidos/procesar') {pedidos.push(req.postDataJSON());data={idPedido:1,codigoPedido:'MON-TEST',total:200,estado:'PENDIENTE_PAGO'};}
            else {await route.fulfill({status:404,body:'{}'});return;}
            await route.fulfill({contentType:'application/json',body:JSON.stringify(data)});
        });
        await page.route('https://**', r=>r.abort());
        const base=`http://127.0.0.1:${server.address().port}`;
        await page.goto(base+'/Paginas/Tops.html');
        await page.locator('.producto-card').waitFor();
        assert.equal(await page.locator('.producto-card h3').textContent(),'Crop Top Ana');
        // Talla y color deben coincidir en una misma variante con stock.
        await page.locator('.filtro-grupo').nth(2).locator('.filtro-item').click();
        await page.locator('input[value="blanco"]').check();
        await page.locator('.filtro-grupo').nth(3).locator('.filtro-item').click();
        await page.locator('input[value="m"]').check();
        assert.equal(await page.locator('.producto-card').count(), 0);
        await page.locator('input[value="m"]').uncheck();
        await page.locator('input[value="blanco"]').uncheck();
        await page.locator('.producto-card').hover();
        await page.waitForFunction(() => getComputedStyle(document.querySelector('.producto-hover-info')).opacity === '1');
        await page.locator('.producto-card .btn-agregar').click();
        await page.waitForURL('**/producto.html?id=1');
        await page.locator('#contenedor-tallas button').first().waitFor();
        assert.equal(await page.locator('#contenedor-tallas button').count(),2);
        assert.equal(await page.locator('.btn-agregar').isDisabled(),true);
        await page.getByRole('button',{name:'S / Negro',exact:true}).click();
        assert.equal(await page.locator('#prod-precio').textContent(),'S/ 65.00');
        await page.locator('.btn-agregar').click();
        await page.locator('.btn-agregar').click();
        await page.locator('.btn-agregar').click();
        assert(dialogs.some(d=>d.includes('stock disponible')));
        await page.getByRole('button',{name:'S / Blanco',exact:true}).click();
        await page.locator('.btn-agregar').click();
        let carrito=await page.evaluate(()=>JSON.parse(localStorage.getItem('monarca_carrito')));
        assert.equal(carrito.length,2); assert.equal(carrito[0].cantidad,2);assert.equal(carrito[1].cantidad,1);
        assert.equal(carrito[0].precio,65); assert.equal(carrito[1].precio,70);
        assert.deepEqual(Object.keys(carrito[0]).sort(),['idProducto','idVariante','nombre','sku','talla','color','precio','cantidad','imagen','stockDisponible'].sort());
        await page.evaluate(()=>localStorage.setItem('token_monarca','token-exclusivo-de-fixture'));
        await page.goto(base+'/Paginas/checkout.html');
        await page.locator('input[name="pago"]').waitFor();
        assert.equal(await page.locator('#check-total').textContent(),'S/ 200.00');
        for(const [id,val] of Object.entries({nombre:'Ana',apellidos:'Prueba',correo:'ana@example.test',telefono:'900000000',dni:'12345678',direccion:'Calle de prueba 123',referencia:'Puerta'})) await page.locator('#'+id).fill(val);
        await page.locator('#distrito').selectOption('surco');
        await page.locator('input[name="pago"]').check();
        await page.locator('.btn-pagar').click();
        await page.waitForURL('**/pedidos.html?id=1',{waitUntil:'domcontentloaded'});
        assert.equal(pedidos.length,1);assert.equal(pedidos[0].idMetodoPago,37);
        assert.deepEqual(pedidos[0].items,[{idVariante:10,cantidad:2},{idVariante:11,cantidad:1}]);
        assert.equal(pedidos[0].total,undefined);assert.equal(pedidos[0].idEstatusEnvio,undefined);
        assert.equal(pedidos[0].entrega.direccion,'Calle de prueba 123');assert.match(pedidos[0].claveOperacion,/^[0-9a-f-]{36}$/);
        assert.equal(await page.evaluate(()=>localStorage.getItem('monarca_carrito')),null);
        // Carrito heredado/corrupto se descarta con aviso, sin inventar una variante.
        await page.evaluate(()=>localStorage.setItem('monarca_carrito',JSON.stringify([{idProducto:1,talla:'S',cantidad:1}])));
        await page.goto(base+'/Paginas/Tops.html');
        assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('monarca_carrito'))),[]);
        assert(dialogs.some(d=>d.includes('compatibles')));
        await page.evaluate(()=>localStorage.setItem('monarca_carrito','{invalid'));
        await page.reload();
        assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('monarca_carrito'))),[]);
        assert.deepEqual(errores,[]);
        console.log('PASS: catálogo → detalle → dos colores misma talla → límite de stock → checkout → pedido por idVariante.');
        console.log('PASS: contrato de carrito, precios de variante, pago id 37, dirección, total servidor, migración de carrito y JSON corrupto.');
    } finally {await browser.close(); await new Promise(r=>server.close(r));}
})().catch(error=>{console.error(error);process.exitCode=1;});
