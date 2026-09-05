-- ==========================================
-- DATOS DEMO H2ToGo — para la consola de pruebas (F13) y demostraciones
-- ==========================================
-- Ejecutar en una BD ya inicializada con, en este orden:
--   1) H2ToGo_v6_postgresql.sql          (esquema v6)
--   2) h2togo-backend/sql/zona_benito_juarez.sql   (polígono real de cobertura)
--   3) data-demo.sql                     (este archivo)
--
-- Deja lista una purificadora operable con un cliente, listos para el flujo
-- completo desde la consola web (/console/). Todos los usuarios quedan
-- verificados y comparten la contraseña:  Demo1234
--
-- Coordenadas alineadas al grafo OSM de Benito Juárez para que el motor de
-- ruteo encuentre ruta:
--   negocio / origen del repartidor : 19.376692, -99.165057
--   dirección de entrega (destino)  : 19.377934, -99.171138
--
-- Los ids se fijan (OVERRIDING SYSTEM VALUE) para que coincidan con los valores
-- por defecto de la consola: negocio=1, marca=1, vehiculo=1, dirección=1,
-- producto=1. En una BD real los genera la identidad de cada columna.
-- ==========================================

BEGIN;

-- Contraseña BCrypt compartida (texto plano: Demo1234)
-- (generada con el mismo BCryptPasswordEncoder del backend)
-- $2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku

-- ---- Administrador -----------------------------------------------------
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Adela', 'Admin', 'admin@h2togo.mx',
        '$2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku', '5550000001', 'admin', TRUE);
INSERT INTO administradores (id_usuario) VALUES (1);

-- ---- Dueño (repartidor) + su negocio (ciclo diferido resuelto en la tx) --
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
OVERRIDING SYSTEM VALUE
VALUES (2, 'Rodrigo', 'Repartidor', 'dueno@h2togo.mx',
        '$2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku', '5550000002', 'repartidor', TRUE);

INSERT INTO negocios (id_negocio, nombre_comercial, id_dueno,
                      calle, numero_exterior, colonia, codigo_postal, ubicacion_base)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Purificadora Demo', 2,
        'Av. Insurgentes Sur', '1234', 'Del Valle', '03100',
        ST_GeogFromText('SRID=4326;POINT(-99.165057 19.376692)'));

INSERT INTO repartidores (id_usuario, id_negocio) VALUES (2, 1);

-- ---- Cliente ------------------------------------------------------------
INSERT INTO usuarios (id_usuario, nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
OVERRIDING SYSTEM VALUE
VALUES (3, 'Carla', 'Cliente', 'cliente@h2togo.mx',
        '$2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku', '5550000003', 'cliente', TRUE);
INSERT INTO clientes (id_usuario) VALUES (3);

-- ---- Vehículo del negocio ----------------------------------------------
INSERT INTO vehiculos_negocio (id_vehiculo, id_negocio, tipo_vehiculo, marca, color, capacidad_garrafones)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'motocicleta', 'Italika', 'azul', 30);

-- ---- Catálogo: marca + producto del negocio -----------------------------
INSERT INTO marcas (id_marca, nombre) OVERRIDING SYSTEM VALUE
VALUES (1, 'Agua Demo 20L');

INSERT INTO productos_negocio (id_producto_negocio, id_negocio, id_marca, precio, precio_envase, capacidad_maxima)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 1, 25.00, 30.00, 500);

-- ---- Horario: abierto los 7 días 00:00–23:59 (siempre "abierto ahora") --
INSERT INTO horarios_negocio (id_negocio, dia_semana, cerrado, hora_apertura, hora_cierre)
VALUES (1,1,FALSE,'00:00','23:59'), (1,2,FALSE,'00:00','23:59'), (1,3,FALSE,'00:00','23:59'),
       (1,4,FALSE,'00:00','23:59'), (1,5,FALSE,'00:00','23:59'), (1,6,FALSE,'00:00','23:59'),
       (1,7,FALSE,'00:00','23:59');

-- ---- Inventario en base: un lote amplio y vigente -----------------------
INSERT INTO lotes_inventario (id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual, proveedor)
VALUES (1, 1, CURRENT_DATE + INTERVAL '6 months', 100, 100, 'Distribuidora Demo');

-- ---- Dirección del cliente (destino, dentro de cobertura y del grafo) ----
INSERT INTO direcciones_clientes (id_direccion, id_cliente, alias, calle, numero_exterior,
                                  colonia, codigo_postal, referencias, ubicacion)
OVERRIDING SYSTEM VALUE
VALUES (1, 3, 'Casa', 'Calle Nueva York', '250', 'Nápoles', '03810',
        'Portón azul, timbre 2',
        ST_GeogFromText('SRID=4326;POINT(-99.171138 19.377934)'));

COMMIT;

-- Avanzar las secuencias de identidad tras fijar ids manualmente.
SELECT setval(pg_get_serial_sequence('usuarios',             'id_usuario'),          100);
SELECT setval(pg_get_serial_sequence('negocios',             'id_negocio'),          100);
SELECT setval(pg_get_serial_sequence('vehiculos_negocio',    'id_vehiculo'),         100);
SELECT setval(pg_get_serial_sequence('marcas',               'id_marca'),            100);
SELECT setval(pg_get_serial_sequence('productos_negocio',    'id_producto_negocio'), 100);
SELECT setval(pg_get_serial_sequence('direcciones_clientes', 'id_direccion'),        100);

-- Verificación rápida del cálculo de cobertura (esperado: t).
SELECT id_direccion, alias, en_zona_cobertura FROM direcciones_clientes WHERE id_direccion = 1;
