-- ==========================================
-- PRUEBAS DEL ESQUEMA H2ToGo v6 (PostgreSQL + PostGIS)
-- ==========================================
-- Ejecutar DESPUÉS de H2ToGo_v6_postgresql.sql, en una BD desechable:
--   \i pruebas_v6.sql
--
-- Secciones:
--   1. Datos semilla (incluye el alta dueño+negocio en una transacción,
--      que es la razón de las FKs diferibles [P2]).
--   2. Comprobaciones del cálculo de zona de cobertura y de proximidad.
--   3. Pruebas negativas: operaciones que el esquema DEBE rechazar.
--      Cada bloque termina en "OK ..." si la BD rechazó la operación.
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
INSERT INTO administradores (id_usuario) VALUES (1);

-- Dueño (repartidor) + su negocio: el ciclo se resuelve porque las FKs
-- negocios.id_dueno y repartidores.id_negocio se verifican al COMMIT [P2].
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (2, 'Rafa', 'Repartidor', 'rafa@h2togo.mx', 'hash', '5550000002', 'repartidor');

INSERT INTO negocios (id_negocio, nombre_comercial, id_dueno,
                      calle, numero_exterior, colonia, codigo_postal, ubicacion_base)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Purificadora El Manantial', 2,
        'Eje Central', '100', 'Portales', '03300',
        ST_GeogFromText('SRID=4326;POINT(-99.1500 19.3700)'));

INSERT INTO repartidores (id_usuario, id_negocio) VALUES (2, 1);

-- Segundo negocio (sin dirección de base todavía: caso permitido por el CHECK)
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol)
OVERRIDING SYSTEM VALUE
VALUES (5, 'Sofía', 'Repartidora', 'sofia@h2togo.mx', 'hash', '5550000005', 'repartidor');

INSERT INTO negocios (id_negocio, nombre_comercial, id_dueno)
OVERRIDING SYSTEM VALUE
VALUES (2, 'Purificadora Azul', 5);

INSERT INTO repartidores (id_usuario, id_negocio) VALUES (5, 2);

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

-- 3.1 [A2] Un cliente NO puede ser dueño de un negocio.
DO $$
BEGIN
    SET CONSTRAINTS ALL IMMEDIATE;
    INSERT INTO negocios (nombre_comercial, id_dueno)
    VALUES ('Negocio inválido', 3);  -- 3 = Carla, cliente
    RAISE EXCEPTION 'FALLO 3.1: se permitió un negocio con dueño cliente';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.1 [A2]: negocio con dueño cliente rechazado';
END $$;

-- 3.2 [A2] Un repartidor NO puede figurar como cliente de un pedido.
DO $$
BEGIN
    INSERT INTO pedidos (id_cliente, id_direccion_entrega, total_pagar, garrafones_totales)
    VALUES (2, 1, 45.00, 1);  -- 2 = Rafa, repartidor
    RAISE EXCEPTION 'FALLO 3.2: se permitió un pedido con cliente repartidor';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.2 [A2]: pedido hecho por un no-cliente rechazado';
END $$;

-- 3.3 [A5] Un pedido NO puede entregarse en la dirección de OTRO cliente.
DO $$
BEGIN
    INSERT INTO pedidos (id_cliente, id_direccion_entrega, total_pagar, garrafones_totales)
    VALUES (3, 3, 45.00, 1);  -- dirección 3 es de Beto, no de Carla
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

-- 3.8 [A2] Solo un administrador puede revisar solicitudes.
DO $$
BEGIN
    INSERT INTO solicitudes_cambio_perfil (codigo_cambio, id_negocio, id_admin_revisor, valor_nuevo)
    VALUES ('NOMBRE_NEGOCIO', 1, 2, 'Otro nombre');  -- 2 = repartidor, no admin
    RAISE EXCEPTION 'FALLO 3.8: se permitió un revisor que no es admin';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'OK 3.8 [A2]: revisor no administrador rechazado';
END $$;


-- ------------------------------------------
-- 4. RESUMEN
-- ------------------------------------------
SELECT 'usuarios' AS tabla, count(*) FROM usuarios
UNION ALL SELECT 'clientes', count(*) FROM clientes
UNION ALL SELECT 'repartidores', count(*) FROM repartidores
UNION ALL SELECT 'administradores', count(*) FROM administradores
UNION ALL SELECT 'negocios', count(*) FROM negocios
UNION ALL SELECT 'direcciones_clientes', count(*) FROM direcciones_clientes
UNION ALL SELECT 'pedidos', count(*) FROM pedidos
ORDER BY 1;
