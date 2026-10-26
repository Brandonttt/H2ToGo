-- ==========================================
-- PRUEBAS DEL ESQUEMA H2ToGo v7 (PostgreSQL + PostGIS)
-- ==========================================
-- Ejecutar DESPUÉS de H2ToGo_v7_postgresql.sql, en una BD desechable y con un
-- usuario que pueda adoptar los roles h2togo_* (el dueño del esquema):
--   \i pruebas_v7.sql
--
-- Secciones:
--   1. Datos semilla (alta dueño+negocio en una transacción: la BD revisa al
--      COMMIT que cada negocio tenga exactamente un dueño [R1]).
--   2. Comprobaciones del cálculo de zona de cobertura y de proximidad.
--   3. Pruebas negativas: operaciones que el esquema DEBE rechazar.
--      Cada bloque termina en "OK ..." si la BD rechazó la operación.
--   4. Seguridad por actor [S]: lo que cada rol puede y no puede ver o hacer.
--
-- Nota: se fijan ids con OVERRIDING SYSTEM VALUE para que las pruebas sean
-- legibles. En una BD real los ids los genera la identidad de la columna.
-- ==========================================

-- ------------------------------------------
-- 1. SEMILLA
-- ------------------------------------------
BEGIN;

-- Zona de cobertura: rectángulo aproximado sobre la Alcaldía Benito Juárez.
-- En el proyecto real aquí se carga el polígono administrativo exportado de OSM.
INSERT INTO zonas_cobertura (nombre, geom) VALUES
('Zona de prueba (aprox. Benito Juárez)',
 ST_GeogFromText('SRID=4326;MULTIPOLYGON(((-99.20 19.34, -99.12 19.34, -99.12 19.42, -99.20 19.42, -99.20 19.34)))'));

-- Admin
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Ana', 'Admin', 'admin@h2togo.mx', 'hash', '5550000001', 'admin');

-- Dueño (repartidor) + su negocio: primero el negocio, luego el repartidor con
-- es_dueno; el trigger diferido comprueba al COMMIT que haya un dueño [R1].
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (2, 'Rafa', 'Repartidor', 'rafa@h2togo.mx', 'hash', '5550000002', 'repartidor');

INSERT INTO negocios (id_negocio, nombre_comercial,
                      calle, numero_exterior, colonia, codigo_postal, ubicacion_base)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Purificadora El Manantial',
        'Eje Central', '100', 'Portales', '03300',
        ST_GeogFromText('SRID=4326;POINT(-99.1500 19.3700)'));

INSERT INTO repartidores (id_usuario, id_negocio, es_dueno) VALUES (2, 1, TRUE);

-- Segundo negocio (sin dirección de base todavía: caso permitido por el CHECK)
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (5, 'Sofía', 'Repartidora', 'sofia@h2togo.mx', 'hash', '5550000005', 'repartidor');

INSERT INTO negocios (id_negocio, nombre_comercial)
OVERRIDING SYSTEM VALUE
VALUES (2, 'Purificadora Azul');

INSERT INTO repartidores (id_usuario, id_negocio, es_dueno) VALUES (5, 2, TRUE);

-- Vehículos
INSERT INTO vehiculos_negocio (id_vehiculo, id_negocio, tipo_vehiculo, marca, color, capacidad_garrafones)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'motocicleta', 'Italika', 'rojo', 12),
       (2, 2, 'camioneta',   'Nissan',  'blanco', 40);

-- Clientes y direcciones (una dentro de la zona, otra fuera)
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (3, 'Carla', 'Cliente', 'carla@h2togo.mx', 'hash', '5550000003', 'cliente'),
       (4, 'Beto',  'Cliente', 'beto@h2togo.mx',  'hash', '5550000004', 'cliente');
INSERT INTO clientes (id_usuario) VALUES (3), (4);

