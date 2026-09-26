# Iniciar Monarca y recuperar cuentas

El registro y la recuperación ya están implementados. El iniciador configura Gmail y Supabase una vez y carga esos datos en cada arranque. No instala Java ni inicia Live Server.

## Primera vez

1. Usa Java 21. Abre `fronted/Paginas/login.html` con Live Server y copia su URL completa del navegador.
2. En tu cuenta personal de Google, activa la verificación en dos pasos y crea una [contraseña de aplicación](https://myaccount.google.com/apppasswords). No uses tu contraseña habitual de Gmail ni compartas la clave por chat.
3. Desde una terminal PowerShell ejecuta:

```powershell
cd C:\Users\USUARIO\Videos\Monarca
powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Monarca.ps1
```

El cambio de política solo afecta al proceso que ejecuta este archivo. Primero detén con Ctrl+C cualquier backend que ya esté usando el puerto 8080.

El asistente pide la URL JDBC de Supabase, su usuario y contraseña, el Gmail remitente, su contraseña de aplicación y la URL del login. Las claves no se muestran mientras escribes. Los valores existentes de esa terminal se ofrecen como valores iniciales. La URL JDBC tiene la forma `jdbc:postgresql://HOST:5432/postgres?sslmode=require`: sustituye HOST por el de tu conexión Supabase; no pegues una URL que contenga contraseña.

No necesitas inventar JWT_SECRET: conserva el existente si está configurado; de lo contrario genera uno aleatorio y lo guarda para próximos arranques. Si genera una clave distinta a la usada antes, inicia sesión de nuevo.

## Siguientes veces

Ejecuta el mismo comando: arranca sin volver a pedir los datos. Mantén Live Server y el backend encendidos. Para corregir los datos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Monarca.ps1 -Configurar
```

Enter conserva cada dato anterior. Añade `-SoloConfigurar` para guardar sin arrancar.

Los datos se guardan en `%LOCALAPPDATA%\Monarca\configuracion.xml`, fuera de GitHub. Las tres claves (Supabase, Gmail y JWT) están cifradas con la protección de Windows y solo se pueden recuperar con la misma cuenta en el mismo equipo. Correo, URL y usuario no están cifrados. No copies ese archivo al repositorio. Cada PC necesita su propia configuración. El archivo sirve para una instalación local de Monarca por cuenta de Windows.

## Cómo funciona en la página

1. **Crear cuenta:** introduce nombre, apellido, correo real y contraseña. Se crea un CLIENTE; nunca un administrador.
2. **Olvidé mi contraseña:** escribe el mismo correo registrado. El backend envía un enlace mediante el Gmail configurado.
3. Abre el enlace, escribe la nueva contraseña dos veces y vuelve a iniciar sesión. El enlace vence en 15 minutos y solo sirve una vez. Cambiar la contraseña invalida las sesiones anteriores.

La recuperación también sirve para un ADMIN si su cuenta tiene un buzón real al que tenga acceso; no cambia su rol. `admin@monarca.com` y `cliente@monarca.com` no recibirán mensajes si esos buzones no existen o no te pertenecen. Puedes crear un cliente con tu correo real para probar sin modificar al administrador.

Los enlaces locales se abren en la misma computadora donde corren los servidores; no sirven desde el celular. Reiniciar el backend invalida los enlaces pendientes. El mensaje de solicitud es genérico aunque la cuenta no exista; revisa spam y, si falla el envío, la consola del backend. Espera un minuto para pedir otro enlace.

## Comprobación pendiente con tus datos

El guardado local no comprueba que las credenciales sean correctas. Para validar la instalación real: registra tu correo, solicita el enlace, verifica que llegue, cambia la contraseña y prueba que puedas entrar con la nueva y que el enlace usado ya no funcione. No se ha enviado un correo real durante la implementación. Las pruebas automatizadas previas de recuperación usan correo y base de datos simulados.

El iniciador se verificó con arranque y persistencia simulados: generación y conservación del JWT, claves SecureString, carga SMTP y restauración del entorno tras éxito o error. El sandbox no carga el perfil necesario para DPAPI; el cifrado real debe comprobarse al ejecutar el iniciador en tu terminal de Windows. Si Windows no puede cifrar, el script se detiene sin guardar claves en texto plano ni sustituir la configuración anterior.
