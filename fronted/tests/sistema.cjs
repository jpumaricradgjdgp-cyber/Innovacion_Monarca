const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('node:assert/strict');
(async()=>{
 const raiz=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{const f=path.resolve(raiz,'.'+new URL(req.url,'http://localhost').pathname);
  if(!f.startsWith(raiz+path.sep)){res.writeHead(403).end();return;}fs.readFile(f,(e,d)=>{if(e){res.writeHead(404).end();return;}
  res.setHeader('Content-Type',{'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css'}[path.extname(f)]||'application/octet-stream');res.end(d);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({headless:true,...(process.env.BROWSER_EXECUTABLE?{executablePath:process.env.BROWSER_EXECUTABLE}:{})});
 try{
  const page=await browser.newPage();const errors=[],writes=[],dialogs=[];
  page.on('pageerror',e=>errors.push(e.message));page.on('dialog',async d=>{dialogs.push(d.message());await d.accept();});
  await page.route('https://**',r=>r.abort());
  const producto={idProducto:1,nombre:'Top de prueba',categoria:'Tops',precioBase:25,activo:true,imagen:null,variantes:[{idVariante:10,sku:'TOP-S',talla:'S',color:'Negro',precio:25,stock:5,activo:true}]};
  const pedido={idPedido:3,codigoPedido:'MON-PRUEBA',cliente:'ana@example.test',fechaPedido:'2026-09-25T10:00:00Z',estado:'PENDIENTE_PAGO',total:50,entrega:'Ana Prueba\nCalle 123',items:[{nombre:'Top de prueba',talla:'S',color:'Negro',cantidad:2,subtotal:50}],pagos:[{metodo:'Pago manual',estado:'PENDIENTE',referencia:null}]};
  let fallarCompra=true, fallarEstado=true;
  await page.route('http://localhost:8080/api/**',async r=>{const req=r.request(),u=new URL(req.url()),ruta=u.pathname;let data;
   if(req.method()!=='GET'){
    const body=req.postDataJSON();writes.push({ruta,method:req.method(),body});
    if(ruta==='/api/pedidos/procesar') {if(fallarCompra){fallarCompra=false;await r.abort();return;}data={idPedido:3,codigoPedido:'MON-PRUEBA',estado:pedido.estado,total:50};}
    else if(ruta==='/api/pedidos/3/estado'){if(fallarEstado){fallarEstado=false;await r.fulfill({status:409,contentType:'application/json',body:JSON.stringify({error:'Conflicto de prueba: incidente de prueba'})});return;}pedido.estado=body.estado;if(body.estado==='CANCELADO')pedido.pagos[0].estado='CANCELADO';data=pedido;}
    else if(ruta==='/api/admin/variantes/10/stock'){producto.variantes[0].stock+=body.cambio;data=null;}
    else if(ruta==='/api/admin/productos/1/activo'){producto.activo=body.activo;data=null;}
    else data=null;
   }else if(ruta==='/api/admin/productos'||ruta==='/api/productos')data=[producto];
   else if(ruta==='/api/categorias')data=[{nombre:'Tops',activo:true}];
   else if(ruta==='/api/admin/reportes')data={meses:[{anio:2026,mes:9,clientes:1,total:50}],categorias:[{categoria:'Tops',unidades:2,total:50}],estados:[{estado:'PENDIENTE_PAGO',cantidad:1,total:50}]};
   else if(ruta==='/api/pedidos'||ruta==='/api/pedidos/mios')data=[pedido];
   else if(ruta==='/api/pedidos/3')data=pedido;
   else if(ruta==='/api/metodos-pago')data=[{idMetodoPago:1,nombre:'Manual'}];
   else {await r.fulfill({status:404,body:'{}'});return;}
   await r.fulfill({contentType:'application/json',body:JSON.stringify(data)});
  });
  const base=`http://127.0.0.1:${server.address().port}`;
  await page.addInitScript(()=>localStorage.setItem('token_monarca','fixture-token'));
  await page.goto(base+'/Paginas/admin.html',{waitUntil:'domcontentloaded'});
  await page.getByRole('button',{name:'Editar',exact:true}).click();await page.locator('#prod-nombre').fill('Top actualizado');
  await page.getByRole('button',{name:'Guardar Producto'}).click();await page.waitForFunction(()=>document.getElementById('modalCRUD').style.display==='none');
  const edit=writes.find(w=>w.ruta==='/api/productos/1');assert.equal(edit.method,'PUT');assert.equal(edit.body.stock,undefined);assert.equal(edit.body.nombre,'Top actualizado');
  await page.getByRole('button',{name:'Editar',exact:true}).click();await page.locator('#stock-cambio').fill('2');await page.getByRole('button',{name:'Aplicar ajuste de stock'}).click();
  await page.waitForFunction(()=>document.getElementById('modalCRUD').style.display==='none');assert.deepEqual(writes.find(w=>w.ruta.endsWith('/stock')).body,{cambio:2,stockAnterior:5});
  await page.getByRole('button',{name:'Añadir variante',exact:true}).click();await page.locator('#prod-talla').fill('S-M-L');assert.equal(await page.locator('#prod-talla').evaluate(e=>e.checkValidity()),false);await page.locator('#prod-talla').fill('M');await page.locator('#prod-color').fill('Blanco');await page.locator('#prod-stock').fill('3');
  await page.getByRole('button',{name:'Guardar variante',exact:true}).click();await page.waitForFunction(()=>document.getElementById('modalCRUD').style.display==='none');assert.equal(writes.find(w=>w.ruta.endsWith('/variantes')).body.stock,3);
  await page.getByRole('button',{name:'Desactivar',exact:true}).click();await page.getByRole('button',{name:'Reactivar',exact:true}).waitFor();
  assert.match(await page.locator('#reporte-meses').textContent(),/2026/);
  await page.locator('[data-pedido="3"]').click();
  await page.locator('#referencia-pago').fill('   ');
  await page.getByRole('button',{name:'Confirmar pago recibido',exact:true}).click();
  assert.equal(writes.filter(w=>w.ruta.endsWith('/estado')).length,0);
  assert.match(await page.locator('#error-estado-pedido').textContent(),/Indica la referencia/);
  await page.locator('#referencia-pago').fill(' OP-123 ');
  await page.getByRole('button',{name:'Confirmar pago recibido',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('error-estado-pedido').textContent.includes('incidente de prueba'));
  assert.equal(await page.locator('#referencia-pago').inputValue(),' OP-123 ');
  await page.getByRole('button',{name:'Confirmar pago recibido',exact:true}).click();
  await page.getByRole('button',{name:'Marcar enviado',exact:true}).waitFor();
  assert.equal(writes.filter(w=>w.ruta.endsWith('/estado')).at(-1).body.referencia,'OP-123');
  pedido.estado='PENDIENTE_PAGO';
  if(process.env.MONARCA_SCREENSHOT_DIR)await page.screenshot({path:path.join(process.env.MONARCA_SCREENSHOT_DIR,'Monarca-admin-sistema.png'),fullPage:true});
  await page.goto(base+'/Paginas/pedidos.html?id=3',{waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'Cancelar pedido',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('detalle-pedido').textContent.includes('CANCELADO'));assert.equal(writes.filter(w=>w.ruta.endsWith('/estado')).at(-1).body.estado,'CANCELADO');
  if(process.env.MONARCA_SCREENSHOT_DIR){await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(process.env.MONARCA_SCREENSHOT_DIR,'Monarca-pedidos-movil.png'),fullPage:true});}
  const clave=require('crypto').randomUUID();
  await page.evaluate(clave=>sessionStorage.setItem('monarca_pedido_pendiente',JSON.stringify({cuenta:'fixture-token',payload:{claveOperacion:clave,idMetodoPago:1,entrega:{nombre:'Ana'},items:[{idVariante:10,cantidad:2}]}})),clave);
  await page.goto(base+'/Paginas/checkout.html',{waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'REINTENTAR PEDIDO PENDIENTE'}).click();
  await page.waitForFunction(()=>!document.querySelector('.btn-pagar').disabled);
  await page.getByRole('button',{name:'REINTENTAR PEDIDO PENDIENTE'}).click();await page.waitForURL('**/pedidos.html?id=3',{waitUntil:'domcontentloaded'});
  const compras=writes.filter(w=>w.ruta==='/api/pedidos/procesar');assert.equal(compras.length,2);assert.equal(compras[0].body.claveOperacion,clave);assert.deepEqual(compras[0].body,compras[1].body);
  assert.equal(await page.evaluate(()=>sessionStorage.getItem('monarca_pedido_pendiente')),null);assert.deepEqual(errors,[]);
  console.log('PASS: administración, edición sin stock, ajuste explícito, nueva variante, reactivación, reportes reales, detalle/cancelación y reintento con la misma operación.');
 }finally{await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
