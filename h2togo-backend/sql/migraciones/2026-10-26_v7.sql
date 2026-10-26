-- ==========================================
-- MIGRACIÓN v6 → v7 (2026-10-26)
-- ==========================================
-- Lleva una BD v6 (con 2026-10-25_aprobacion_repartidores.sql ya aplicada) a la
-- estructura de H2ToGo_v7_postgresql.sql SIN perder datos. Ver ese archivo para
-- el porqué de cada cambio ([R], [C], [V], [I], [S]).
--
-- Correr ANTES de desplegar el backend nuevo (Hibernate valida el esquema al
-- arrancar y el backend adopta los roles h2togo_* que crea este script), como el
-- dueño del esquema:
--   psql "postgresql://h2togo:<password>@<host>:5432/h2togo_v6" -v ON_ERROR_STOP=1 \
--        -f h2togo-backend/sql/migraciones/2026-10-26_v7.sql
--
-- Todo va en una transacción: si un paso falla, la BD queda igual que antes.
-- Las sesiones abiertas siguen funcionando (los tokens se convierten a su hash).
-- ==========================================

BEGIN;

-- ------------------------------------------
-- 0. Datos que v7 ya no admite
-- ------------------------------------------

-- [R1] En v6 negocios.id_dueno podía apuntar a un repartidor de OTRO negocio.
DO $$
DECLARE
    v_ids TEXT;
BEGIN
    SELECT string_agg(n.id_negocio::TEXT, ', ') INTO v_ids
    FROM negocios n
    WHERE NOT EXISTS (SELECT 1 FROM repartidores r
                      WHERE r.id_usuario = n.id_dueno AND r.id_negocio = n.id_negocio);
    IF v_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Negocios cuyo dueño no es miembro del negocio: %. Corrige negocios.id_dueno y vuelve a correr.', v_ids;
    END IF;
END $$;

-- [R3] Lotes cargados en un vehículo de otro negocio.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM inventario_vehiculo iv
               JOIN lotes_inventario l ON l.id_lote = iv.id_lote
               JOIN vehiculos_negocio v ON v.id_vehiculo = iv.id_vehiculo
               WHERE l.id_negocio <> v.id_negocio) THEN
        RAISE EXCEPTION 'Hay inventario de vehículo con lote y vehículo de negocios distintos.';
    END IF;
END $$;

-- [R7] Lotes de una marca que el negocio no tiene en su catálogo.
DO $$
DECLARE
    v_ids TEXT;
BEGIN
    SELECT string_agg(l.id_lote::TEXT, ', ') INTO v_ids
    FROM lotes_inventario l
    WHERE NOT EXISTS (SELECT 1 FROM productos_negocio p
                      WHERE p.id_negocio = l.id_negocio AND p.id_marca = l.id_marca);
    IF v_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Lotes de marcas que su negocio no vende: %. Agrega el producto al negocio y vuelve a correr.', v_ids;
    END IF;
END $$;

-- [V] Apartado mayor que la existencia: lo dejaba la devolución de fin de jornada
-- con pedidos aún apartados en el vehículo. Se acota y se informa.
DO $$
DECLARE
    v_n INT;
BEGIN
    UPDATE inventario_vehiculo SET cantidad_apartada = cantidad_actual
    WHERE cantidad_apartada > cantidad_actual;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    IF v_n > 0 THEN
        RAISE NOTICE 'inventario_vehiculo: % filas con apartado mayor que la existencia se acotaron.', v_n;
    END IF;
    UPDATE lotes_inventario SET cantidad_apartada = cantidad_actual
    WHERE cantidad_apartada > cantidad_actual;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    IF v_n > 0 THEN
        RAISE NOTICE 'lotes_inventario: % filas con apartado mayor que la existencia se acotaron.', v_n;
    END IF;
END $$;


-- ------------------------------------------
-- 1. [R1] El dueño pasa a repartidores.es_dueno (sin ciclo)
-- ------------------------------------------

ALTER TABLE repartidores ADD COLUMN es_dueno BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE repartidores r SET es_dueno = TRUE, aprobado = TRUE
FROM negocios n
WHERE n.id_dueno = r.id_usuario AND n.id_negocio = r.id_negocio;

ALTER TABLE negocios DROP CONSTRAINT fk_negocios_dueno;
ALTER TABLE negocios DROP COLUMN id_dueno;

-- La FK ya no necesita ser diferible.
ALTER TABLE repartidores DROP CONSTRAINT fk_repartidores_negocio;
ALTER TABLE repartidores ADD CONSTRAINT fk_repartidores_negocio
    FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio);
ALTER TABLE repartidores ADD CONSTRAINT chk_repartidores_dueno_aprobado CHECK (aprobado OR NOT es_dueno);
UPDATE repartidores SET ubicacion_reportada_en = now()
WHERE ubicacion_actual IS NOT NULL AND ubicacion_reportada_en IS NULL;
UPDATE repartidores SET ubicacion_reportada_en = NULL
WHERE ubicacion_actual IS NULL AND ubicacion_reportada_en IS NOT NULL;
ALTER TABLE repartidores ADD CONSTRAINT chk_repartidores_ubicacion
    CHECK ((ubicacion_actual IS NULL) = (ubicacion_reportada_en IS NULL));

CREATE UNIQUE INDEX uk_repartidores_un_dueno ON repartidores (id_negocio) WHERE es_dueno;

