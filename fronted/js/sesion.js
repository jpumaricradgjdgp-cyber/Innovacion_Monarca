// El JWT contiene el nombre registrado; nunca se deriva del correo.
document.addEventListener('DOMContentLoaded', () => {
    const token=localStorage.getItem('token_monarca');
    const link=document.querySelector('.iconos-usuario a[href$="login.html"]')||document.getElementById('link-perfil');
    if(!token||!link)return;
    try {
        const segmento=token.split('.')[1].replaceAll('-','+').replaceAll('_','/');
        const payload=JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(segmento),c=>c.charCodeAt(0))));
        if(payload.exp && payload.exp*1000<=Date.now())return;
        const nombre=typeof payload.nombre==='string'?payload.nombre.trim():'';
        const saludo=document.createElement('span');saludo.id='user-name-display';
        saludo.textContent=nombre?'Hola, '+nombre:'Mi cuenta';saludo.title=saludo.textContent;
        link.before(saludo);link.setAttribute('aria-label','Mi cuenta');
        const salir=document.createElement('button');salir.type='button';salir.id='btn-logout';salir.textContent='Salir';salir.setAttribute('aria-label','Cerrar sesión');
        salir.onclick=()=>{localStorage.removeItem('token_monarca');localStorage.removeItem('rol_monarca');location.href=new URL('index.html',TIENDA_RAIZ).href;};
        link.parentElement.append(salir);
    } catch { /* Un token inválido se verificará en el servidor al acceder. */ }
});