-- Una marca en el catálogo de cada negocio y un lote en la base de cada uno
-- (un negocio solo puede almacenar marcas que vende [R7])
INSERT INTO marcas (id_marca, nombre) OVERRIDING SYSTEM VALUE VALUES (1, 'Agua Prueba 20L');
INSERT INTO productos_negocio (id_negocio, id_marca, precio, precio_envase, capacidad_maxima)
VALUES (1, 1, 25.00, 30.00, 500), (2, 1, 24.00, 30.00, 300);
INSERT INTO lotes_inventario (id_lote, id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 1, CURRENT_DATE + 90, 50, 50),
       (2, 2, 1, CURRENT_DATE + 90, 30, 30);

INSERT INTO direcciones_clientes (id_direccion, id_cliente, alias, calle, numero_exterior,
                                  colonia, codigo_postal, ubicacion)
OVERRIDING SYSTEM VALUE
VALUES
 (1, 3, 'Casa',    'Municipio Libre', '25',  'Portales Norte', '03303',
    ST_GeogFromText('SRID=4326;POINT(-99.1450 19.3720)')),   -- dentro de la zona
 (2, 3, 'Oficina', 'Reforma',         '222', 'Juárez',         '06600',
    ST_GeogFromText('SRID=4326;POINT(-99.1550 19.4280)')),   -- fuera de la zona
 (3, 4, 'Casa',    'Zapata',          '10',  'Del Valle',      '03100',
    ST_GeogFromText('SRID=4326;POINT(-99.1650 19.3750)'));   -- dentro de la zona

COMMIT;

-- Como la semilla fijó ids manualmente, se avanzan las secuencias de
-- identidad para que los siguientes INSERT sin id no colisionen.
SELECT setval(pg_get_serial_sequence('usuarios',             'id_usuario'),   100);
SELECT setval(pg_get_serial_sequence('negocios',             'id_negocio'),   100);
SELECT setval(pg_get_serial_sequence('vehiculos_negocio',    'id_vehiculo'),  100);
SELECT setval(pg_get_serial_sequence('direcciones_clientes', 'id_direccion'), 100);
SELECT setval(pg_get_serial_sequence('marcas',               'id_marca'),     100);
SELECT setval(pg_get_serial_sequence('lotes_inventario',     'id_lote'),      100);


-- ------------------------------------------
-- 2. COMPROBACIONES POSITIVAS
-- ------------------------------------------

-- 2.1 [G2] El trigger calculó en_zona_cobertura.
--     Esperado: dir 1 = t, dir 2 = f, dir 3 = t
SELECT id_direccion, alias, en_zona_cobertura
FROM direcciones_clientes ORDER BY id_direccion;

-- 2.2 Pedido válido: Carla (cliente 3) pide a su dirección 1.
INSERT INTO pedidos (id_cliente, id_direccion_entrega, id_negocio_solicitado,
                     total_pagar, garrafones_totales)
VALUES (3, 1, 1, 90.00, 2);
INSERT INTO historial_estados_pedido (id_pedido, estado) VALUES (1, 'pendiente');

-- 2.3 Rafa inicia jornada con el vehículo de SU negocio y reporta ubicación.
UPDATE repartidores
SET id_vehiculo_actual = 1,
    estado_operativo   = TRUE,
    ubicacion_actual   = ST_GeogFromText('SRID=4326;POINT(-99.1452 19.3722)'),
    ubicacion_reportada_en = now()
WHERE id_usuario = 2;

-- 2.4 [G3] Proximidad RF-005: ¿a menos de 500 m de la entrega?
--     Esperado: avisar = t, distancia ≈ 30 m
SELECT ST_DWithin(d.ubicacion, r.ubicacion_actual, 500) AS avisar,
       round(ST_Distance(d.ubicacion, r.ubicacion_actual)::numeric, 1) AS distancia_m
FROM pedidos p
JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
JOIN repartidores r         ON r.id_usuario   = 2
WHERE p.id_pedido = 1;

-- 2.5 [G2] Al mover una dirección, el trigger recalcula la zona.
--     Esperado: en_zona_cobertura pasa de f a t
UPDATE direcciones_clientes
SET ubicacion = ST_GeogFromText('SRID=4326;POINT(-99.1600 19.3800)')
WHERE id_direccion = 2;
SELECT id_direccion, en_zona_cobertura FROM direcciones_clientes WHERE id_direccion = 2;