CREATE FUNCTION fn_valida_dueno_unico() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_negocios INT[] := '{}';
    v_negocio  INT;
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_negocios := v_negocios || NEW.id_negocio;
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_negocios := v_negocios || OLD.id_negocio;
    END IF;
    FOREACH v_negocio IN ARRAY v_negocios LOOP
        CONTINUE WHEN NOT EXISTS (SELECT 1 FROM negocios WHERE id_negocio = v_negocio);
        IF (SELECT COUNT(*) FROM repartidores WHERE id_negocio = v_negocio AND es_dueno) <> 1 THEN
            RAISE EXCEPTION 'El negocio % debe tener exactamente un dueño', v_negocio
                USING ERRCODE = 'check_violation';
        END IF;
    END LOOP;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_negocios_tiene_dueno
    AFTER INSERT ON negocios
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_valida_dueno_unico();

CREATE CONSTRAINT TRIGGER trg_repartidores_dueno_unico
    AFTER INSERT OR DELETE OR UPDATE OF es_dueno, id_negocio ON repartidores
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_valida_dueno_unico();

COMMENT ON COLUMN repartidores.es_dueno IS '[R1] TRUE para el dueño del negocio. Índice único parcial: un dueño por negocio; trigger diferido: exactamente uno.';


-- ------------------------------------------
-- 2. [R2] [R3] Inventario sin arcos exclusivos
-- ------------------------------------------

-- Apartados y movimientos guardan siempre el lote y, si aplica, el vehículo.
ALTER TABLE apartados_pedido ADD COLUMN id_lote INT, ADD COLUMN id_vehiculo INT;
UPDATE apartados_pedido SET id_lote = id_lote_base WHERE id_lote_base IS NOT NULL;
UPDATE apartados_pedido a SET id_lote = iv.id_lote, id_vehiculo = iv.id_vehiculo
FROM inventario_vehiculo iv WHERE iv.id_inventario_vehiculo = a.id_inventario_vehiculo;

ALTER TABLE movimientos_inventario ADD COLUMN id_lote INT, ADD COLUMN id_vehiculo INT;
UPDATE movimientos_inventario SET id_lote = id_lote_base WHERE id_lote_base IS NOT NULL;
UPDATE movimientos_inventario m SET id_lote = iv.id_lote, id_vehiculo = iv.id_vehiculo
FROM inventario_vehiculo iv WHERE iv.id_inventario_vehiculo = m.id_inventario_vehiculo;

ALTER TABLE apartados_pedido
    DROP CONSTRAINT chk_apartados_origen,
    DROP CONSTRAINT fk_apartados_lote_base,
    DROP CONSTRAINT fk_apartados_vehiculo,
    DROP COLUMN id_lote_base,
    DROP COLUMN id_inventario_vehiculo,
    ALTER COLUMN id_lote SET NOT NULL;

-- [C2] Proveedor y costo ya están en el lote. [R8] La bitácora se queda sin FKs.
ALTER TABLE movimientos_inventario
    DROP CONSTRAINT chk_mov_origen,
    DROP CONSTRAINT fk_mov_lote_base,
    DROP CONSTRAINT fk_mov_vehiculo,
    DROP CONSTRAINT fk_mov_pedido,
    DROP CONSTRAINT fk_mov_repartidor,
    DROP COLUMN id_lote_base,
    DROP COLUMN id_inventario_vehiculo,
    DROP COLUMN costo_unitario,
    DROP COLUMN proveedor,
    ALTER COLUMN id_lote SET NOT NULL;

-- inventario_vehiculo: llave natural, mismo negocio para lote y vehículo, sin "activo" [C3].
ALTER TABLE inventario_vehiculo ADD COLUMN id_negocio INT;
UPDATE inventario_vehiculo iv SET id_negocio = l.id_negocio
FROM lotes_inventario l WHERE l.id_lote = iv.id_lote;
ALTER TABLE inventario_vehiculo
    ALTER COLUMN id_negocio SET NOT NULL,
    DROP CONSTRAINT fk_iv_vehiculo,
    DROP CONSTRAINT fk_iv_lote,
    DROP CONSTRAINT uk_inventario_vehiculo_lote,
    DROP CONSTRAINT inventario_vehiculo_pkey,
    DROP COLUMN id_inventario_vehiculo,
    DROP COLUMN activo,
    ADD CONSTRAINT pk_inventario_vehiculo PRIMARY KEY (id_vehiculo, id_lote);

ALTER TABLE lotes_inventario ADD CONSTRAINT uk_lote_negocio UNIQUE (id_lote, id_negocio);
-- [R7] Una sola FK al catálogo del negocio en lugar de negocio y marca por separado.
ALTER TABLE lotes_inventario
    DROP CONSTRAINT fk_lote_negocio,
    DROP CONSTRAINT fk_lote_marca,
    ADD CONSTRAINT fk_lote_producto
        FOREIGN KEY (id_negocio, id_marca) REFERENCES productos_negocio (id_negocio, id_marca);
