-- SOLO LECTURA. Revisar antes de probar nuevos estados sobre una base preexistente.
SELECT conrelid::regclass AS tabla, conname AS restriccion, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint WHERE conrelid IN ('monarca.pedidos'::regclass,'monarca.pagos'::regclass,'monarca.usuarios'::regclass)
ORDER BY tabla,restriccion;
-- pedidos.estado debe permitir PENDIENTE_PAGO, PAGADO, ENVIADO, ENTREGADO, CANCELADO.
-- pagos.estado debe permitir PENDIENTE, PAGADO, CANCELADO.
-- codigo_pedido necesita unicidad y capacidad de 40 caracteres.
SELECT table_name,column_name,data_type,character_maximum_length FROM information_schema.columns
WHERE table_schema='monarca' AND (column_name='estado' OR column_name='codigo_pedido');