-- ------------------------------------------
-- 3. PRUEBAS NEGATIVAS (el esquema debe rechazarlas)
-- ------------------------------------------

-- 3.1 [A3] Un cliente NO puede ser repartidor (ni, por tanto, dueño de un negocio).
DO $$
BEGIN
    INSERT INTO repartidores (id_usuario, id_negocio, es_dueno)
    VALUES (3, 2, FALSE);  -- 3 = Carla, cliente
    RAISE EXCEPTION 'FALLO 3.1: se permitió un repartidor que es cliente';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.1 [A3]: cliente como repartidor rechazado';
END $$;

-- 3.2 [A2] Un repartidor NO puede figurar como cliente de un pedido.
DO $$
BEGIN
    INSERT INTO pedidos (id_cliente, id_direccion_entrega, id_negocio_solicitado, total_pagar, garrafones_totales)
    VALUES (2, 1, 1, 45.00, 1);  -- 2 = Rafa, repartidor
    RAISE EXCEPTION 'FALLO 3.2: se permitió un pedido con cliente repartidor';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.2 [A2]: pedido hecho por un no-cliente rechazado';
END $$;

-- 3.3 [A5] Un pedido NO puede entregarse en la dirección de OTRO cliente.
DO $$
BEGIN
    INSERT INTO pedidos (id_cliente, id_direccion_entrega, id_negocio_solicitado, total_pagar, garrafones_totales)
    VALUES (3, 3, 1, 45.00, 1);  -- dirección 3 es de Beto, no de Carla
    RAISE EXCEPTION 'FALLO 3.3: se permitió entregar en dirección ajena';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.3 [A5]: entrega en dirección de otro cliente rechazada';
END $$;

-- 3.4 [A3] No se puede cambiar usuarios.rol mientras exista el subtipo.
DO $$
BEGIN
    UPDATE usuarios SET rol = 'cliente' WHERE id_usuario = 2;
    RAISE EXCEPTION 'FALLO 3.4: se permitió cambiar el rol con subtipo vivo';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.4 [A3]: cambio de rol con subtipo existente rechazado';
END $$;

-- 3.5 [A3] Un usuario NO puede estar en dos subtipos a la vez.
DO $$
BEGIN
    INSERT INTO clientes (id_usuario) VALUES (2);  -- 2 ya es repartidor
    RAISE EXCEPTION 'FALLO 3.5: se permitió un doble subtipo';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.5 [A3]: doble subtipo rechazado';
END $$;

-- 3.6 [A6] El vehículo en uso debe ser del negocio del repartidor.
DO $$
BEGIN
    UPDATE repartidores SET id_vehiculo_actual = 2 WHERE id_usuario = 2;
    -- vehículo 2 pertenece al negocio 2; Rafa es del negocio 1
    RAISE EXCEPTION 'FALLO 3.6: se permitió usar un vehículo ajeno';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.6 [A6]: vehículo de otro negocio rechazado';
END $$;

-- 3.7 Dirección de base incompleta (CHECK de negocios).
DO $$
BEGIN
    UPDATE negocios SET calle = 'Sola, sin lo demás' WHERE id_negocio = 2;
    RAISE EXCEPTION 'FALLO 3.7: se permitió una dirección de base incompleta';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'OK 3.7: dirección de base incompleta rechazada';
END $$;

-- 3.8 [R7] Un negocio no puede almacenar una marca que no vende.
DO $$
BEGIN
    INSERT INTO marcas (nombre) VALUES ('Marca sin vender');
    INSERT INTO lotes_inventario (id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual)
    VALUES (1, currval(pg_get_serial_sequence('marcas', 'id_marca')), CURRENT_DATE + 30, 10, 10);
    RAISE EXCEPTION 'FALLO 3.8: se guardó un lote de una marca que el negocio no vende';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.8 [R7]: lote de marca fuera del catálogo rechazado';
