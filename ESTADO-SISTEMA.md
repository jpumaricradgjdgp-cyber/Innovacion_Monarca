# Estado del sistema — 26 de septiembre de 2026

## Confirmado en la instalación por el usuario

- El backend inicia y conecta con Supabase.
- El correo de recuperación llega por Gmail.
- Las fotos seleccionadas en el administrador llegan a Storage.

## Comprobado automáticamente

- 59 pruebas del backend: autenticación real, tokens, registro, recuperación, variantes, galería, permisos, compras, concurrencia, cancelaciones y reportes. Base H2 aislada; no se han usado los pedidos de Supabase para las pruebas.
- Ciclo completo por API: login de cliente y administrador, pedido, reintento con la misma clave, listado, confirmación de pago con referencia, envío y entrega.
- El cliente no puede confirmar pagos ni cancelar pedidos ajenos. Cancelar dos veces devuelve stock una sola vez. No se permite cancelar pedidos pagados ni saltarse estados.
- La organización de fotos nuevas utiliza `catalogo/nombre-identificador/`. El formulario reutiliza la carpeta de las imágenes guardadas. Los enlaces de fotos antiguas no cambian.
- La interfaz se comprueba con API simulada, incluyendo carga múltiple, formularios, carrito, checkout, nombre del cliente y cinco anchos de pantalla.

## Pendientes que requieren datos o acceso de la instalación

1. Corregir las tallas antiguas que se registraron juntas y repartir su stock según las unidades físicas. El programa no puede deducir esa distribución.
2. Realizar una compra de aceptación en Supabase con cuentas de prueba, verificando las restricciones reales de la base y las instrucciones del método de pago elegido. El cobro sigue siendo manual.
3. Revisar los cambios locales y las eliminaciones de archivos antes de subirlos a GitHub. Este avance no hace commit ni push. La ejecución de GitHub Actions con PostgreSQL queda por confirmar tras publicar.
4. Desarrollar el Probador Virtual de IA exigido por la propuesta: busto, cintura y cadera; ficha técnica por talla; recomendación personalizada; fotografía opcional y visualización aproximada. El alcance ya está definido. Faltan las medidas reales de prendas, los datos para entrenar/evaluar y la implementación del modelo.

Las fotos antiguas bajo `catalogo/` se mantienen para conservar sus URL. Moverlas exige actualizar sus referencias; no las muevas manualmente desde Storage sin actualizar los productos.

## Corrección de confirmación de pagos

La restricción real compartida desde Supabase admite PAGADO para pagos, pero el servicio enviaba CONFIRMADO. Se corrigió el servicio y se alineó la base generada de pruebas. La interfaz ahora exige una referencia antes de confirmar. Falta reiniciar y verificar el pedido en Supabase. Consulta AUDITORIA-SISTEMA-2026-09-26.md para el diagnóstico y los pendientes del Probador Virtual.