ALTER TABLE inventario_vehiculo
    ADD CONSTRAINT fk_iv_vehiculo
        FOREIGN KEY (id_vehiculo, id_negocio) REFERENCES vehiculos_negocio (id_vehiculo, id_negocio),
    ADD CONSTRAINT fk_iv_lote
        FOREIGN KEY (id_lote, id_negocio)     REFERENCES lotes_inventario (id_lote, id_negocio),
    ADD CONSTRAINT chk_iv_cantidades CHECK (
        cantidad_actual >= 0 AND cantidad_apartada BETWEEN 0 AND cantidad_actual);

ALTER TABLE apartados_pedido
    ADD CONSTRAINT fk_apartados_lote
        FOREIGN KEY (id_lote) REFERENCES lotes_inventario (id_lote),
    ADD CONSTRAINT fk_apartados_vehiculo
        FOREIGN KEY (id_vehiculo, id_lote) REFERENCES inventario_vehiculo (id_vehiculo, id_lote),
    ADD CONSTRAINT chk_apartados_cantidad CHECK (cantidad > 0);

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT chk_mov_cantidad CHECK (cantidad > 0),
    ADD CONSTRAINT chk_mov_origen CHECK (
        (tipo = 'entrada_proveedor' AND id_vehiculo IS NULL)
        OR (tipo IN ('traspaso_a_vehiculo', 'devolucion_a_base', 'salida_pedido') AND id_vehiculo IS NOT NULL)
        OR tipo IN ('salida_manual', 'ajuste')),
    ADD CONSTRAINT chk_mov_salida_pedido CHECK (tipo <> 'salida_pedido' OR id_pedido IS NOT NULL);

ALTER TABLE lotes_inventario
    ADD CONSTRAINT chk_lote_cantidades CHECK (
        cantidad_inicial > 0 AND cantidad_actual >= 0
        AND cantidad_apartada BETWEEN 0 AND cantidad_actual),
    ADD CONSTRAINT chk_lote_costo CHECK (costo_unitario IS NULL OR costo_unitario >= 0);

COMMENT ON TABLE  inventario_vehiculo            IS 'Garrafones de cada lote que van en cada vehículo. Marca y caducidad se leen del lote.';
COMMENT ON COLUMN inventario_vehiculo.id_negocio IS '[R3] Repetido a propósito: las dos FKs compuestas obligan a que lote y vehículo sean del mismo negocio.';
COMMENT ON COLUMN apartados_pedido.id_vehiculo   IS '[R2] NULL = el garrafón sigue en la base. Al cargarlo al vehículo solo se llena esta columna.';
COMMENT ON COLUMN movimientos_inventario.id_lote     IS '[R2] Siempre se conoce el lote. Proveedor y costo se leen de él.';
COMMENT ON COLUMN movimientos_inventario.id_vehiculo IS '[R10] En salida_pedido, el vehículo con que se entregó el pedido.';


-- ------------------------------------------
-- 3. [R4] Solicitudes de cambio
-- ------------------------------------------

ALTER TABLE solicitudes_cambio_perfil
    DROP CONSTRAINT fk_solicitudes_producto_neg,
    DROP CONSTRAINT fk_solicitudes_admin,
    ADD CONSTRAINT fk_solicitudes_revisor
        FOREIGN KEY (id_admin_revisor) REFERENCES usuarios (id_usuario),
    DROP COLUMN id_producto_negocio,
    DROP CONSTRAINT fk_solicitudes_vehiculo,
    ADD CONSTRAINT fk_solicitudes_vehiculo
        FOREIGN KEY (id_vehiculo, id_negocio) REFERENCES vehiculos_negocio (id_vehiculo, id_negocio),
    ADD CONSTRAINT chk_solicitudes_vehiculo CHECK (
        codigo_cambio NOT IN ('DATOS_VEHICULO', 'ELIMINAR_VEHICULO') OR id_vehiculo IS NOT NULL),
    ADD CONSTRAINT chk_solicitudes_resolucion CHECK (
        (estado = 'pendiente' AND id_admin_revisor IS NULL AND fecha_resolucion IS NULL)
        OR (estado <> 'pendiente' AND id_admin_revisor IS NOT NULL AND fecha_resolucion IS NOT NULL));


-- [R9] Sin tabla administradores: no tenía atributos y ya nada la referencia.
DROP TABLE administradores;


-- ------------------------------------------
-- 4. [R5] Bitácoras inmutables
-- ------------------------------------------

CREATE FUNCTION fn_bitacora_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La tabla % es una bitácora: no admite %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END $$;

ALTER TABLE historial_estados_pedido DROP CONSTRAINT fk_historial_pedido;
ALTER TABLE historial_estados_pedido ADD CONSTRAINT fk_historial_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido);

CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial_estados_pedido
    FOR EACH ROW EXECUTE FUNCTION fn_bitacora_inmutable();
CREATE TRIGGER trg_movimientos_inmutable
    BEFORE UPDATE OR DELETE ON movimientos_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_bitacora_inmutable();


-- ------------------------------------------
-- 5. [C1] [C4] [C5] Columnas
-- ------------------------------------------

-- [R6] La FK compuesta a direcciones_clientes ya garantiza que el cliente exista.
ALTER TABLE pedidos DROP CONSTRAINT fk_pedidos_cliente;
-- [R10] El vehículo de cada entrega queda en su movimiento salida_pedido.
ALTER TABLE pedidos DROP COLUMN id_vehiculo_utilizado;

