# Probador Virtual — alcance académico confirmado

La propuesta suministrada por el usuario menciona Nayelis's Moda. El repositorio actual se llama Monarca; este documento no cambia la marca de la tienda.

## Funciones requeridas

1. Recibir medidas de busto, cintura y cadera, expresadas en centímetros.
2. Mantener una ficha técnica por prenda y talla: medidas, corte, elasticidad, material y propiedades físicas relevantes.
3. Recomendar la talla más compatible mediante un modelo evaluado. Explicar el resultado y reconocer cuándo faltan datos para recomendar.
4. Permitir fotografía de manera opcional y generar una visualización aproximada de la prenda mediante visión artificial / Virtual Try-On.

La visualización no equivale a una medición exacta del ajuste. La reducción de devoluciones y mejora de conversión son resultados esperados de la propuesta, no resultados demostrados del sistema actual.

## Orden de implementación

### Fichas técnicas en el catálogo

Añadir un formulario separado de las existencias. Registrar qué significan las medidas: contorno de la prenda o rango corporal recomendado; no mezclar ambos. Los colores de una misma talla pueden compartir ficha, pero las variantes deben mantener su stock individual. Documentar el método de medición, las unidades y los campos no aplicables a accesorios/calzado.

Antes de modificar PostgreSQL se debe preparar y revisar una migración aditiva, conservando productos, variantes y pedidos existentes. No usar `ddl-auto=update` como sustituto de la migración.

### Datos y modelo de recomendación

Recopilar ejemplos autorizados con medidas corporales, ficha técnica, talla probada y resultado de ajuste (pequeña, adecuada, grande). Separar entrenamiento y evaluación evitando que datos de una misma persona se filtren entre ambos. Comparar el modelo con una referencia simple y documentar resultados y limitaciones. No presentar reglas fijas ni datos inventados como un modelo entrenado y validado.

Todavía no se han proporcionado las medidas reales ni ese conjunto de ejemplos. La ficha administrativa y la validación de entradas pueden desarrollarse antes del entrenamiento.

### Integración con la tienda

Botón «Encontrar mi talla» en la ficha del producto. Mostrar recomendación, motivo y disponibilidad. Si la talla recomendada está agotada, mostrarlo; no recomendar otra únicamente porque tiene stock. Mantener la compra manual disponible sin obligar a usar el probador.

### Fotografía y visualización

La foto es opcional y su procesamiento requiere una acción clara del usuario. Mantener las fotos personales fuera del bucket público de productos. Elegir una solución de Virtual Try-On, revisar sus requisitos de ejecución/licencia, y definir retención y eliminación antes de transmitir fotografías a un proveedor. Probar la calidad en los tipos de prendas del catálogo y comunicar que la imagen es aproximada.

## Estado

Este documento fija requisitos y dependencias; todavía no implementa entrenamiento, inferencia ni Virtual Try-On. La siguiente implementación corresponde a las fichas técnicas y los datos que alimentarán el modelo.
