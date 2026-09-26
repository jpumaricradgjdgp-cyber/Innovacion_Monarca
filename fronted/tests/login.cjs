const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('node:assert/strict');
(async()=>{
 const root=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://localhost').pathname);
  if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return;}
  fs.readFile(file,(err,data)=>{if(err){res.writeHead(404).end();return;}
   res.setHeader('Content-Type', {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css'}[path.extname(file)]||'application/octet-stream');res.end(data);});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({headless:true,...(process.env.BROWSER_EXECUTABLE?{executablePath:process.env.BROWSER_EXECUTABLE}:{})});
 try{
  const page=await browser.newPage();const errors=[],requests=[];
  let modo='normal';
  page.on('pageerror',e=>errors.push(e.message));
  await page.route('https://**',r=>r.abort());
  await page.route('http://localhost:8080/api/auth/**',async route=>{
   const tipo=new URL(route.request().url()).pathname.split('/').pop();
   const datos=route.request().postDataJSON();requests.push({tipo,datos});
   if(modo==='sinconexion'){await route.abort();return;}
   if(modo==='duplicado'){await route.fulfill({status:400,body:'El correo ya está registrado'});return;}
   if(modo==='sinSMTP'){await route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({error:'La recuperación por correo todavía no está configurada.'})});return;}
   const respuesta=tipo==='login'?{token:require('crypto').randomBytes(32).toString('hex'),rol:'ROLE_CLIENTE'}:
       {mensaje:tipo==='olvide-password'?'Si existe una cuenta activa, recibirás un enlace.':'Contraseña actualizada. Vuelve a iniciar sesión.'};
   await route.fulfill({status:tipo==='registro'?201:200,contentType:'application/json',body:JSON.stringify(respuesta)});
  });
  const url=`http://127.0.0.1:${server.address().port}/Paginas/login.html`;
  await page.goto(url);
  await page.getByRole('link',{name:'Crear cuenta',exact:true}).click();
  await page.locator('#registro-nombre').fill('Ana');await page.locator('#registro-apellido').fill('Pérez');
  await page.locator('#registro-email').fill('ana@example.test');
  const password=require('crypto').randomBytes(12).toString('hex');
  await page.locator('#registro-password').fill(password);await page.locator('#registro-confirmar').fill(password+'x');
  await page.getByRole('button',{name:'Crear mi cuenta'}).click();
  assert.match(await page.locator('#mensaje-cuenta').textContent(),/no coinciden/);assert.equal(requests.length,0);
  await page.locator('#registro-confirmar').fill(password);
  modo='duplicado';await page.getByRole('button',{name:'Crear mi cuenta'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('ya está registrado'));
  modo='normal';await page.getByRole('button',{name:'Crear mi cuenta'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('Cuenta creada'));
  assert.equal(await page.locator('#usuario').inputValue(),'ana@example.test');
  assert.deepEqual(Object.keys(requests.at(-1).datos).sort(),['apellido','email','nombre','password']);
  await page.getByRole('link',{name:'Olvidé mi contraseña'}).click();
  await page.locator('#recuperar-email').fill('ana@example.test');
  modo='sinSMTP';await page.getByRole('button',{name:'Enviar enlace'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('no está configurada'));
  modo='normal';await page.getByRole('button',{name:'Enviar enlace'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('Si existe'));
  const token=require('crypto').randomBytes(32).toString('base64url');
  await page.goto(url+'#restablecer='+token);
  await page.waitForFunction(()=>location.hash==='#restablecer');
  assert.equal(new URL(page.url()).hash,'#restablecer');
  await page.locator('#nueva-password').fill(password);await page.locator('#nueva-confirmar').fill(password);
  await page.getByRole('button',{name:'Guardar contraseña'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('actualizada'));
  assert.equal(requests.at(-1).datos.token,token);
  await page.goto(url+'#restablecer');
  assert.equal(await page.getByRole('button',{name:'Guardar contraseña'}).isDisabled(),true);
  await page.goto(url);
  await page.locator('#usuario').fill('ana@example.test');await page.locator('#password').fill(password);
  modo='sinconexion';await page.getByRole('button',{name:'Iniciar sesión'}).click();
  await page.waitForFunction(()=>document.getElementById('mensaje-cuenta').textContent.includes('conectar'));
  assert.equal(await page.getByRole('button',{name:'Iniciar sesión'}).isEnabled(),true);
  if(process.env.PREVIEW_PATH){
   await page.goto(url);await page.screenshot({path:process.env.PREVIEW_PATH,fullPage:true});
   await page.setViewportSize({width:390,height:844});await page.goto(url+'#registro');
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   await page.screenshot({path:process.env.PREVIEW_PATH.replace('.png','-mobile.png'),fullPage:true});
  }
  assert.deepEqual(errors,[]);
  console.log('PASS: registro, validación, correo duplicado, recuperación, SMTP ausente, nueva contraseña, token retirado de URL, enlace ausente y error de conexión.');
 }finally{await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