-- es_programado derivada (PostgreSQL 16 no convierte una columna normal en generada).
ALTER TABLE pedidos DROP COLUMN es_programado;
ALTER TABLE pedidos ADD COLUMN es_programado BOOLEAN GENERATED ALWAYS AS (fecha_programada IS NOT NULL) STORED;

-- Token de sesión → su hash SHA-256. Las sesiones abiertas siguen válidas.
ALTER TABLE usuarios ADD COLUMN token_sesion_hash VARCHAR(64);
UPDATE usuarios SET token_sesion_hash = encode(sha256(convert_to(token_sesion, 'UTF8')), 'hex')
WHERE token_sesion IS NOT NULL;
ALTER TABLE usuarios DROP COLUMN token_sesion;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_token_sesion_hash_key UNIQUE (token_sesion_hash);
-- Sesiones a medias (sin fechas) se cierran.
UPDATE usuarios SET token_sesion_hash = NULL, sesion_fecha_creacion = NULL, sesion_fecha_expiracion = NULL
WHERE token_sesion_hash IS NULL OR sesion_fecha_creacion IS NULL OR sesion_fecha_expiracion IS NULL
   OR sesion_fecha_expiracion <= sesion_fecha_creacion;
UPDATE usuarios SET codigo_verificacion = NULL, codigo_verificacion_expiracion = NULL
WHERE (codigo_verificacion IS NULL) <> (codigo_verificacion_expiracion IS NULL);

ALTER TABLE usuarios
    ADD COLUMN codigo_intentos SMALLINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_usuarios_correo CHECK (correo LIKE '%_@_%'),
    ADD CONSTRAINT chk_usuarios_token_hash CHECK (token_sesion_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT chk_usuarios_sesion CHECK (
        (token_sesion_hash IS NULL AND sesion_fecha_creacion IS NULL AND sesion_fecha_expiracion IS NULL)
        OR (token_sesion_hash IS NOT NULL AND sesion_fecha_creacion IS NOT NULL
            AND sesion_fecha_expiracion > sesion_fecha_creacion)),
    ADD CONSTRAINT chk_usuarios_codigo CHECK (
        (codigo_verificacion IS NULL) = (codigo_verificacion_expiracion IS NULL)),
    ADD CONSTRAINT chk_usuarios_intentos CHECK (codigo_intentos BETWEEN 0 AND 10);

COMMENT ON COLUMN usuarios.codigo_intentos   IS '[C5] Intentos fallidos con el código actual. Al llegar a 5 el código se invalida.';
COMMENT ON COLUMN usuarios.token_sesion_hash IS '[C4] SHA-256 (hex) del token opaco. El token en claro solo lo conoce el dispositivo. NULL si no hay sesión.';


-- ------------------------------------------
-- 6. [V] Verificaciones nuevas
-- ------------------------------------------

ALTER TABLE clientes ADD CONSTRAINT chk_clientes_ausencias CHECK (ausencias_consecutivas >= 0);
ALTER TABLE vehiculos_negocio ADD CONSTRAINT chk_vehiculos_capacidad CHECK (capacidad_garrafones > 0);
ALTER TABLE productos_negocio ADD CONSTRAINT chk_pn_valores
    CHECK (precio >= 0 AND precio_envase >= 0 AND capacidad_maxima > 0);

ALTER TABLE horarios_negocio DROP CONSTRAINT chk_horarios_consistencia;
ALTER TABLE horarios_negocio ADD CONSTRAINT chk_horarios_consistencia
    CHECK ((cerrado AND hora_apertura IS NULL AND hora_cierre IS NULL)
        OR (NOT cerrado AND hora_apertura < hora_cierre));

ALTER TABLE pedidos
    ADD CONSTRAINT chk_pedidos_montos CHECK (
        total_pagar >= 0 AND garrafones_totales > 0
        AND (precio_maximo_garrafon IS NULL OR precio_maximo_garrafon > 0)),
    ADD CONSTRAINT chk_pedidos_modalidad CHECK (
        (tipo_solicitud = 'directa' AND id_negocio_solicitado IS NOT NULL AND precio_maximo_garrafon IS NULL)
        OR (tipo_solicitud = 'abierta' AND id_negocio_solicitado IS NULL AND precio_maximo_garrafon IS NOT NULL)),
    ADD CONSTRAINT chk_pedidos_programado CHECK (
        estado_actual <> 'pendiente_programado' OR fecha_programada IS NOT NULL),
    ADD CONSTRAINT chk_pedidos_asignacion CHECK (
        estado_actual NOT IN ('asignado', 'en_camino', 'entregado', 'no_entregado')
        OR id_repartidor IS NOT NULL),
    ADD CONSTRAINT chk_pedidos_fecha_entrega CHECK (
        (estado_actual IN ('entregado', 'no_entregado')) = (fecha_entrega IS NOT NULL));

ALTER TABLE detalles_pedido
    DROP CONSTRAINT chk_detalles_agregada_en_sitio,
    DROP CONSTRAINT chk_detalles_cantidad_entregada,
    DROP CONSTRAINT chk_detalles_precio_envase;
ALTER TABLE detalles_pedido
    ADD CONSTRAINT chk_detalles_cantidad_entregada CHECK (
        cantidad_entregada IS NULL OR cantidad_entregada <= cantidad_solicitada OR agregada_en_sitio),
    ADD CONSTRAINT chk_detalles_precio_envase CHECK (tiene_envase OR precio_envase_unitario IS NOT NULL),
    ADD CONSTRAINT chk_detalles_valores CHECK (
        cantidad_solicitada >= 0 AND (cantidad_entregada IS NULL OR cantidad_entregada >= 0)
        AND precio_unitario >= 0 AND (precio_envase_unitario IS NULL OR precio_envase_unitario >= 0)),
    ADD CONSTRAINT chk_detalles_agregada_en_sitio CHECK (agregada_en_sitio = (cantidad_solicitada = 0));


-- ------------------------------------------
-- 7. [I] Índices
-- ------------------------------------------

DROP INDEX IF EXISTS idx_horarios_negocio;          -- lo cubre uk_horario_negocio_dia
DROP INDEX IF EXISTS idx_clientes_suspendido;       -- ninguna consulta filtra por esa columna
DROP INDEX IF EXISTS idx_pedidos_estado;
DROP INDEX IF EXISTS idx_lotes_caducidad;
DROP INDEX IF EXISTS idx_mov_lote_base_fecha;       -- se fueron con sus columnas, por si acaso
DROP INDEX IF EXISTS idx_mov_vehiculo_fecha;
DROP INDEX IF EXISTS idx_usuarios_sesion_expiracion;

CREATE INDEX idx_pedidos_direccion         ON pedidos (id_direccion_entrega);
CREATE INDEX idx_pedidos_abiertos          ON pedidos (estado_actual, fecha_programada)
    WHERE estado_actual IN ('pendiente', 'pendiente_programado');
CREATE INDEX idx_pedidos_por_activar       ON pedidos (fecha_programada)
    WHERE estado_actual = 'pendiente_programado' AND NOT notificado_programado;
CREATE INDEX idx_detalles_pedido           ON detalles_pedido (id_pedido);
CREATE INDEX idx_lotes_fifo                ON lotes_inventario (id_negocio, id_marca, fecha_caducidad)
    WHERE activo;
CREATE INDEX idx_mov_lote_fecha            ON movimientos_inventario (id_lote, fecha DESC);
CREATE INDEX idx_mov_pedido                ON movimientos_inventario (id_pedido)
    WHERE id_pedido IS NOT NULL;
CREATE INDEX idx_usuarios_sesion_expiracion ON usuarios (sesion_fecha_expiracion)
    WHERE token_sesion_hash IS NOT NULL;


-- ------------------------------------------
-- 8. [S] Seguridad en la base de datos
-- ------------------------------------------
-- Igual que la sección 16 de H2ToGo_v7_postgresql.sql.

DO $$
DECLARE
    v_rol TEXT;
BEGIN
    FOREACH v_rol IN ARRAY ARRAY['h2togo_publico', 'h2togo_cliente', 'h2togo_repartidor',
                                 'h2togo_admin', 'h2togo_sistema'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_rol) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN', v_rol);
        END IF;
    END LOOP;
END $$;

GRANT h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema
    TO CURRENT_USER;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema;

CREATE FUNCTION fn_usuario_actual() RETURNS INT
LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('h2togo.id_usuario', true), '')::INT
$$;

