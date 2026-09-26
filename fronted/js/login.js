document.addEventListener('DOMContentLoaded', () => {
    const mensaje = document.getElementById('mensaje-cuenta');
    const formLogin = document.getElementById('form-login');
    const formRegistro = document.getElementById('form-registro');
    let ocupado = false;
    // El enlace usa un fragmento; retirarlo evita conservarlo en el historial visible.
    let tokenRecuperacion = null;

    function avisar(texto, error = false) {
        mensaje.textContent = texto;
        mensaje.hidden = !texto;
        mensaje.classList.toggle('error', error);
    }
    function mostrarPanel() {
    if (location.hash.startsWith('#restablecer=')) {
        tokenRecuperacion = location.hash.slice('#restablecer='.length);
        history.replaceState(null, '', '#restablecer');
    }

        const vista = ['registro', 'recuperar', 'restablecer'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'login';
        const titulos = {login: ['Iniciar sesión', 'Ingresa a tu cuenta Monarca'], registro: ['Crear cuenta', 'Regístrate para comprar en Monarca'], recuperar: ['Recuperar mi cuenta', 'Te ayudamos a volver a ingresar'], restablecer: ['Nueva contraseña', 'Elige una contraseña para tu cuenta']};
        for (const nombre of ['login', 'registro', 'recuperar', 'restablecer']) document.getElementById(`panel-${nombre}`).hidden = nombre !== vista;
        document.getElementById('titulo-cuenta').textContent = titulos[vista][0];
        document.getElementById('subtitulo-cuenta').textContent = titulos[vista][1];
        avisar('');
        document.querySelector('#form-restablecer button').disabled = !tokenRecuperacion;
        if (vista === 'restablecer' && !tokenRecuperacion) {
            avisar('Abre el enlace de tu correo o solicita uno nuevo.', true);
            document.querySelector('#form-restablecer button').disabled = true;
        }
    }
    window.addEventListener('hashchange', mostrarPanel);
    mostrarPanel();

    async function enviar(ruta, datos) {
        const respuesta = await fetch(`${window.MONARCA_CONFIG.apiUrl}/auth/${ruta}`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(datos)
        });
        const texto = await respuesta.text();
        let contenido;
        try { contenido = JSON.parse(texto); } catch { contenido = {mensaje: texto}; }
        if (!respuesta.ok) {
            if (respuesta.status === 401) throw new Error('Correo o contraseña incorrectos.');
            if (respuesta.status === 503 && contenido.error) throw new Error(contenido.error);
            if (respuesta.status >= 500) throw new Error('No se pudo completar la solicitud. Inténtalo más tarde.');
            throw new Error(contenido.error || contenido.mensaje || 'Revisa los datos e inténtalo de nuevo.');
        }
        return contenido;
    }
    async function conEspera(form, accion) {
        if (ocupado) return;
        ocupado = true;
        const boton = form.querySelector('button[type="submit"]');
        const texto = boton.textContent;
        boton.disabled = true; boton.textContent = 'Procesando…'; avisar('');
        try { await accion(); }
        catch (error) { avisar(error instanceof TypeError ? 'No se pudo conectar con el servidor. Verifica que esté encendido.' : error.message, true); }
        finally { ocupado = false; boton.disabled = false; boton.textContent = texto; }
    }
    formLogin.addEventListener('submit', e => {
        e.preventDefault();
        if (!formLogin.reportValidity()) return;
        conEspera(formLogin, async () => {
            const datos = await enviar('login', {
                email: document.getElementById('usuario').value.trim(),
                password: document.getElementById('password').value
            });
            localStorage.setItem('token_monarca', datos.token);
            localStorage.setItem('rol_monarca', datos.rol);
            location.href = datos.rol === 'ROLE_ADMIN' ? 'admin.html' : '../index.html';
        });
    });
    formRegistro.addEventListener('submit', e => {
        e.preventDefault();
        if (!formRegistro.reportValidity()) return;
        const nombre = document.getElementById('registro-nombre').value.trim();
        const apellido = document.getElementById('registro-apellido').value.trim();
        const email = document.getElementById('registro-email').value.trim();
        const password = document.getElementById('registro-password').value;
        if (!nombre || !apellido) return avisar('Escribe tus nombres y apellidos.', true);
        if (password !== document.getElementById('registro-confirmar').value) return avisar('Las contraseñas no coinciden.', true);
        if (new TextEncoder().encode(password).length > 72) return avisar('La contraseña es demasiado larga. Usa menos caracteres.', true);
        conEspera(formRegistro, async () => {
            await enviar('registro', {nombre, apellido, email, password});
            formRegistro.reset();
            history.replaceState(null, '', '#login');
            mostrarPanel();
            document.getElementById('usuario').value = email;
            document.getElementById('password').value = '';
            avisar('Cuenta creada. Ya puedes iniciar sesión con tu correo y contraseña.');
            document.getElementById('password').focus();
        });
    });
    const formRecuperar = document.getElementById('form-recuperar');
    formRecuperar.addEventListener('submit', e => {
        e.preventDefault();
        if (!formRecuperar.reportValidity()) return;
        conEspera(formRecuperar, async () => {
            const respuesta = await enviar('olvide-password', {email: document.getElementById('recuperar-email').value.trim()});
            avisar(respuesta.mensaje);
        });
    });
    const formRestablecer = document.getElementById('form-restablecer');
    formRestablecer.addEventListener('submit', e => {
        e.preventDefault();
        if (!formRestablecer.reportValidity() || !tokenRecuperacion) return;
        const password = document.getElementById('nueva-password').value;
        if (password !== document.getElementById('nueva-confirmar').value) return avisar('Las contraseñas no coinciden.', true);
        if (new TextEncoder().encode(password).length > 72) return avisar('La contraseña es demasiado larga. Usa menos caracteres.', true);
        conEspera(formRestablecer, async () => {
            const respuesta = await enviar('restablecer-password', {token: tokenRecuperacion, password});
            tokenRecuperacion = null; formRestablecer.reset();
            localStorage.removeItem('token_monarca'); localStorage.removeItem('rol_monarca');
            history.replaceState(null, '', '#login'); mostrarPanel();
            avisar(respuesta.mensaje);
        });
    });
    document.querySelectorAll('[data-clave]').forEach(boton => boton.addEventListener('click', () => {
        const campo = document.getElementById(boton.dataset.clave);
        const mostrar = campo.type === 'password';
        campo.type = mostrar ? 'text' : 'password';
        boton.setAttribute('aria-pressed', String(mostrar));
        boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
    }));
});