END $$;


-- 3.9 [R1] Un negocio no puede tener dos dueños.
DO $$
BEGIN
    INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol)
    VALUES ('Otro', 'Dueño', 'otro@h2togo.mx', 'hash', '5550000099', 'repartidor');
    INSERT INTO repartidores (id_usuario, id_negocio, es_dueno)
    VALUES (currval(pg_get_serial_sequence('usuarios', 'id_usuario')), 1, TRUE);
    RAISE EXCEPTION 'FALLO 3.9: se permitieron dos dueños en un negocio';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'OK 3.9 [R1]: segundo dueño rechazado';
END $$;

-- 3.10 [R1] Un negocio no puede quedarse sin dueño (lo revisa el trigger diferido).
DO $$
BEGIN
    SET CONSTRAINTS ALL IMMEDIATE;
    INSERT INTO negocios (nombre_comercial) VALUES ('Negocio sin dueño');
    RAISE EXCEPTION 'FALLO 3.10: se permitió un negocio sin dueño';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'OK 3.10 [R1]: negocio sin dueño rechazado';
END $$;

-- 3.11 [R3] Un lote no puede cargarse en un vehículo de otro negocio.
DO $$
BEGIN
    INSERT INTO inventario_vehiculo (id_vehiculo, id_lote, id_negocio, cantidad_actual)
    VALUES (1, 2, 2, 5);  -- vehículo 1 es del negocio 1; lote 2 es del negocio 2
    RAISE EXCEPTION 'FALLO 3.11: se cargó un lote en un vehículo de otro negocio';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.11 [R3]: lote en vehículo ajeno rechazado';
END $$;

-- 3.12 [V] Lo apartado no puede superar lo que hay.
DO $$
BEGIN
    UPDATE lotes_inventario SET cantidad_apartada = 60 WHERE id_lote = 1;  -- hay 50
    RAISE EXCEPTION 'FALLO 3.12: se apartaron más garrafones de los que hay';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'OK 3.12 [V]: apartado mayor que la existencia rechazado';
END $$;

-- 3.13 [V] Un pedido entregado debe tener repartidor, vehículo y fecha de entrega.
DO $$
BEGIN
    UPDATE pedidos SET estado_actual = 'entregado' WHERE id_pedido = 1;
    RAISE EXCEPTION 'FALLO 3.13: se marcó entregado un pedido sin repartidor ni fecha';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'OK 3.13 [V]: pedido entregado incompleto rechazado';
END $$;

-- 3.14 [R5] El historial no se modifica ni se borra (RN-013).
DO $$
BEGIN
    UPDATE historial_estados_pedido SET estado = 'cancelado' WHERE id_pedido = 1;
    RAISE EXCEPTION 'FALLO 3.14: se modificó el historial';
EXCEPTION WHEN restrict_violation THEN
    RAISE NOTICE 'OK 3.14 [R5]: modificación del historial rechazada';
END $$;


-- ------------------------------------------
-- 4. SEGURIDAD POR ACTOR [S]
-- ------------------------------------------
-- Igual que el backend: se adopta el rol del actor y su id para esta transacción.

-- 4.1 [S1] Un cliente no tiene permiso sobre el inventario.
DO $$
BEGIN
    PERFORM set_config('role', 'h2togo_cliente', true), set_config('h2togo.id_usuario', '3', true);
    PERFORM count(*) FROM lotes_inventario;
    RAISE EXCEPTION 'FALLO 4.1: un cliente leyó el inventario';
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'OK 4.1 [S1]: el cliente no puede leer lotes_inventario';
END $$;

-- 4.2 [S3] Un cliente no ve los pedidos ni las direcciones de otro.
DO $$
DECLARE
    v_pedidos INT;
    v_direcciones INT;
