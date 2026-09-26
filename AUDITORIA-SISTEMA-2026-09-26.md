# Auditoría del sistema y preparación del Probador Virtual

## Hallazgo bloqueante: confirmación de pagos

La definición de `chk_pago_estado` compartida desde Supabase permite PENDIENTE, EN_REVISION, PAGADO, RECHAZADO, CANCELADO y REEMBOLSADO. `PedidoService` intentaba escribir CONFIRMADO al verificar el pago; ese valor es rechazado por PostgreSQL y explica el conflicto 409 observado. La restricción de pedidos sí admite PAGADO.

Se corrige el servicio para guardar PAGADO en el pago y en el pedido dentro de la misma transacción. Se alinean el esquema para instalaciones nuevas y la restricción del modelo usada al generar la base de pruebas. No se ejecuta ninguna modificación sobre Supabase ni se cambia la restricción existente.

La interfaz valida la referencia antes de enviar la confirmación, explica qué introducir y conserva los datos ante errores. El error de integridad incluye un identificador; el backend registra ese identificador, SQLSTATE y nombre de restricción sin incluir los valores de la fila en este diagnóstico.

El código HTTP 400 aislado no identifica su causa. La referencia vacía era una causa posible comprobada en el código; la URL y respuesta de aquella petición no se proporcionaron.

## Estado funcional y límites de la verificación

| Área | Estado | Verificación pendiente en la instalación |
| --- | --- | --- |
| Registro, login y recuperación | Implementados; correo recibido confirmado por el usuario | Repetir aceptación tras cambios de configuración |
| Catálogo y variantes | Edición, tallas individuales y ajuste explícito de stock | Corregir tallas antiguas combinadas con cantidades físicas reales |
| Fotos | Carga múltiple, galería y carpetas por producto | Las fotos anteriores mantienen sus rutas; no se reorganizan automáticamente |
| Compras | Totales calculados por servidor, reserva de stock y reintentos con clave | Compra de aceptación contra Supabase |
| Estados y pago | Corregida incompatibilidad de estado; verificación manual con referencia | Reiniciar backend y confirmar el pedido pendiente en Supabase |
| Permisos | Propiedad de pedidos y acciones administrativas verificadas en pruebas | Aceptación con cuentas separadas en la instalación |
| Presentación | Formularios, galería, navegación y tamaños móviles cubiertos por pruebas anteriores | Revisión visual con catálogo y textos definitivos |

Las pruebas de backend se ejecutan en H2. Las pruebas de interfaz usan API simulada. No equivalen a una compra real sobre Supabase. La restricción añadida a la entidad Pago hace que el estado incorrecto también falle en la base generada de pruebas. PostgreSQL en GitHub Actions sigue pendiente de confirmar. No se han efectuado cobros reales ni confirmado pedidos del usuario desde estas pruebas.

## Qué falta para el ML obligatorio

1. **Ficha técnica persistente y formulario administrativo:** medidas por prenda/talla, material, corte y elasticidad. Definir si cada medida representa contorno de prenda o rango corporal; expresarla en centímetros. Preparar migración aditiva.
2. **Datos reales:** medidas de prendas y ejemplos autorizados de medidas corporales, talla probada y resultado de ajuste. No deducirlos de nombres de tallas o fotografías generales.
3. **Recomendación entrenada y evaluada:** separar datos de entrenamiento y evaluación por persona, comparar con una referencia sencilla, medir errores y explicar cuándo faltan datos. Las reglas fijas por sí solas no completan el requisito de ML.
4. **Integración:** formulario de busto/cintura/cadera, recomendación y disponibilidad en el producto, sin obligar a usar el probador para comprar.
5. **Virtual Try-On:** seleccionar y evaluar la tecnología, comprobar licencia y recursos de ejecución; fotografía opcional, almacenamiento privado, consentimiento y eliminación. Mostrar que el resultado visual es aproximado.

El modelo, la inferencia y el probador visual todavía no están implementados. El siguiente desarrollo puede ser la ficha técnica; el entrenamiento requiere datos reales. No es posible afirmar todavía una reducción de devoluciones.

## Verificación manual del arreglo

1. Detener el backend actual con Ctrl+C en su terminal e iniciarlo con el script habitual.
2. Recargar el administrador con Ctrl+F5 y abrir el pedido pendiente.
3. Verificar el pago e introducir su número de operación o recibo; pulsar Confirmar pago recibido.
4. Comprobar que pedido y pago muestran PAGADO, que no se descuenta stock una segunda vez y que el cliente ve el cambio.
5. Si falla, conservar el identificador mostrado y buscar la línea correspondiente de «Conflicto de integridad» en la terminal.

No ejecutar `001_esquema_nuevo.sql` sobre la base existente: está destinado únicamente a instalaciones vacías.