CREATE FUNCTION fn_negocio_actual() RETURNS INT
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT id_negocio FROM repartidores WHERE id_usuario = fn_usuario_actual()
$$;

CREATE FUNCTION fn_es_dueno_actual() RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT COALESCE((SELECT es_dueno FROM repartidores WHERE id_usuario = fn_usuario_actual()), FALSE)
$$;

CREATE FUNCTION fn_pedido_al_alcance(p pedidos) RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
    SELECT p.id_repartidor = fn_usuario_actual()
        OR (p.id_repartidor IS NULL AND p.estado_actual IN ('pendiente', 'pendiente_programado')
            AND (p.tipo_solicitud = 'abierta' OR p.id_negocio_solicitado = fn_negocio_actual()))
$$;

GRANT SELECT, INSERT, UPDATE ON usuarios, clientes, repartidores, zonas_cobertura,
    direcciones_clientes, negocios, horarios_negocio, vehiculos_negocio, marcas, productos_negocio,
    lotes_inventario, inventario_vehiculo, solicitudes_cambio_perfil, pedidos, detalles_pedido,
    apartados_pedido
    TO h2togo_admin, h2togo_sistema;
GRANT SELECT, INSERT ON historial_estados_pedido, movimientos_inventario TO h2togo_admin, h2togo_sistema;
GRANT DELETE ON apartados_pedido, horarios_negocio, solicitudes_cambio_perfil TO h2togo_admin, h2togo_sistema;

GRANT SELECT ON negocios, horarios_negocio, marcas, productos_negocio, zonas_cobertura
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor;

