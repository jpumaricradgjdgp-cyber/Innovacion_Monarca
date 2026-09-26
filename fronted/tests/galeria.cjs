const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('node:assert/strict');
(async()=>{
 const raiz=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{const f=path.resolve(raiz,'.'+decodeURIComponent(new URL(req.url,'http://localhost').pathname));
  if(!f.startsWith(raiz+path.sep)){res.writeHead(403).end();return;}fs.readFile(f,(e,d)=>{if(e){res.writeHead(404).end();return;}
   res.setHeader('Content-Type',{'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css','.png':'image/png'}[path.extname(f)]||'application/octet-stream');res.end(d);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({headless:true,...(process.env.BROWSER_EXECUTABLE?{executablePath:process.env.BROWSER_EXECUTABLE}:{})});
 try {
  const page=await browser.newPage(),errores=[];page.on('pageerror',e=>errores.push(e.message));
  page.on('dialog',async d=>{throw new Error('Diálogo inesperado: '+d.message());});
  const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=','base64');
  await page.route('https://**',r=>r.request().resourceType()==='image'?r.fulfill({contentType:'image/png',body:fs.readFileSync(path.join(raiz,'img/casaca_denim_luna.jpg'))}):r.abort());
  let subidas=0,guardado;const carpetas=[];
  const p={idProducto:1,nombre:'Blusa de algodón',descripcion:'Algodón suave',categoria:'Tops',precioBase:49,imagen:'https://example.test/frontal.png',activo:true,
    imagenes:[{url:'https://example.test/frontal.png',textoAlternativo:'Frontal'},{url:'https://example.test/posterior.png',textoAlternativo:'Posterior'}],
    variantes:[{idVariante:1,talla:'S-M-L',color:'Crema',precio:49,stock:5,activo:true}]};
  await page.route('http://localhost:8080/api/**',async r=>{const ruta=new URL(r.request().url()).pathname;let data;
   if(ruta==='/api/admin/imagenes'){subidas++;assert.match(r.request().headers()['content-type'],/^multipart\/form-data/);const carpeta=r.request().postDataBuffer().toString().match(/name="carpeta"\r\n\r\n([^\r]+)\r\n/)[1];carpetas.push(carpeta);data={url:`https://prueba.supabase.co/storage/v1/object/public/productos/catalogo/${carpeta}/subida-${subidas}.png`};}
   else if(r.request().method()==='PUT'){guardado=r.request().postDataJSON();data=null;}
   else if(ruta==='/api/admin/productos')data=[p];
   else if(ruta==='/api/productos')data=Array.from({length:25},(_,i)=>({...p,idProducto:i+1}));
   else if(ruta==='/api/productos/1')data=p;
   else if(ruta==='/api/categorias')data=[{nombre:'Tops',activo:true}];
   else if(ruta==='/api/admin/reportes')data={meses:[],categorias:[],estados:[]};
   else if(ruta==='/api/pedidos')data=[];
   else data=[];
   await r.fulfill({contentType:'application/json',body:JSON.stringify(data)});
  });
  const base=`http://127.0.0.1:${server.address().port}`;
  const token='x.'+Buffer.from(JSON.stringify({sub:'correo@example.test',nombre:'María José',exp:4102444800})).toString('base64url')+'.x';
  await page.addInitScript(t=>localStorage.setItem('token_monarca',t),token);
  await page.goto(base+'/Paginas/admin.html');await page.getByRole('button',{name:'Editar S-M-L / Crema',exact:true}).click();assert.equal(await page.getByLabel('Talla (solo una)',{exact:true}).inputValue(),'S-M-L');await page.getByLabel('Talla (solo una)',{exact:true}).fill('S');
  await page.setViewportSize({width:390,height:844});await page.locator('.modal-content').evaluate(e=>e.scrollTop=0);await page.screenshot({path:path.resolve(process.env.MONARCA_SCREENSHOT_DIR||'.','Monarca-admin-formulario.png')});assert.equal(await page.locator('#prod-descripcion').inputValue(),'Algodón suave');
  await page.locator('#prod-descripcion').fill('Tela suave. Lavado a mano.');
  await page.locator('#prod-archivos').setInputFiles([{name:'detalle.png',mimeType:'image/png',buffer:png},{name:'etiqueta.png',mimeType:'image/png',buffer:png}]);
  assert.equal(await page.locator('.foto-editor').count(),4);
  await page.getByRole('textbox',{name:'Descripción de foto 2',exact:true}).fill('Parte posterior');
  await page.locator('.foto-editor').nth(1).getByRole('button',{name:'Hacer portada'}).click();
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:path.resolve(process.env.MONARCA_SCREENSHOT_DIR||'.','Monarca-editor-fotos.png'),fullPage:true});
  await page.getByRole('button',{name:'Guardar Producto',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('modalCRUD').style.display==='none');
  assert.equal(guardado.idVariante,1);assert.equal(guardado.talla,'S');assert.equal(guardado.stock,undefined);assert.equal(subidas,2);assert.deepEqual(carpetas,['blusa-de-algodon-1','blusa-de-algodon-1']);assert.equal(guardado.imagenes.length,4);assert.equal(guardado.imagenes[0].textoAlternativo,'Parte posterior');assert.equal(guardado.descripcion,'Tela suave. Lavado a mano.');
  for(const ancho of [360,390,768,1024,1440]){
   await page.setViewportSize({width:ancho,height:900});await page.goto(base+'/Paginas/Tops.html');
   await page.locator('.paginador').waitFor();assert.equal(await page.locator('#user-name-display').textContent(),'Hola, María José');
   const desborde=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);assert.equal(desborde,false,`Desborde a ${ancho}px`);
   const medidas=await page.locator('.paginador').evaluate(e=>({ancho:e.clientWidth,alto:e.clientHeight}));assert.ok(medidas.ancho>250);
   if(ancho===390){await page.locator('.menu-icon').click();assert.equal(await page.locator('.navbar').isVisible(),true);await page.screenshot({path:path.resolve(process.env.MONARCA_SCREENSHOT_DIR||'.','Monarca-menu-movil.png')});await page.locator('.menu-icon').click();}
   await page.getByRole('button',{name:'Siguiente',exact:true}).click();assert.match(await page.locator('.paginador').textContent(),/Página 2 de 2/);
   if(ancho===390)await page.locator('.paginador').screenshot({path:path.resolve(process.env.MONARCA_SCREENSHOT_DIR||'.','Monarca-paginacion.png')});
  }
  await page.setViewportSize({width:390,height:844});await page.goto(base+'/Paginas/producto.html?id=1');
  await page.getByRole('button',{name:'Posterior',exact:true}).click();assert.match(await page.locator('#img-principal').getAttribute('src'),/posterior/);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
  await page.screenshot({path:path.resolve(process.env.MONARCA_SCREENSHOT_DIR||'.','Monarca-galeria-movil.png'),fullPage:true});
  assert.deepEqual(errores,[]);console.log('Galería, carga múltiple, nombre, paginación y 5 anchos: OK');
 } finally {await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
