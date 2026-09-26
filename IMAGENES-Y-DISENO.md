# Fotos y diseño de Monarca

## Qué cambió

- El formulario de productos permite escribir una descripción de hasta 5000 caracteres.
- Cada producto admite hasta 8 fotos. La primera es la portada; las demás aparecen como miniaturas en la ficha. Puedes colocar frontal, posterior y detalles, describir cada foto, cambiar el orden y quitarla de la galería.
- Puedes seleccionar varias fotos JPG o PNG desde tu computadora (5 MB por archivo). Al guardar se suben al backend, este las envía a Supabase y guarda sus URL automáticamente. Los reintentos conservan las URL de las cargas ya terminadas en el formulario abierto. Las fotos nuevas se agrupan en `catalogo/nombre-del-producto-identificador/`. Al editar se reutiliza la carpeta de una foto existente, aunque cambie el nombre del producto. Si se eliminan todas las fotos asociadas, una carga futura puede crear otra carpeta. Las fotos antiguas guardadas directamente en `catalogo/` mantienen su URL y no se mueven automáticamente.
- Se mantiene la opción de añadir una URL existente.
- El encabezado usa los nombres registrados del usuario, no su correo. Hace falta volver a iniciar sesión para obtener un token con el nombre.
- Menú móvil, iconos, paginación, galería y formulario adaptados a pantallas pequeñas.

## Configurar la carga directa una sola vez

1. En tu proyecto Supabase, abre **Storage** y usa o crea un bucket público para fotos de productos, por ejemplo `productos`. El bucket debe ser público porque las fotos se muestran a los visitantes. No necesitas habilitar subidas públicas.
2. Busca la **Project URL** y la clave de servidor **service_role** en la configuración de API del proyecto (claves heredadas/legacy si corresponde). No uses la contraseña de PostgreSQL ni la clave `anon`. No pegues esa clave en el chat, en archivos del frontend ni en GitHub.
3. Ejecuta en tu PowerShell:

```powershell
cd C:\Users\USUARIO\Videos\Monarca
powershell -NoProfile -ExecutionPolicy Bypass -File .\Configurar-Imagenes.ps1
```

El programa solicita URL del proyecto, nombre del bucket y clave. La clave se escribe oculta y Windows la guarda cifrada en `%LOCALAPPDATA%\Monarca\imagenes.xml`, fuera del repositorio. Solo el backend la utiliza. La configuración anterior de Gmail y PostgreSQL no cambia.

4. Detén el backend anterior con Ctrl+C y vuelve a iniciarlo:

```powershell
cd C:\Users\USUARIO\Videos\Monarca\backend
.\mvnw.cmd clean compile
cd ..
powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Monarca.ps1
```

5. Recarga la web con Ctrl+F5 y vuelve a iniciar sesión. En Administración → Registrar producto o Editar, escribe la descripción, selecciona varias fotos, elige la portada y guarda.

No necesitas migrar la base de datos: se reutilizan `descripcion` e `imagenes_producto`, con `texto_alternativo`, `principal` y `orden` existentes.

## Límites y comprobaciones

- Quitar una foto de la galería no elimina su archivo de Storage. Esto evita borrar una imagen compartida. Una carga terminada antes de cancelar o de un error al guardar también puede dejar un archivo sin asociar; se puede revisar en `catalogo/` del bucket.
- Sin configurar Storage puedes seguir usando URL HTTPS existentes. La carga de archivos muestra un aviso de configuración pendiente.
- Las pruebas de Storage simulan la respuesta de Supabase. El usuario confirmó que la carga real funciona. La nueva agrupación por producto se verifica en las siguientes subidas. Las pruebas de interfaz usan productos de prueba, no crean productos en tu base real.
- El bucket consume el almacenamiento y tráfico de tu plan Supabase.

Referencias oficiales: [carga estándar](https://supabase.com/docs/guides/storage/uploads/standard-uploads) y [control de acceso de Storage](https://supabase.com/docs/guides/storage/security/access-control).
