# Monarca — sistema de tienda

Proyecto universitario: Spring Boot 3.4.2, Java 21, PostgreSQL/Supabase y HTML/CSS/JavaScript. La carpeta `fronted` mantiene su nombre original.

## Arranque local

1. Instala Java 21 y abre la carpeta raíz en VS Code.
2. Abre `fronted/Paginas/login.html` con Live Server (puertos admitidos por defecto: 5500 y 5501).
3. Ejecuta `powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Monarca.ps1` desde la raíz. Consulta `INICIO-MONARCA.md` para Gmail y Supabase.
4. Para cambiar datos usa `-Configurar`. Mantén el backend y Live Server encendidos. Actualiza el navegador con Ctrl+F5 después de instalar cambios.

No vuelvas a crear tu base actual. `backend/db/001_esquema_nuevo.sql` es exclusivamente para una base vacía y se detiene si ya hay tablas. En una instalación nueva ejecuta primero ese archivo y luego `002_datos_iniciales.sql`. No se crean cuentas con claves predeterminadas. El registro público crea CLIENTE; asignar ADMIN requiere una operación administrativa controlada en la base.

En la base preexistente, `003_revision_base_existente.sql` permite revisar, sin modificar datos, las restricciones de estados y la unicidad de `codigo_pedido`. El código reutiliza las tablas y columnas actuales; `ddl-auto=validate` sigue activo. Si hay restricciones CHECK diferentes a las documentadas, deben revisarse antes de activar los nuevos estados; no se eliminan automáticamente restricciones desconocidas.

## Flujo del cliente

- Crear cuenta, iniciar sesión y recuperar contraseña mediante un enlace enviado por Gmail.
- Elegir talla/color, añadir a la bolsa y confirmar entrega y método de pago.
- Cada intento de compra lleva una clave UUID. Repetir la misma operación devuelve el pedido ya creado, sin volver a descontar stock.
- Si se pierde la respuesta, el checkout conserva esa operación en la pestaña y permite reintentarlo con los mismos datos. No cierres la pestaña antes de consultar Mis pedidos si no sabes si la compra terminó.
- Mis pedidos muestra historial paginado, detalle y entrega. El cliente solo ve sus propios pedidos.
- Se permite cancelar únicamente mientras esté PENDIENTE_PAGO. La cancelación devuelve existencias una sola vez. Hay un máximo de cinco pedidos pendientes por cuenta.

## Flujo del administrador

- Crear/editar productos. Añadir, activar y desactivar variantes. Desactivar/reactivar productos.
- Editar nombre, precio o imagen no modifica stock. El botón «Aplicar ajuste de stock» recibe una diferencia (+entrada, -salida) y rechaza el ajuste si el stock cambió desde que se abrió el formulario.
- Ver pedidos y datos de entrega. Tras comprobar externamente el pago, registrar su referencia y confirmar la recepción.
- Estados: `PENDIENTE_PAGO → PAGADO → ENVIADO → ENTREGADO`; o `PENDIENTE_PAGO → CANCELADO`.
- La confirmación manual registra fecha y usuario administrador en el pago. No existe cobro automático ni se afirma que elegir Yape/tarjeta produzca un cobro.
- Reportes con datos reales: pedidos por estado, clientes de pedidos pagados por mes de creación del pedido y unidades/importe por categoría actual. Los pendientes no cuentan como ingresos cobrados. Se usan tablas en lugar de gráficas con cifras ficticias.
- Catálogo público en lotes de 60 productos, con consultas agrupadas de imágenes y variantes; las categorías se muestran en páginas de 24. Listados administrativos y pedidos en páginas de 20. La búsqueda del inventario filtra la página cargada.

La política de esta etapa es envío gratuito y sin descuentos. No se implementa devolución de dinero de pedidos ya pagados ni expiración automática de reservas: las cancelaciones son explícitas. Para una pasarela o reembolsos se necesita ampliar ese flujo.

## Contacto

El contacto abre la aplicación de correo del visitante para escribir al correo de Monarca. Se retiraron formularios de suscripción y enlaces sin destino que aparentaban tener una función. No se envía un correo sin que el visitante lo envíe desde su aplicación.

## Configuración y seguridad

- `fronted/js/config.js` contiene solo configuración pública de la API y contacto. Para publicar, cambia `apiUrl` al dominio HTTPS real.
- Backend: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `PASSWORD_RESET_URL`, `CORS_ORIGINS` (lista separada por comas).
- CORS se define en un solo lugar y admite únicamente los orígenes configurados. Para otro puerto local añade su origen completo. Al usar Gmail, la clave es la contraseña de aplicación generada por Google.
- Login: límite de 20 intentos fallidos por cuenta y 50 fallidos por IP cada 15 minutos; los accesos correctos no consumen el límite y reinician el contador de la cuenta; registro: 15 por IP. Recuperación tiene límites propios. Son límites en memoria para una instancia, no un servicio distribuido.
- Tokens de recuperación: 15 minutos, un uso, almacenados como hash en memoria. Reiniciar invalida enlaces pendientes. Cambiar la contraseña invalida sesiones anteriores.
- El navegador conserva JWT en localStorage. Cerrar sesión los borra de ese navegador; no revoca un JWT copiado antes. No se implantó refresh token ni gestión de dispositivos.
- Los secretos del iniciador se guardan cifrados en el perfil de Windows, fuera de Git. Nunca los copies al código, `.env` versionados o incidencias.

## Pruebas

Desde `backend`: `./mvnw test` (Windows: `.\mvnw.cmd test`). El perfil de pruebas usa H2 aislado con compatibilidad PostgreSQL, crea y elimina solo sus tablas en memoria, y no requiere credenciales de Supabase. Incluye concurrencia, idempotencia, rollback, cancelación, permisos, entrega, stock, recuperación y reportes. No utilices variables globales de Spring que redirijan pruebas a una base con datos reales.

Desde `fronted`: `npm ci`, `npx playwright install chromium`, `npm test`. Para usar Chrome instalado, define `BROWSER_EXECUTABLE` con su ruta. Las pruebas de interfaz simulan la API; no hacen compras ni envíos reales.

GitHub Actions ejecuta las pruebas al subir cambios, incluyendo un PostgreSQL 17 temporal creado exclusivamente para CI con el esquema versionado. Su contraseña de ejemplo no pertenece a ninguna cuenta real. Este job debe terminar correctamente para dar por validado PostgreSQL: las comprobaciones locales se realizaron con H2. El usuario confirmó la recepción real del correo de recuperación y la subida de fotos a Supabase. El ciclo de pedidos se comprueba automáticamente con autenticación real y H2 aislado; la aceptación de pedidos contra la base Supabase existente sigue pendiente.

## Siguiente etapa obligatoria: ML

Machine learning es un requisito del proyecto y queda pendiente después de cerrar y validar el sistema. Esta entrega no implementa ni presenta reglas fijas o productos destacados como si fueran un modelo ML. El caso de uso confirmado es un Probador Virtual: recomendación de talla a partir de busto, cintura y cadera, comparados con fichas técnicas de prendas; foto opcional para visualización aproximada. Se deben recopilar medidas reales por talla, material, corte y elasticidad, además de datos de ajuste y métricas para entrenar y evaluar. Véase PROBADOR-VIRTUAL.md.
