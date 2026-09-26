-- Catálogos mínimos; nunca crea usuarios ni contraseñas.
BEGIN;
INSERT INTO monarca.roles(nombre,descripcion,activo) VALUES
 ('CLIENTE','Cliente de la tienda',true),('ADMIN','Administración de la tienda',true) ON CONFLICT DO NOTHING;
INSERT INTO monarca.categorias(nombre,slug,activo) VALUES
 ('Tops','tops',true),('Bottoms','bottoms',true),('Calzado','calzado',true),('Accesorios','accesorios',true) ON CONFLICT DO NOTHING;
INSERT INTO monarca.metodos_pago(codigo,nombre,descripcion,requiere_comprobante,activo) VALUES
 ('MANUAL','Pago coordinado con Monarca','Coordina el pago con Monarca. El administrador confirma la recepción.',false,true) ON CONFLICT DO NOTHING;
COMMIT;