GRANT SELECT, INSERT ON usuarios, clientes, repartidores TO h2togo_publico;
GRANT INSERT ON negocios, horarios_negocio TO h2togo_publico;
GRANT UPDATE (telefono_verificado, codigo_verificacion, codigo_verificacion_expiracion, codigo_intentos,
              password_hash, token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_publico;

GRANT SELECT ON usuarios, clientes, repartidores, vehiculos_negocio TO h2togo_cliente;
GRANT UPDATE (nombre, apellidos, url_foto_perfil, password_hash,
              token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_cliente;
GRANT SELECT, INSERT, UPDATE, DELETE ON direcciones_clientes TO h2togo_cliente;
GRANT SELECT, INSERT ON pedidos, detalles_pedido, historial_estados_pedido TO h2togo_cliente;
GRANT UPDATE (estado_actual) ON pedidos TO h2togo_cliente;

GRANT SELECT ON usuarios, clientes, repartidores, direcciones_clientes, vehiculos_negocio
    TO h2togo_repartidor;
GRANT UPDATE (nombre, apellidos, url_foto_perfil, password_hash,
              token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_repartidor;
GRANT UPDATE (ausencias_consecutivas, suspendido_hasta) ON clientes TO h2togo_repartidor;
GRANT UPDATE (estado_operativo, id_vehiculo_actual, ubicacion_actual, ubicacion_reportada_en)
    ON repartidores TO h2togo_repartidor;
GRANT INSERT, DELETE ON horarios_negocio TO h2togo_repartidor;
GRANT UPDATE (precio, precio_envase) ON productos_negocio TO h2togo_repartidor;
GRANT SELECT, INSERT, UPDATE ON lotes_inventario, inventario_vehiculo TO h2togo_repartidor;
GRANT SELECT, INSERT, UPDATE, DELETE ON apartados_pedido TO h2togo_repartidor;
GRANT SELECT, INSERT ON movimientos_inventario, historial_estados_pedido TO h2togo_repartidor;
GRANT SELECT, INSERT, DELETE ON solicitudes_cambio_perfil TO h2togo_repartidor;
GRANT SELECT ON pedidos, detalles_pedido TO h2togo_repartidor;
GRANT UPDATE (id_repartidor, estado_actual, total_pagar, fecha_entrega)
    ON pedidos TO h2togo_repartidor;
GRANT INSERT, UPDATE ON detalles_pedido TO h2togo_repartidor;

DO $$
DECLARE
    v_tabla TEXT;
BEGIN
    FOREACH v_tabla IN ARRAY ARRAY['usuarios', 'clientes', 'repartidores', 'direcciones_clientes',
            'horarios_negocio', 'vehiculos_negocio', 'productos_negocio', 'lotes_inventario',
            'inventario_vehiculo', 'solicitudes_cambio_perfil', 'pedidos', 'detalles_pedido',
            'apartados_pedido', 'movimientos_inventario', 'historial_estados_pedido'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', v_tabla);
        EXECUTE format('CREATE POLICY pol_%s_backend ON %I TO h2togo_admin, h2togo_sistema '
                       'USING (true) WITH CHECK (true)', v_tabla, v_tabla);
    END LOOP;
END $$;

CREATE POLICY pol_usuarios_publico_leer ON usuarios FOR SELECT TO h2togo_publico USING (true);
CREATE POLICY pol_usuarios_publico_alta ON usuarios FOR INSERT TO h2togo_publico
    WITH CHECK (rol IN ('cliente', 'repartidor'));
CREATE POLICY pol_usuarios_publico_cambio ON usuarios FOR UPDATE TO h2togo_publico USING (true);
CREATE POLICY pol_usuarios_cliente ON usuarios FOR SELECT TO h2togo_cliente USING (
    usuarios.id_usuario = fn_usuario_actual()
    OR usuarios.id_usuario IN (SELECT p.id_repartidor FROM pedidos p));
CREATE POLICY pol_usuarios_repartidor ON usuarios FOR SELECT TO h2togo_repartidor USING (
    usuarios.id_usuario = fn_usuario_actual()
    OR usuarios.id_usuario IN (SELECT r.id_usuario FROM repartidores r)
    OR EXISTS (SELECT 1 FROM pedidos p WHERE p.id_cliente = usuarios.id_usuario AND fn_pedido_al_alcance(p)));
CREATE POLICY pol_usuarios_propio ON usuarios FOR UPDATE TO h2togo_cliente, h2togo_repartidor
    USING (usuarios.id_usuario = fn_usuario_actual());

CREATE POLICY pol_clientes_publico ON clientes FOR SELECT TO h2togo_publico USING (true);
CREATE POLICY pol_clientes_publico_alta ON clientes FOR INSERT TO h2togo_publico WITH CHECK (true);
CREATE POLICY pol_clientes_propio ON clientes FOR SELECT TO h2togo_cliente
    USING (clientes.id_usuario = fn_usuario_actual());
CREATE POLICY pol_clientes_repartidor ON clientes FOR ALL TO h2togo_repartidor USING (
    EXISTS (SELECT 1 FROM pedidos p WHERE p.id_cliente = clientes.id_usuario AND fn_pedido_al_alcance(p)));

CREATE POLICY pol_repartidores_publico ON repartidores FOR SELECT TO h2togo_publico USING (true);
CREATE POLICY pol_repartidores_publico_alta ON repartidores FOR INSERT TO h2togo_publico
    WITH CHECK (es_dueno = aprobado);
CREATE POLICY pol_repartidores_cliente ON repartidores FOR SELECT TO h2togo_cliente
    USING (repartidores.id_usuario IN (SELECT p.id_repartidor FROM pedidos p));
CREATE POLICY pol_repartidores_mi_negocio ON repartidores FOR SELECT TO h2togo_repartidor
    USING (repartidores.id_negocio = fn_negocio_actual());
CREATE POLICY pol_repartidores_propio ON repartidores FOR UPDATE TO h2togo_repartidor
    USING (repartidores.id_usuario = fn_usuario_actual());

CREATE POLICY pol_direcciones_propias ON direcciones_clientes FOR ALL TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual()) WITH CHECK (id_cliente = fn_usuario_actual());
CREATE POLICY pol_direcciones_repartidor ON direcciones_clientes FOR SELECT TO h2togo_repartidor USING (
    EXISTS (SELECT 1 FROM pedidos p
            WHERE p.id_direccion_entrega = direcciones_clientes.id_direccion AND fn_pedido_al_alcance(p)));

CREATE POLICY pol_horarios_leer ON horarios_negocio FOR SELECT
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor USING (true);
CREATE POLICY pol_horarios_alta_registro ON horarios_negocio FOR INSERT TO h2togo_publico WITH CHECK (true);
CREATE POLICY pol_horarios_dueno_alta ON horarios_negocio FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual());
CREATE POLICY pol_horarios_dueno_baja ON horarios_negocio FOR DELETE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual());

CREATE POLICY pol_productos_leer ON productos_negocio FOR SELECT
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor USING (true);
CREATE POLICY pol_productos_dueno ON productos_negocio FOR UPDATE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual());

