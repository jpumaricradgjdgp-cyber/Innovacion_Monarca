let fotosEditor=[];
let carpetaFotos='';
function iniciarGaleria(fotos) {
    carpetaFotos='';
    fotosEditor.forEach(f=>{if(f.preview)URL.revokeObjectURL(f.preview);});
    fotosEditor=fotos.map(f=>({...f}));
    document.getElementById('prod-archivos').value='';
    document.getElementById('estado-fotos').textContent='';
    pintarGaleria();
}
function pintarGaleria() {
    const contenedor=document.getElementById('galeria-editor');contenedor.replaceChildren();
    fotosEditor.forEach((foto,i)=>{
        const tarjeta=document.createElement('div');tarjeta.className='foto-editor';
        const imagen=document.createElement('img');imagen.src=foto.preview||imagenHTTPS(foto.url);imagen.alt=foto.textoAlternativo||'Vista del producto';
        const etiqueta=document.createElement('strong');etiqueta.textContent=i===0?'Portada':`Foto ${i+1}`;
        const texto=document.createElement('input');texto.type='text';texto.maxLength=200;texto.value=foto.textoAlternativo||'';
        texto.placeholder=i===0?'Vista frontal':i===1?'Vista posterior':'Detalle de la prenda';texto.setAttribute('aria-label',`Descripción de foto ${i+1}`);
        texto.oninput=()=>{foto.textoAlternativo=texto.value;imagen.alt=texto.value;};
        const acciones=document.createElement('div');
        const accion=(nombre,fn,disabled=false)=>{const b=document.createElement('button');b.type='button';b.textContent=nombre;b.disabled=disabled;b.onclick=fn;acciones.append(b);};
        accion('Hacer portada',()=>{fotosEditor.splice(i,1);fotosEditor.unshift(foto);pintarGaleria();},i===0);
        accion('Mover antes',()=>{[fotosEditor[i-1],fotosEditor[i]]=[foto,fotosEditor[i-1]];pintarGaleria();},i===0);
        accion('Quitar',()=>{if(foto.preview)URL.revokeObjectURL(foto.preview);fotosEditor.splice(i,1);pintarGaleria();});
        tarjeta.append(imagen,etiqueta,texto,acciones);contenedor.append(tarjeta);
    });
}
async function guardarFotos() {
    const estado=document.getElementById('estado-fotos');
    // Compatibilidad con quien pega una URL y pulsa Guardar directamente.
    const url=document.getElementById('prod-img');
    if(url.value.trim()){agregarFotoURL(url.value);url.value='';}
    if(!carpetaFotos) {
        // Reutiliza la carpeta de una foto guardada al volver a editar el producto.
        for(const foto of fotosEditor) {
            try {
                const ruta=new URL(foto.url).pathname.match(/^\/storage\/v1\/object\/public\/[^/]+\/catalogo\/([a-z0-9][a-z0-9-]{0,99})\/[^/]+$/);
                if(ruta && ruta[1]!=='sin-clasificar'){carpetaFotos=ruta[1];break;}
            } catch { /* Las fotos locales todavía no tienen URL. */ }
        }
        if(!carpetaFotos) {
            const nombre=document.getElementById('prod-nombre').value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase()
                .replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'').slice(0,45)||'producto';
            const id=document.getElementById('prod-id').value;
            carpetaFotos=nombre+'-'+(/^\d+$/.test(id)?id:crypto.randomUUID());
        }
    }
    for(let i=0;i<fotosEditor.length;i++) {
        const foto=fotosEditor[i];if(!foto.archivo)continue;
        estado.textContent=`Subiendo foto ${i+1} de ${fotosEditor.length}…`;
        const datos=new FormData();datos.append('archivo',foto.archivo);
        datos.append('carpeta',carpetaFotos);
        const respuesta=await fetch(TIENDA_API+'/admin/imagenes',{method:'POST',headers:{Authorization:'Bearer '+localStorage.getItem('token_monarca')},body:datos});
        let contenido;try{contenido=await respuesta.json();}catch{contenido={};}
        if(!respuesta.ok)throw new Error(contenido.error||`No se pudo subir la foto ${i+1}. Inténtalo de nuevo.`);
        foto.url=contenido.url;foto.archivo=null; // Los reintentos no vuelven a subir las fotos ya recibidas.
    }
    estado.textContent='Fotos listas. Guardando producto…';
    return fotosEditor.map((foto,i)=>({url:foto.url,textoAlternativo:foto.textoAlternativo||(['Vista frontal','Vista posterior'][i]||`Detalle ${i+1}`)}));
}
function agregarFotoURL(valor) {
    if(fotosEditor.length>=8)throw new Error('Puedes añadir hasta 8 fotos.');
    const url=imagenHTTPS(valor.trim());if(!url)throw new Error('Usa una URL HTTPS válida.');
    fotosEditor.push({url,textoAlternativo:''});pintarGaleria();
}
document.addEventListener('DOMContentLoaded',()=>{
    document.getElementById('prod-archivos').addEventListener('change',e=>{
        const archivos=[...e.target.files];const estado=document.getElementById('estado-fotos');
        if(archivos.length+fotosEditor.length>8){estado.textContent='Puedes añadir hasta 8 fotos por producto.';e.target.value='';return;}
        if(archivos.some(f=>!['image/jpeg','image/png'].includes(f.type)||f.size>5*1024*1024)){
            estado.textContent='Selecciona fotos JPG o PNG de hasta 5 MB cada una.';e.target.value='';return;
        }
        archivos.forEach(archivo=>fotosEditor.push({archivo,preview:URL.createObjectURL(archivo),textoAlternativo:''}));
        estado.textContent=`${fotosEditor.length} fotos seleccionadas. Se subirán al guardar.`;e.target.value='';pintarGaleria();
    });
    document.getElementById('agregar-url').onclick=()=>{try{const input=document.getElementById('prod-img');agregarFotoURL(input.value);input.value='';}catch(e){document.getElementById('estado-fotos').textContent=e.message;}};
});
