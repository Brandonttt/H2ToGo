-- ============================================================================
-- H2ToGo · Borra todo lo que crearon 01_datos_carga.sql y las pruebas de carga.
--
-- Solo toca la purificadora "Prueba de Carga", su marca y los usuarios
-- @carga.h2togo.test, con sus pedidos, historial, inventario y movimientos.
--
-- El historial de pedidos y los movimientos de inventario no se pueden borrar
-- (trigger de bitácora inmutable). Para limpiar los datos de prueba, esta
-- transacción apaga los triggers con session_replication_role = replica; eso
-- también apaga las FKs, por eso se borra en orden de hijos a padres.
-- Requiere el superusuario del esquema (h2togo).
-- ============================================================================
BEGIN;
SET LOCAL session_replication_role = replica;

CREATE TEMP TABLE c_usuarios ON COMMIT DROP AS
    SELECT id_usuario FROM usuarios WHERE correo LIKE '%@carga.h2togo.test';
CREATE TEMP TABLE c_negocios ON COMMIT DROP AS
    SELECT id_negocio FROM negocios WHERE nombre_comercial = 'Purificadora Prueba de Carga';
CREATE TEMP TABLE c_marcas ON COMMIT DROP AS
    SELECT id_marca FROM marcas WHERE nombre = 'Prueba de carga 20L';
CREATE TEMP TABLE c_lotes ON COMMIT DROP AS
    SELECT id_lote FROM lotes_inventario WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
CREATE TEMP TABLE c_pedidos ON COMMIT DROP AS
    SELECT id_pedido FROM pedidos
    WHERE id_cliente IN (SELECT id_usuario FROM c_usuarios)
       OR id_repartidor IN (SELECT id_usuario FROM c_usuarios)
       OR id_negocio_solicitado IN (SELECT id_negocio FROM c_negocios);

SELECT (SELECT count(*) FROM c_usuarios) AS usuarios, (SELECT count(*) FROM c_negocios) AS negocios,
       (SELECT count(*) FROM c_pedidos)  AS pedidos;

DELETE FROM movimientos_inventario
WHERE id_pedido IN (SELECT id_pedido FROM c_pedidos)
   OR id_lote IN (SELECT id_lote FROM c_lotes)
   OR id_repartidor_responsable IN (SELECT id_usuario FROM c_usuarios);
DELETE FROM historial_estados_pedido  WHERE id_pedido IN (SELECT id_pedido FROM c_pedidos);
DELETE FROM apartados_pedido          WHERE id_pedido IN (SELECT id_pedido FROM c_pedidos)
                                         OR id_lote IN (SELECT id_lote FROM c_lotes);
DELETE FROM detalles_pedido           WHERE id_pedido IN (SELECT id_pedido FROM c_pedidos);
DELETE FROM pedidos                   WHERE id_pedido IN (SELECT id_pedido FROM c_pedidos);
DELETE FROM inventario_vehiculo       WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
DELETE FROM lotes_inventario          WHERE id_lote IN (SELECT id_lote FROM c_lotes);
DELETE FROM solicitudes_cambio_perfil WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
DELETE FROM direcciones_clientes      WHERE id_cliente IN (SELECT id_usuario FROM c_usuarios);
DELETE FROM clientes                  WHERE id_usuario IN (SELECT id_usuario FROM c_usuarios);
DELETE FROM repartidores              WHERE id_usuario IN (SELECT id_usuario FROM c_usuarios);
DELETE FROM vehiculos_negocio         WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
DELETE FROM horarios_negocio          WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
DELETE FROM productos_negocio         WHERE id_negocio IN (SELECT id_negocio FROM c_negocios)
                                         OR id_marca IN (SELECT id_marca FROM c_marcas);
DELETE FROM negocios                  WHERE id_negocio IN (SELECT id_negocio FROM c_negocios);
DELETE FROM marcas                    WHERE id_marca IN (SELECT id_marca FROM c_marcas);
DELETE FROM usuarios                  WHERE id_usuario IN (SELECT id_usuario FROM c_usuarios);

-- Verificación (esperado: 0 | 0 | 0).
SELECT (SELECT count(*) FROM usuarios WHERE correo LIKE '%@carga.h2togo.test')                       AS usuarios,
       (SELECT count(*) FROM negocios WHERE nombre_comercial = 'Purificadora Prueba de Carga')       AS negocios,
       (SELECT count(*) FROM pedidos p WHERE NOT EXISTS
            (SELECT 1 FROM usuarios u WHERE u.id_usuario = p.id_cliente))                           AS pedidos_huerfanos;
COMMIT;