CREATE POLICY pol_vehiculos_cliente ON vehiculos_negocio FOR SELECT TO h2togo_cliente USING (
    vehiculos_negocio.id_vehiculo IN (SELECT r.id_vehiculo_actual FROM repartidores r));
CREATE POLICY pol_vehiculos_repartidor ON vehiculos_negocio FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());

CREATE POLICY pol_lotes_leer ON lotes_inventario FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
CREATE POLICY pol_lotes_mover ON lotes_inventario FOR UPDATE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
CREATE POLICY pol_lotes_alta ON lotes_inventario FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual());

CREATE POLICY pol_iv_negocio ON inventario_vehiculo FOR ALL TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual()) WITH CHECK (id_negocio = fn_negocio_actual());

CREATE POLICY pol_apartados_repartidor ON apartados_pedido FOR ALL TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM pedidos p
                   WHERE p.id_pedido = apartados_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual())
           AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = apartados_pedido.id_lote))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p
                        WHERE p.id_pedido = apartados_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual())
                AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = apartados_pedido.id_lote));

CREATE POLICY pol_mov_leer ON movimientos_inventario FOR SELECT TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = movimientos_inventario.id_lote));
CREATE POLICY pol_mov_registrar ON movimientos_inventario FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_repartidor_responsable = fn_usuario_actual()
                AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = movimientos_inventario.id_lote));

CREATE POLICY pol_solicitudes_leer ON solicitudes_cambio_perfil FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
CREATE POLICY pol_solicitudes_dueno_alta ON solicitudes_cambio_perfil FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual() AND estado = 'pendiente');
CREATE POLICY pol_solicitudes_dueno_retira ON solicitudes_cambio_perfil FOR DELETE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual() AND estado = 'pendiente');

CREATE POLICY pol_pedidos_cliente ON pedidos FOR SELECT TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual());
CREATE POLICY pol_pedidos_cliente_alta ON pedidos FOR INSERT TO h2togo_cliente
    WITH CHECK (id_cliente = fn_usuario_actual() AND id_repartidor IS NULL
                AND estado_actual IN ('pendiente', 'pendiente_programado'));
CREATE POLICY pol_pedidos_cliente_cancela ON pedidos FOR UPDATE TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual() AND estado_actual IN ('pendiente', 'pendiente_programado'))
    WITH CHECK (id_cliente = fn_usuario_actual() AND estado_actual = 'cancelado');
CREATE POLICY pol_pedidos_repartidor_leer ON pedidos FOR SELECT TO h2togo_repartidor USING (true);
CREATE POLICY pol_pedidos_repartidor ON pedidos FOR UPDATE TO h2togo_repartidor
    USING (id_repartidor IS NULL OR id_repartidor = fn_usuario_actual())
    WITH CHECK (id_repartidor IS NULL OR id_repartidor = fn_usuario_actual());

CREATE POLICY pol_detalles_cliente ON detalles_pedido FOR ALL TO h2togo_cliente
    USING (EXISTS (SELECT 1 FROM pedidos p WHERE p.id_pedido = detalles_pedido.id_pedido))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p WHERE p.id_pedido = detalles_pedido.id_pedido
                        AND p.estado_actual IN ('pendiente', 'pendiente_programado')));
CREATE POLICY pol_detalles_repartidor_leer ON detalles_pedido FOR SELECT TO h2togo_repartidor USING (true);
CREATE POLICY pol_detalles_repartidor ON detalles_pedido FOR ALL TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM pedidos p
                   WHERE p.id_pedido = detalles_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual()))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p
                        WHERE p.id_pedido = detalles_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual()));

CREATE POLICY pol_historial_cliente ON historial_estados_pedido FOR ALL TO h2togo_cliente
    USING (EXISTS (SELECT 1 FROM pedidos p WHERE p.id_pedido = historial_estados_pedido.id_pedido))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p WHERE p.id_pedido = historial_estados_pedido.id_pedido)
                AND estado IN ('pendiente', 'pendiente_programado', 'cancelado'));