BEGIN
    PERFORM set_config('role', 'h2togo_cliente', true), set_config('h2togo.id_usuario', '4', true);
    SELECT count(*) INTO v_pedidos FROM pedidos;               -- el pedido 1 es de Carla
    SELECT count(*) INTO v_direcciones FROM direcciones_clientes;
    IF v_pedidos <> 0 OR v_direcciones <> 1 THEN
        RAISE EXCEPTION 'FALLO 4.2: Beto ve % pedidos y % direcciones', v_pedidos, v_direcciones;
    END IF;
    RAISE NOTICE 'OK 4.2 [S3]: Beto solo ve su dirección y ningún pedido ajeno';
    PERFORM set_config('role', 'none', true);
END $$;

-- 4.3 [S4] El público ve el stock disponible por marca, pero no los lotes.
DO $$
DECLARE
    v_disponibles BIGINT;
BEGIN
    PERFORM set_config('role', 'h2togo_publico', true);
    SELECT disponibles INTO v_disponibles FROM v_existencias_negocio WHERE id_negocio = 1;
    IF v_disponibles <> 50 THEN
        RAISE EXCEPTION 'FALLO 4.3: disponibles = %', v_disponibles;
    END IF;
    RAISE NOTICE 'OK 4.3 [S4]: el público ve 50 garrafones disponibles (vista agregada)';
    PERFORM set_config('role', 'none', true);
END $$;

-- 4.4 [S3] Un repartidor solo ve el inventario de su purificadora.
DO $$
DECLARE
    v_lotes INT;
BEGIN
    PERFORM set_config('role', 'h2togo_repartidor', true), set_config('h2togo.id_usuario', '5', true);
    SELECT count(*) INTO v_lotes FROM lotes_inventario;  -- Sofía es del negocio 2
    IF v_lotes <> 1 THEN
        RAISE EXCEPTION 'FALLO 4.4: Sofía ve % lotes', v_lotes;
    END IF;
    RAISE NOTICE 'OK 4.4 [S3]: Sofía solo ve el lote de su negocio';
    PERFORM set_config('role', 'none', true);
END $$;

-- 4.5 [S2] Nadie se da de alta como administrador sin sesión.
DO $$
BEGIN
    PERFORM set_config('role', 'h2togo_publico', true);
    INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol)
    VALUES ('Intruso', 'X', 'intruso@h2togo.mx', 'hash', '5550000098', 'admin');
    RAISE EXCEPTION 'FALLO 4.5: se registró un administrador sin sesión';
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'OK 4.5 [S2]: alta de administrador sin sesión rechazada';
END $$;

-- 4.6 [S2] [R9] Solo el administrador resuelve solicitudes de cambio.
DO $$
BEGIN
    PERFORM set_config('role', 'h2togo_repartidor', true), set_config('h2togo.id_usuario', '2', true);
    UPDATE solicitudes_cambio_perfil SET estado = 'aprobado', id_admin_revisor = 2, fecha_resolucion = now();
    RAISE EXCEPTION 'FALLO 4.6: un repartidor resolvió una solicitud';
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'OK 4.6 [S2]: un repartidor no puede resolver solicitudes';
END $$;

-- 4.7 [S2] Un cliente no puede cambiar su propio rol.
DO $$
BEGIN
    PERFORM set_config('role', 'h2togo_cliente', true), set_config('h2togo.id_usuario', '3', true);
    UPDATE usuarios SET rol = 'admin' WHERE id_usuario = 3;
    RAISE EXCEPTION 'FALLO 4.7: un cliente cambió su rol';
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'OK 4.7 [S2]: cambio de rol por el propio cliente rechazado';
END $$;


-- ------------------------------------------
-- 5. RESUMEN
-- ------------------------------------------
SELECT 'usuarios' AS tabla, count(*) FROM usuarios
UNION ALL SELECT 'clientes', count(*) FROM clientes
UNION ALL SELECT 'repartidores', count(*) FROM repartidores
UNION ALL SELECT 'negocios', count(*) FROM negocios
UNION ALL SELECT 'direcciones_clientes', count(*) FROM direcciones_clientes
UNION ALL SELECT 'pedidos', count(*) FROM pedidos
ORDER BY 1;
