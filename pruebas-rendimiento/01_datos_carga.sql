-- ============================================================================
-- H2ToGo · Datos para las pruebas de carga y rendimiento (CC-03, RE-01, RE-02, RE-04)
--
-- Crea, aparte de los datos reales:
--   - la purificadora "Prueba de Carga" con su marca, horario 24/7, un lote de
--     5 000 garrafones y 10 vehículos;
--   - 10 repartidores  carga.rep01 … carga.rep10 @carga.h2togo.test (rep01 es el dueño);
--   - 100 clientes     carga.cli001 … carga.cli100 @carga.h2togo.test, cada uno con
--     una dirección en un punto distinto de Benito Juárez.
-- Todos usan la contraseña Demo1234 y ya están verificados.
--
-- Los pedidos de la prueba son directos a esta purificadora: los repartidores reales
-- no los ven ni reciben avisos. Al terminar, correr 99_limpieza_carga.sql.
--
-- Correr como el dueño del esquema (h2togo). Si ya hay datos de carga, se detiene.
-- ============================================================================
DO $$
DECLARE
    v_marca    INT;
    v_negocio  INT;
    v_usuario  INT;
    v_vehiculo INT;
    i          INT;
    punto      geometry;
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE correo LIKE '%@carga.h2togo.test') THEN
        RAISE EXCEPTION 'Ya hay datos de carga. Corre 99_limpieza_carga.sql antes de volver a sembrar.';
    END IF;

    INSERT INTO marcas (nombre) VALUES ('Prueba de carga 20L') RETURNING id_marca INTO v_marca;

    INSERT INTO negocios (nombre_comercial, calle, numero_exterior, colonia, codigo_postal, ubicacion_base)
    VALUES ('Purificadora Prueba de Carga', 'Av. Cuauhtémoc', '1000', 'Narvarte', '03020',
            ST_GeogFromText('SRID=4326;POINT(-99.1560 19.3880)'))
    RETURNING id_negocio INTO v_negocio;

    INSERT INTO horarios_negocio (id_negocio, dia_semana, cerrado, hora_apertura, hora_cierre)
    SELECT v_negocio, d, FALSE, '00:00', '23:59' FROM generate_series(1, 7) AS d;

    INSERT INTO productos_negocio (id_negocio, id_marca, precio, precio_envase, capacidad_maxima)
    VALUES (v_negocio, v_marca, 30.00, 40.00, 10000);

    INSERT INTO lotes_inventario (id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual, proveedor)
    VALUES (v_negocio, v_marca, CURRENT_DATE + 180, 5000, 5000, 'Prueba de carga');

    -- Repartidores: el primero es el dueño. Cada uno con su vehículo.
    FOR i IN 1..10 LOOP
        INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
        VALUES ('Repartidor', 'Carga ' || i, format('carga.rep%s@carga.h2togo.test', lpad(i::text, 2, '0')),
                '$2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku',
                '559900' || lpad(i::text, 4, '0'), 'repartidor', TRUE)
        RETURNING id_usuario INTO v_usuario;
        INSERT INTO repartidores (id_usuario, id_negocio, es_dueno) VALUES (v_usuario, v_negocio, i = 1);
        INSERT INTO vehiculos_negocio (id_negocio, tipo_vehiculo, marca, color, capacidad_garrafones)
        VALUES (v_negocio, 'camioneta', 'Carga', 'blanco', 100)
        RETURNING id_vehiculo INTO v_vehiculo;
    END LOOP;

    -- Clientes: 100 puntos reproducibles dentro del polígono de cobertura.
    i := 0;
    FOR punto IN
        SELECT (ST_Dump(ST_GeneratePoints(z.geom::geometry, 100, 2026))).geom
        FROM zonas_cobertura z WHERE z.activo ORDER BY z.id_zona LIMIT 100
    LOOP
        i := i + 1;
        INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
        VALUES ('Cliente', 'Carga ' || i, format('carga.cli%s@carga.h2togo.test', lpad(i::text, 3, '0')),
                '$2a$10$LTorDuTcMQc/Q1fI.e2cmuiIY.VQJ07K4F5zJqp1eltkdpAM9.pku',
                '559910' || lpad(i::text, 4, '0'), 'cliente', TRUE)
        RETURNING id_usuario INTO v_usuario;
        INSERT INTO clientes (id_usuario) VALUES (v_usuario);
        INSERT INTO direcciones_clientes (id_cliente, alias, calle, numero_exterior, colonia, codigo_postal, ubicacion)
        VALUES (v_usuario, 'Casa', 'Calle de prueba', i::text, 'Benito Juárez', '03000', punto::geography);
    END LOOP;

    RAISE NOTICE 'Datos de carga listos: negocio %, marca %, 10 repartidores, % clientes.', v_negocio, v_marca, i;
END $$;

-- Verificación (esperado: 10 | 100 | 100).
SELECT (SELECT count(*) FROM repartidores r JOIN usuarios u USING (id_usuario)
        WHERE u.correo LIKE '%@carga.h2togo.test')                         AS repartidores,
       (SELECT count(*) FROM clientes c JOIN usuarios u USING (id_usuario)
        WHERE u.correo LIKE '%@carga.h2togo.test')                         AS clientes,
       (SELECT count(*) FROM direcciones_clientes d JOIN usuarios u ON u.id_usuario = d.id_cliente
        WHERE u.correo LIKE '%@carga.h2togo.test' AND d.en_zona_cobertura) AS direcciones_en_cobertura;