CREATE POLICY pol_historial_repartidor ON historial_estados_pedido FOR ALL TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM pedidos p
                   WHERE p.id_pedido = historial_estados_pedido.id_pedido AND fn_pedido_al_alcance(p)))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p
                        WHERE p.id_pedido = historial_estados_pedido.id_pedido
                          AND p.id_repartidor = fn_usuario_actual()));

CREATE FUNCTION fn_liberar_apartados(p_id_pedido INT) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_rol TEXT := current_setting('role');
BEGIN
    IF v_rol = 'h2togo_publico'
       OR (v_rol = 'h2togo_cliente' AND NOT EXISTS (
               SELECT 1 FROM pedidos WHERE id_pedido = p_id_pedido AND id_cliente = fn_usuario_actual()))
       OR (v_rol = 'h2togo_repartidor' AND NOT EXISTS (
               SELECT 1 FROM pedidos WHERE id_pedido = p_id_pedido AND id_repartidor = fn_usuario_actual())) THEN
        RAISE EXCEPTION 'Sin permiso para liberar los apartados del pedido %', p_id_pedido
            USING ERRCODE = 'insufficient_privilege';
    END IF;

    UPDATE inventario_vehiculo iv SET cantidad_apartada = iv.cantidad_apartada - a.total
    FROM (SELECT id_vehiculo, id_lote, SUM(cantidad) AS total FROM apartados_pedido
          WHERE id_pedido = p_id_pedido AND id_vehiculo IS NOT NULL
          GROUP BY id_vehiculo, id_lote) a
    WHERE iv.id_vehiculo = a.id_vehiculo AND iv.id_lote = a.id_lote;

    UPDATE lotes_inventario l SET cantidad_apartada = l.cantidad_apartada - a.total
    FROM (SELECT id_lote, SUM(cantidad) AS total FROM apartados_pedido
          WHERE id_pedido = p_id_pedido AND id_vehiculo IS NULL
          GROUP BY id_lote) a
    WHERE l.id_lote = a.id_lote;

    DELETE FROM apartados_pedido WHERE id_pedido = p_id_pedido;
END $$;

CREATE FUNCTION fn_aprobar_repartidor(p_id_repartidor INT) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
    UPDATE repartidores SET aprobado = TRUE
    WHERE id_usuario = p_id_repartidor AND NOT aprobado
      AND (current_setting('role') IN ('h2togo_admin', 'h2togo_sistema', 'none')
           OR (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual()));
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Solo el dueño del negocio aprueba a sus repartidores pendientes'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
END $$;

CREATE FUNCTION fn_rechazar_repartidor(p_id_repartidor INT) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
    PERFORM 1 FROM repartidores
    WHERE id_usuario = p_id_repartidor AND NOT aprobado
      AND (current_setting('role') IN ('h2togo_admin', 'h2togo_sistema', 'none')
           OR (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual()));
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Solo el dueño del negocio rechaza a sus repartidores pendientes'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    UPDATE usuarios SET cuenta_activa = FALSE, motivo_baja = 'Solicitud rechazada por el dueño',
        fecha_baja = now(), token_sesion_hash = NULL, token_fcm = NULL,
        sesion_fecha_creacion = NULL, sesion_fecha_expiracion = NULL
    WHERE id_usuario = p_id_repartidor;
END $$;

REVOKE ALL ON FUNCTION fn_liberar_apartados(INT), fn_aprobar_repartidor(INT), fn_rechazar_repartidor(INT),
    fn_negocio_actual(), fn_es_dueno_actual(), fn_valida_dueno_unico() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION fn_liberar_apartados(INT)
    TO h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema;
GRANT EXECUTE ON FUNCTION fn_aprobar_repartidor(INT), fn_rechazar_repartidor(INT)
    TO h2togo_repartidor, h2togo_admin, h2togo_sistema;
GRANT EXECUTE ON FUNCTION fn_negocio_actual(), fn_es_dueno_actual()
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema;

CREATE VIEW v_existencias_negocio AS
    SELECT id_negocio, id_marca, SUM(cantidad_actual - cantidad_apartada)::BIGINT AS disponibles
    FROM lotes_inventario
    WHERE activo
    GROUP BY id_negocio, id_marca;

CREATE VIEW v_repartidores_por_negocio AS
    SELECT id_negocio, COUNT(*)::BIGINT AS repartidores
    FROM repartidores
    GROUP BY id_negocio;

COMMENT ON VIEW v_existencias_negocio      IS '[S4] Stock disponible (no apartado) por negocio y marca. Única forma en que clientes y público ven inventario.';
COMMENT ON VIEW v_repartidores_por_negocio IS '[S4] Cuántos repartidores tiene cada negocio, sin exponer quiénes son ni dónde están.';

GRANT SELECT ON v_existencias_negocio, v_repartidores_por_negocio
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema;

COMMIT;


-- ------------------------------------------
-- Opcional (recomendado): usuario de login propio para el backend
-- ------------------------------------------
-- Hoy el backend entra con el dueño del esquema. Con un usuario sin permisos
-- propios, cualquier consulta que no adopte un rol h2togo_* falla en lugar de
-- tener acceso total. Después hay que cambiar DB_USER y DB_PASSWORD del backend.
--
--   CREATE ROLE h2togo_app LOGIN PASSWORD '<una contraseña nueva>' NOINHERIT;
--   GRANT h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema
--       TO h2togo_app;
--   GRANT CONNECT ON DATABASE h2togo_v6 TO h2togo_app;
