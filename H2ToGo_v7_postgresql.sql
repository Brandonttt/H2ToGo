-- ==========================================
-- BASE DE DATOS: H2ToGo (v7) — PostgreSQL 16 + PostGIS 3
-- Proyecto: Trabajo Terminal 2026-B176
-- ==========================================
--
-- Requisitos: PostgreSQL >= 16 con la extensión PostGIS instalada.
-- Ejecución (BD nueva):
--   CREATE DATABASE h2togo_v6;   (el nombre de la BD no cambia entre versiones)
--   \c h2togo_v6
--   \i H2ToGo_v7_postgresql.sql
-- Una BD v6 existente se actualiza con
--   h2togo-backend/sql/migraciones/2026-10-26_v7.sql
--
-- Cambios respecto a v6: menos relaciones (34 → 24) y una tabla menos, sin ciclos;
-- más verificaciones, índices revisados y seguridad aplicada por la propia BD.
--
-- RELACIONES [R]
--  [R1] Sin ciclos. v6 tenía negocios.id_dueno → repartidores y
--       repartidores.id_negocio → negocios (FKs diferibles). Ahora el dueño es
--       repartidores.es_dueno; un índice único parcial permite un solo dueño por
--       negocio y un trigger diferido exige que haya exactamente uno. Además el
--       dueño queda obligado a pertenecer a su negocio (v6 no lo garantizaba).
--  [R2] Sin arcos exclusivos en el inventario. apartados_pedido y
--       movimientos_inventario guardaban id_lote_base O id_inventario_vehiculo.
--       Ahora siempre guardan el lote (id_lote) y, si el garrafón está en un
--       vehículo, id_vehiculo; la FK compuesta (id_vehiculo, id_lote) apunta a
--       inventario_vehiculo. Se elimina la redundancia de los traspasos, que
--       guardaban el lote dos veces (id_lote_base y el de inventario_vehiculo).
--  [R3] inventario_vehiculo usa la llave natural (id_vehiculo, id_lote) en lugar de
--       un id sustituto, y lleva id_negocio con FKs compuestas: el lote y el
--       vehículo deben ser del mismo negocio (v6 no lo garantizaba).
--  [R4] solicitudes_cambio_perfil: el vehículo debe ser del negocio que solicita
--       (FK compuesta). Se elimina id_producto_negocio: ningún tipo de cambio lo
--       usaba (AGREGAR_PRODUCTO crea el producto; los precios se editan directo).
--  [R5] Bitácoras inmutables (RN-013): historial_estados_pedido y
--       movimientos_inventario rechazan UPDATE y DELETE (trigger) y el historial
--       ya no se borra en cascada con el pedido.
--  [R6] pedidos.id_cliente sin FK propia: la FK compuesta (id_direccion_entrega,
--       id_cliente) → direcciones_clientes ya obliga a que el cliente exista.
--  [R7] lotes_inventario apunta a productos_negocio (id_negocio, id_marca) en lugar de
--       a negocios y a marcas por separado: un negocio solo almacena lo que vende.
--  [R8] movimientos_inventario es una bitácora sin FKs: guarda los ids como datos.
--       Se escribe en la misma transacción que mueve las existencias y nunca cambia.
--  [R9] Sin tabla administradores (no tenía atributos). id_admin_revisor apunta a
--       usuarios; que solo un admin resuelva lo garantizan los permisos [S2]: ningún
--       otro rol puede modificar solicitudes.
--  [R10] Sin pedidos.id_vehiculo_utilizado: el vehículo de cada entrega queda en su
--       movimiento salida_pedido.
--
-- COLUMNAS [C]
--  [C1] pedidos.es_programado pasa a columna generada (fecha_programada IS NOT NULL).
--  [C2] movimientos_inventario pierde proveedor y costo_unitario: ya viven en el lote.
--  [C3] inventario_vehiculo pierde activo: nunca se ponía en FALSE (la existencia es
--       cantidad_actual > 0).
--  [C4] usuarios.token_sesion → token_sesion_hash (SHA-256 del token). Si se filtra
--       un respaldo de la BD, los tokens no sirven para entrar.
--  [C5] usuarios.codigo_intentos: el código SMS se invalida tras 5 intentos fallidos.
--
-- VERIFICACIONES [V]
--  Cantidades y precios no negativos, apartado <= existencia, horario con apertura
--  antes del cierre, coherencia de pedidos por modalidad, estado y fechas, sesión
--  completa o vacía, solicitudes resueltas con revisor y fecha (ver cada tabla).
--
-- ÍNDICES [I]
--  + detalles_pedido(id_pedido) y pedidos(id_direccion_entrega), que faltaban.
--  + Índices parciales para pedidos abiertos, programados por activar, lotes
--    activos y sesiones abiertas.
--  - horarios_negocio(id_negocio) y clientes(suspendido_hasta): redundante y sin uso.
--
-- SEGURIDAD EN LA BD [S]  (sección 16)
--  [S1] Un rol de PostgreSQL por actor (publico, cliente, repartidor, admin,
--       sistema). El backend adopta en cada conexión el rol del usuario de la
--       petición: aunque el código tuviera un error, un cliente no puede leer
--       inventario porque su rol no tiene permiso sobre esas tablas.
--  [S2] Permisos mínimos por tabla y por columna (p. ej. nadie salvo el admin puede
--       cambiar usuarios.rol ni usuarios.cuenta_activa).
--  [S3] Seguridad por filas (RLS): cada cliente ve solo sus pedidos y direcciones;
--       cada repartidor solo el inventario y los vehículos de su purificadora.
--  [S4] Lo que un actor hace sobre datos de otro pasa por funciones SECURITY DEFINER
--       que validan quién llama (liberar apartados al cancelar, aprobar o rechazar
--       repartidores). Al público solo se le expone el stock agregado (vistas).
--
-- Resumen: 19 → 18 tablas, 34 → 24 llaves foráneas, 0 ciclos, 0 FKs diferibles,
-- 0 arcos exclusivos.
-- ==========================================

CREATE EXTENSION IF NOT EXISTS postgis;


-- ------------------------------------------
-- 0. TIPOS ENUM
-- ------------------------------------------
-- Agregar un valor: ALTER TYPE ... ADD VALUE (quitar uno requiere migración).

CREATE TYPE rol_usuario AS ENUM ('cliente', 'repartidor', 'admin');

CREATE TYPE tipo_vehiculo AS ENUM
    ('motocicleta', 'automovil', 'camioneta', 'triciclo_carga', 'bicicleta_carga');

CREATE TYPE codigo_cambio_perfil AS ENUM
    ('FOTO_PERFIL', 'DATOS_VEHICULO', 'AGREGAR_VEHICULO', 'ELIMINAR_VEHICULO',
     'NOMBRE_NEGOCIO', 'DIRECCION_BASE', 'AGREGAR_PRODUCTO');

CREATE TYPE estado_solicitud AS ENUM ('pendiente', 'aprobado', 'rechazado');

CREATE TYPE tipo_solicitud_pedido AS ENUM ('directa', 'abierta');

CREATE TYPE estado_pedido AS ENUM
    ('pendiente', 'pendiente_programado', 'asignado', 'en_camino',
     'entregado', 'cancelado', 'no_entregado');

CREATE TYPE tipo_movimiento AS ENUM
    ('entrada_proveedor', 'traspaso_a_vehiculo', 'devolucion_a_base',
     'salida_pedido', 'salida_manual', 'ajuste');


-- ------------------------------------------
-- 0b. FUNCIONES DE TRIGGER
-- ------------------------------------------

-- PostgreSQL no tiene ON UPDATE CURRENT_TIMESTAMP.
CREATE FUNCTION fn_touch_fecha_actualizacion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END $$;

-- en_zona_cobertura se deriva del polígono, no se captura a mano.
CREATE FUNCTION fn_calcula_zona_cobertura() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.en_zona_cobertura := EXISTS (
        SELECT 1
        FROM zonas_cobertura z
        WHERE z.activo
          AND ST_Covers(z.geom, NEW.ubicacion)
    );
    RETURN NEW;
END $$;

-- [R5] Las bitácoras solo admiten INSERT (RN-013).
CREATE FUNCTION fn_bitacora_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La tabla % es una bitácora: no admite %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END $$;


-- ------------------------------------------
-- 1. USUARIOS (identidad, credenciales y sesión)
-- ------------------------------------------
-- Solo lo común a los tres actores. Un usuario = un dispositivo activo.

CREATE TABLE usuarios (
    id_usuario                       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                           VARCHAR(100) NOT NULL,
    apellidos                        VARCHAR(100) NOT NULL,
    correo                           VARCHAR(150) NOT NULL UNIQUE,
    password_hash                    VARCHAR(255) NOT NULL,
    telefono                         VARCHAR(20)  NOT NULL UNIQUE,
    rol                              rol_usuario  NOT NULL,
    url_foto_perfil                  VARCHAR(500),
    telefono_verificado              BOOLEAN      NOT NULL DEFAULT FALSE,
    cuenta_activa                    BOOLEAN      NOT NULL DEFAULT TRUE,
    codigo_verificacion              VARCHAR(10),
    codigo_verificacion_expiracion   TIMESTAMPTZ,
    codigo_intentos                  SMALLINT     NOT NULL DEFAULT 0,
    motivo_baja                      VARCHAR(255),
    fecha_baja                       TIMESTAMPTZ,
    fecha_registro                   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Sesión activa (un solo dispositivo por cuenta)
    token_sesion_hash                VARCHAR(64) UNIQUE,
    token_fcm                        VARCHAR(255),
    sesion_fecha_creacion            TIMESTAMPTZ,
    sesion_fecha_expiracion          TIMESTAMPTZ,

    -- Habilita las FKs compuestas (id_usuario, rol) de los subtipos.
    CONSTRAINT uk_usuarios_id_rol UNIQUE (id_usuario, rol),
    CONSTRAINT chk_usuarios_correo CHECK (correo LIKE '%_@_%'),
    -- [C4] Hash SHA-256 en hexadecimal.
    CONSTRAINT chk_usuarios_token_hash CHECK (token_sesion_hash ~ '^[0-9a-f]{64}$'),
    -- La sesión se abre y se cierra completa.
    CONSTRAINT chk_usuarios_sesion CHECK (
        (token_sesion_hash IS NULL AND sesion_fecha_creacion IS NULL AND sesion_fecha_expiracion IS NULL)
        OR (token_sesion_hash IS NOT NULL AND sesion_fecha_creacion IS NOT NULL
            AND sesion_fecha_expiracion > sesion_fecha_creacion)),
    -- El código SMS siempre lleva su vencimiento.
    CONSTRAINT chk_usuarios_codigo CHECK (
        (codigo_verificacion IS NULL) = (codigo_verificacion_expiracion IS NULL)),
    CONSTRAINT chk_usuarios_intentos CHECK (codigo_intentos BETWEEN 0 AND 10)
);

COMMENT ON COLUMN usuarios.rol                 IS 'Discriminador del subtipo. Sin DEFAULT: el alta siempre declara el rol y crea la fila del subtipo en la misma transacción.';
COMMENT ON COLUMN usuarios.telefono_verificado IS 'TRUE cuando el usuario validó el código recibido por SMS (RN-002)';
COMMENT ON COLUMN usuarios.cuenta_activa       IS 'FALSE cuando el admin da de baja al usuario (CU-015) o el dueño rechaza su solicitud';
COMMENT ON COLUMN usuarios.codigo_verificacion IS 'Código numérico enviado por SMS (verificar teléfono o recuperar contraseña)';
COMMENT ON COLUMN usuarios.codigo_intentos     IS '[C5] Intentos fallidos con el código actual. Al llegar a 5 el código se invalida.';
COMMENT ON COLUMN usuarios.token_sesion_hash   IS '[C4] SHA-256 (hex) del token opaco. El token en claro solo lo conoce el dispositivo. NULL si no hay sesión.';
COMMENT ON COLUMN usuarios.token_fcm           IS 'Token Firebase Cloud Messaging del dispositivo activo.';


-- ------------------------------------------
-- 2. SUBTIPOS POR ACTOR
-- ------------------------------------------
-- Herencia por tablas con exclusividad: cada subtipo fija su rol con CHECK y lo
-- empata contra usuarios con una FK compuesta, así un id no puede estar en dos
-- subtipos ni contradecir usuarios.rol. El administrador no tiene datos propios:
-- no necesita subtabla [R9].

CREATE TABLE clientes (
    id_usuario               INT PRIMARY KEY,
    rol                      rol_usuario NOT NULL DEFAULT 'cliente'
                               CONSTRAINT chk_clientes_rol CHECK (rol = 'cliente'),
    ausencias_consecutivas   INT NOT NULL DEFAULT 0
                               CONSTRAINT chk_clientes_ausencias CHECK (ausencias_consecutivas >= 0),
    suspendido_hasta         TIMESTAMPTZ,

    CONSTRAINT fk_clientes_usuario
        FOREIGN KEY (id_usuario, rol) REFERENCES usuarios (id_usuario, rol)
        ON DELETE CASCADE
);

COMMENT ON COLUMN clientes.ausencias_consecutivas IS 'Ausencias consecutivas del cliente (RN-006). Vuelve a 0 con una entrega exitosa.';
COMMENT ON COLUMN clientes.suspendido_hasta       IS 'Suspensión temporal automática por ausencias (RN-006). NULL si no está suspendido.';



-- ------------------------------------------
-- 3. ZONAS DE COBERTURA
-- ------------------------------------------

CREATE TABLE zonas_cobertura (
    id_zona   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL UNIQUE,
    geom      geography(MultiPolygon, 4326) NOT NULL,
    activo    BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE zonas_cobertura IS 'Polígonos de la zona de servicio (límite de la Alcaldía Benito Juárez exportado de OSM). Una dirección está en cobertura si alguna zona activa la cubre (ST_Covers).';


-- ------------------------------------------
-- 4. DIRECCIONES DE ENTREGA (solo clientes)
-- ------------------------------------------

CREATE TABLE direcciones_clientes (
    id_direccion        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente          INT NOT NULL,
    alias               VARCHAR(50)  NOT NULL,
    calle               VARCHAR(150) NOT NULL,
    numero_exterior     VARCHAR(20)  NOT NULL,
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100) NOT NULL,
    codigo_postal       VARCHAR(10)  NOT NULL,
    referencias         TEXT,
    ubicacion           geography(Point, 4326) NOT NULL,
    en_zona_cobertura   BOOLEAN NOT NULL DEFAULT FALSE,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,

    -- Permite que pedidos exija "dirección del mismo cliente".
    CONSTRAINT uk_direcciones_id_cliente UNIQUE (id_direccion, id_cliente),
    CONSTRAINT fk_direcciones_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes (id_usuario)
        ON DELETE CASCADE
);

COMMENT ON COLUMN direcciones_clientes.referencias       IS 'Nota fija a la dirección, ej. "casa azul, frente al parque"';
COMMENT ON COLUMN direcciones_clientes.en_zona_cobertura IS 'Derivada: la asigna el trigger trg_direcciones_zona con ST_Covers. No la escribe el backend.';

CREATE TRIGGER trg_direcciones_zona
    BEFORE INSERT OR UPDATE OF ubicacion ON direcciones_clientes
    FOR EACH ROW EXECUTE FUNCTION fn_calcula_zona_cobertura();


-- ------------------------------------------
-- 5. NEGOCIOS (purificadoras)
-- ------------------------------------------
-- [R1] Ya no apunta a su dueño: el dueño se marca en repartidores.es_dueno.

CREATE TABLE negocios (
    id_negocio          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_comercial    VARCHAR(150) NOT NULL,
    -- Dirección de la base como atributo propio del negocio
    calle               VARCHAR(150),
    numero_exterior     VARCHAR(20),
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100),
    codigo_postal       VARCHAR(10),
    referencias         TEXT,
    ubicacion_base      geography(Point, 4326),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- La dirección de la base se registra completa o no se registra.
    CONSTRAINT chk_negocios_direccion_completa CHECK (
        (calle IS NULL AND numero_exterior IS NULL AND colonia IS NULL
             AND codigo_postal IS NULL AND ubicacion_base IS NULL)
        OR
        (calle IS NOT NULL AND numero_exterior IS NOT NULL AND colonia IS NOT NULL
             AND codigo_postal IS NOT NULL AND ubicacion_base IS NOT NULL)
    )
);

COMMENT ON COLUMN negocios.ubicacion_base IS 'Domicilio físico donde se almacenan los garrafones.';


-- ------------------------------------------
-- 6. VEHÍCULOS DEL NEGOCIO
-- ------------------------------------------

CREATE TABLE vehiculos_negocio (
    id_vehiculo            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_negocio             INT NOT NULL,
    tipo_vehiculo          tipo_vehiculo NOT NULL,
    marca                  VARCHAR(50)  NOT NULL,
    modelo                 VARCHAR(50),
    color                  VARCHAR(30)  NOT NULL,
    placas                 VARCHAR(20),
    capacidad_garrafones   INT          NOT NULL
                             CONSTRAINT chk_vehiculos_capacidad CHECK (capacidad_garrafones > 0),
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Destino de las FKs compuestas "vehículo de este negocio".
    CONSTRAINT uk_vehiculo_negocio UNIQUE (id_vehiculo, id_negocio),
    CONSTRAINT fk_vehiculos_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio) ON DELETE CASCADE
);


-- ------------------------------------------
-- 7. REPARTIDORES (subtipo + datos operativos)
-- ------------------------------------------

CREATE TABLE repartidores (
    id_usuario               INT PRIMARY KEY,
    rol                      rol_usuario NOT NULL DEFAULT 'repartidor'
                               CONSTRAINT chk_repartidores_rol CHECK (rol = 'repartidor'),
    id_negocio               INT NOT NULL,
    -- [R1] Dueño del negocio (RN-021 / RN-024). Uno y solo uno por negocio.
    es_dueno                 BOOLEAN NOT NULL DEFAULT FALSE,
    -- Quien se une a una purificadora existente queda pendiente hasta que el dueño lo aprueba.
    aprobado                 BOOLEAN NOT NULL DEFAULT TRUE,
    estado_operativo         BOOLEAN NOT NULL DEFAULT FALSE,
    id_vehiculo_actual       INT,
    ubicacion_actual         geography(Point, 4326),
    ubicacion_reportada_en   TIMESTAMPTZ,

    CONSTRAINT fk_repartidores_usuario
        FOREIGN KEY (id_usuario, rol) REFERENCES usuarios (id_usuario, rol)
        ON DELETE CASCADE,
    CONSTRAINT fk_repartidores_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio),
    -- El vehículo en uso debe pertenecer al negocio del propio repartidor.
    CONSTRAINT fk_repartidores_vehiculo_actual
        FOREIGN KEY (id_vehiculo_actual, id_negocio)
        REFERENCES vehiculos_negocio (id_vehiculo, id_negocio),
    -- El dueño no puede estar pendiente de aprobación.
    CONSTRAINT chk_repartidores_dueno_aprobado CHECK (aprobado OR NOT es_dueno),
    CONSTRAINT chk_repartidores_ubicacion CHECK ((ubicacion_actual IS NULL) = (ubicacion_reportada_en IS NULL))
);

COMMENT ON COLUMN repartidores.es_dueno               IS '[R1] TRUE para el dueño del negocio. Índice único parcial: un dueño por negocio; trigger diferido: exactamente uno.';
COMMENT ON COLUMN repartidores.aprobado               IS 'FALSE mientras el dueño no aprueba a un repartidor que se unió a su purificadora existente.';
COMMENT ON COLUMN repartidores.estado_operativo       IS 'FALSE = desconectado, TRUE = en jornada y disponible';
COMMENT ON COLUMN repartidores.id_vehiculo_actual     IS 'Vehículo de la jornada actual. NULL si está desconectado.';
COMMENT ON COLUMN repartidores.ubicacion_actual       IS 'Última posición reportada por la app. Alimenta el aviso de proximidad a 500 m (RF-005) y el orden de pedidos cercanos (RF-008).';
COMMENT ON COLUMN repartidores.ubicacion_reportada_en IS 'Cuándo se reportó ubicacion_actual. Permite descartar posiciones viejas.';

-- [R1] A lo más un dueño por negocio...
CREATE UNIQUE INDEX uk_repartidores_un_dueno ON repartidores (id_negocio) WHERE es_dueno;

-- ...y al menos uno, revisado al COMMIT: el alta crea el negocio y luego a su dueño
-- en la misma transacción, y una transferencia quita un dueño antes de poner otro.
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


-- ------------------------------------------
-- 8. HORARIOS DEL NEGOCIO
-- ------------------------------------------

CREATE TABLE horarios_negocio (
    id_horario          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_negocio          INT NOT NULL,
    dia_semana          SMALLINT NOT NULL,
    hora_apertura       TIME,
    hora_cierre         TIME,
    cerrado             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_horario_negocio_dia UNIQUE (id_negocio, dia_semana),
    CONSTRAINT fk_horarios_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio) ON DELETE CASCADE,
    CONSTRAINT chk_horarios_dia
        CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horarios_consistencia
        CHECK ((cerrado AND hora_apertura IS NULL AND hora_cierre IS NULL)
            OR (NOT cerrado AND hora_apertura < hora_cierre))
);

COMMENT ON COLUMN horarios_negocio.dia_semana IS '1=lunes ... 7=domingo';
COMMENT ON COLUMN horarios_negocio.cerrado    IS 'TRUE si el negocio no opera ese día';


-- ------------------------------------------
-- 9. MARCAS Y CATÁLOGO DE PRECIOS POR NEGOCIO
-- ------------------------------------------

CREATE TABLE marcas (
    id_marca   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL UNIQUE,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE
);


CREATE TABLE productos_negocio (
    id_producto_negocio   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_negocio            INT NOT NULL,
    id_marca              INT NOT NULL,
    precio                NUMERIC(10,2) NOT NULL,
    precio_envase         NUMERIC(10,2) NOT NULL,
    capacidad_maxima      INT NOT NULL,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_producto_negocio UNIQUE (id_negocio, id_marca),
    CONSTRAINT fk_pn_negocio FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio),
    CONSTRAINT fk_pn_marca   FOREIGN KEY (id_marca)   REFERENCES marcas (id_marca),
    CONSTRAINT chk_pn_valores CHECK (precio >= 0 AND precio_envase >= 0 AND capacidad_maxima > 0)
);

COMMENT ON COLUMN productos_negocio.precio           IS 'Precio del agua cuando el cliente trae envase propio (RN-025).';
COMMENT ON COLUMN productos_negocio.precio_envase    IS 'Precio adicional del envase cuando el cliente NO trae envase (RN-031).';
COMMENT ON COLUMN productos_negocio.capacidad_maxima IS 'Capacidad máxima de almacenamiento en la base para esta marca.';


-- ------------------------------------------
-- 10. INVENTARIO POR LOTES: BASE Y VEHÍCULO
-- ------------------------------------------

CREATE TABLE lotes_inventario (
    id_lote               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_negocio            INT NOT NULL,
    id_marca              INT NOT NULL,
    fecha_caducidad       DATE NOT NULL,
    fecha_entrada         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cantidad_inicial      INT NOT NULL,
    cantidad_actual       INT NOT NULL DEFAULT 0,
    cantidad_apartada     INT NOT NULL DEFAULT 0,
    proveedor             VARCHAR(150),
    costo_unitario        NUMERIC(10,2),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- [R3] Destino de la FK compuesta "lote de este negocio".
    CONSTRAINT uk_lote_negocio UNIQUE (id_lote, id_negocio),
    -- [R7] Una sola FK: el negocio debe vender esa marca.
    CONSTRAINT fk_lote_producto
        FOREIGN KEY (id_negocio, id_marca) REFERENCES productos_negocio (id_negocio, id_marca),
    CONSTRAINT chk_lote_cantidades CHECK (
        cantidad_inicial > 0 AND cantidad_actual >= 0
        AND cantidad_apartada BETWEEN 0 AND cantidad_actual),
    CONSTRAINT chk_lote_costo CHECK (costo_unitario IS NULL OR costo_unitario >= 0)
);

COMMENT ON COLUMN lotes_inventario.fecha_caducidad   IS 'Fecha de caducidad del lote (RN-030)';
COMMENT ON COLUMN lotes_inventario.cantidad_actual   IS 'Garrafones de este lote que están en la base';
COMMENT ON COLUMN lotes_inventario.cantidad_apartada IS 'De los que están en la base, cuántos ya están reservados para pedidos';
COMMENT ON COLUMN lotes_inventario.activo            IS 'FALSE cuando el lote se agotó o se descartó por caducidad';

CREATE TRIGGER trg_lotes_touch
    BEFORE UPDATE ON lotes_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_touch_fecha_actualizacion();


-- [R3] Llave natural (vehículo, lote) y mismo negocio para ambos.
CREATE TABLE inventario_vehiculo (
    id_vehiculo              INT NOT NULL,
    id_lote                  INT NOT NULL,
    id_negocio               INT NOT NULL,
    cantidad_actual          INT NOT NULL DEFAULT 0,
    cantidad_apartada        INT NOT NULL DEFAULT 0,
    fecha_actualizacion      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_inventario_vehiculo PRIMARY KEY (id_vehiculo, id_lote),
    CONSTRAINT fk_iv_vehiculo
        FOREIGN KEY (id_vehiculo, id_negocio) REFERENCES vehiculos_negocio (id_vehiculo, id_negocio),
    CONSTRAINT fk_iv_lote
        FOREIGN KEY (id_lote, id_negocio)     REFERENCES lotes_inventario (id_lote, id_negocio),
    CONSTRAINT chk_iv_cantidades CHECK (
        cantidad_actual >= 0 AND cantidad_apartada BETWEEN 0 AND cantidad_actual)
);

COMMENT ON TABLE  inventario_vehiculo            IS 'Garrafones de cada lote que van en cada vehículo. Marca y caducidad se leen del lote.';
COMMENT ON COLUMN inventario_vehiculo.id_negocio IS '[R3] Repetido a propósito: las dos FKs compuestas obligan a que lote y vehículo sean del mismo negocio.';

CREATE TRIGGER trg_inventario_vehiculo_touch
    BEFORE UPDATE ON inventario_vehiculo
    FOR EACH ROW EXECUTE FUNCTION fn_touch_fecha_actualizacion();


-- ------------------------------------------
-- 11. SOLICITUDES DE CAMBIO DE PERFIL (RN-019)
-- ------------------------------------------

CREATE TABLE solicitudes_cambio_perfil (
    id_solicitud             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_cambio            codigo_cambio_perfil NOT NULL,
    id_negocio               INT NOT NULL,
    id_vehiculo              INT,
    id_admin_revisor         INT,
    valor_anterior           TEXT,
    valor_nuevo              TEXT NOT NULL,
    estado                   estado_solicitud NOT NULL DEFAULT 'pendiente',
    comentario_admin         VARCHAR(500),
    fecha_solicitud          TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_resolucion         TIMESTAMPTZ,

    CONSTRAINT fk_solicitudes_negocio
        FOREIGN KEY (id_negocio)       REFERENCES negocios (id_negocio),
    -- [R4] El vehículo debe ser del negocio que solicita.
    CONSTRAINT fk_solicitudes_vehiculo
        FOREIGN KEY (id_vehiculo, id_negocio) REFERENCES vehiculos_negocio (id_vehiculo, id_negocio),
    -- [R9] Solo el rol h2togo_admin puede resolver (permisos de la sección 16).
    CONSTRAINT fk_solicitudes_revisor
        FOREIGN KEY (id_admin_revisor) REFERENCES usuarios (id_usuario),
    CONSTRAINT chk_solicitudes_vehiculo CHECK (
        codigo_cambio NOT IN ('DATOS_VEHICULO', 'ELIMINAR_VEHICULO') OR id_vehiculo IS NOT NULL),
    -- Pendiente = sin revisor ni fecha; resuelta = con ambos.
    CONSTRAINT chk_solicitudes_resolucion CHECK (
        (estado = 'pendiente' AND id_admin_revisor IS NULL AND fecha_resolucion IS NULL)
        OR (estado <> 'pendiente' AND id_admin_revisor IS NOT NULL AND fecha_resolucion IS NOT NULL))
);

COMMENT ON COLUMN solicitudes_cambio_perfil.codigo_cambio  IS 'Tipo de cambio solicitado. El solicitante siempre es el dueño del negocio (RN-021).';
COMMENT ON COLUMN solicitudes_cambio_perfil.valor_anterior IS 'Valor previo (JSON si son varios campos).';


-- ------------------------------------------
-- 12. PEDIDOS Y DETALLES
-- ------------------------------------------

CREATE TABLE pedidos (
    id_pedido                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente                 INT NOT NULL,
    id_negocio_solicitado      INT,
    id_repartidor              INT,
    id_direccion_entrega       INT NOT NULL,
    tipo_solicitud             tipo_solicitud_pedido NOT NULL DEFAULT 'directa',
    precio_maximo_garrafon     NUMERIC(10,2),
    estado_actual              estado_pedido NOT NULL DEFAULT 'pendiente',
    total_pagar                NUMERIC(10,2) NOT NULL,
    garrafones_totales         INT NOT NULL,
    indicaciones_entrega       VARCHAR(500),
    fecha_programada           TIMESTAMPTZ,
    -- [C1] Derivada: ya no puede contradecir a fecha_programada.
    es_programado              BOOLEAN GENERATED ALWAYS AS (fecha_programada IS NOT NULL) STORED,
    notificado_programado      BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion             TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_entrega              TIMESTAMPTZ,

    CONSTRAINT fk_pedidos_negocio_solicitado
        FOREIGN KEY (id_negocio_solicitado)   REFERENCES negocios (id_negocio),
    CONSTRAINT fk_pedidos_repartidor
        FOREIGN KEY (id_repartidor)           REFERENCES repartidores (id_usuario),
    -- [R6] La dirección de entrega debe ser del MISMO cliente que pide; como cada
    -- dirección apunta a su cliente, esta FK también garantiza que el cliente exista.
    CONSTRAINT fk_pedidos_direccion_del_cliente
        FOREIGN KEY (id_direccion_entrega, id_cliente)
        REFERENCES direcciones_clientes (id_direccion, id_cliente),

    CONSTRAINT chk_pedidos_montos CHECK (
        total_pagar >= 0 AND garrafones_totales > 0
        AND (precio_maximo_garrafon IS NULL OR precio_maximo_garrafon > 0)),
    -- Directa: a un negocio elegido. Abierta: a quien cumpla el precio máximo.
    CONSTRAINT chk_pedidos_modalidad CHECK (
        (tipo_solicitud = 'directa' AND id_negocio_solicitado IS NOT NULL AND precio_maximo_garrafon IS NULL)
        OR (tipo_solicitud = 'abierta' AND id_negocio_solicitado IS NULL AND precio_maximo_garrafon IS NOT NULL)),
    CONSTRAINT chk_pedidos_programado CHECK (
        estado_actual <> 'pendiente_programado' OR fecha_programada IS NOT NULL),
    -- Desde asignado en adelante hay repartidor (el cancelado puede o no tenerlo).
    CONSTRAINT chk_pedidos_asignacion CHECK (
        estado_actual NOT IN ('asignado', 'en_camino', 'entregado', 'no_entregado')
        OR id_repartidor IS NOT NULL),
    -- Solo los pedidos cerrados en el domicilio tienen fecha de entrega.
    CONSTRAINT chk_pedidos_fecha_entrega CHECK (
        (estado_actual IN ('entregado', 'no_entregado')) = (fecha_entrega IS NOT NULL))
);

COMMENT ON COLUMN pedidos.id_negocio_solicitado  IS 'Negocio elegido por el cliente (modalidad directa)';
COMMENT ON COLUMN pedidos.id_repartidor          IS 'Repartidor que reservó o aceptó el pedido';
COMMENT ON COLUMN pedidos.tipo_solicitud         IS 'directa = eligió negocio; abierta = con precio máximo';
COMMENT ON COLUMN pedidos.total_pagar            IS 'Total ESTIMADO al crear el pedido; el definitivo se recalcula al cerrar con cantidad_entregada (RN-033).';
COMMENT ON COLUMN pedidos.garrafones_totales     IS 'Snapshot de los garrafones solicitados (suma de detalles.cantidad_solicitada, que no cambia). Se guarda para ordenar y planear rutas sin sumar detalles.';
COMMENT ON COLUMN pedidos.notificado_programado  IS 'TRUE cuando el proceso periódico ya activó este programado y avisó a los repartidores.';


CREATE TABLE detalles_pedido (
    id_detalle             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido              INT NOT NULL,
    id_marca               INT NOT NULL,
    cantidad_solicitada    INT NOT NULL,
    cantidad_entregada     INT,
    tiene_envase           BOOLEAN NOT NULL,
    precio_unitario        NUMERIC(10,2) NOT NULL,
    precio_envase_unitario NUMERIC(10,2),
    agregada_en_sitio      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_detalles_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_marca
        FOREIGN KEY (id_marca)  REFERENCES marcas (id_marca),

    CONSTRAINT chk_detalles_valores CHECK (
        cantidad_solicitada >= 0 AND (cantidad_entregada IS NULL OR cantidad_entregada >= 0)
        AND precio_unitario >= 0 AND (precio_envase_unitario IS NULL OR precio_envase_unitario >= 0)),
    CONSTRAINT chk_detalles_cantidad_entregada
        CHECK (cantidad_entregada IS NULL
            OR cantidad_entregada <= cantidad_solicitada
            OR agregada_en_sitio),
    -- Solo las líneas agregadas en sitio nacen sin cantidad solicitada.
    CONSTRAINT chk_detalles_agregada_en_sitio
        CHECK (agregada_en_sitio = (cantidad_solicitada = 0)),
    CONSTRAINT chk_detalles_precio_envase
        CHECK (tiene_envase OR precio_envase_unitario IS NOT NULL)
);

COMMENT ON COLUMN detalles_pedido.cantidad_solicitada IS 'Garrafones pedidos originalmente. No cambia después de creado el pedido.';
COMMENT ON COLUMN detalles_pedido.cantidad_entregada  IS 'Garrafones realmente entregados. NULL hasta cerrar el pedido. Permite entregas parciales (RN-032).';
COMMENT ON COLUMN detalles_pedido.precio_unitario     IS 'Snapshot del precio del agua al momento del pedido (RN-025).';
COMMENT ON COLUMN detalles_pedido.agregada_en_sitio   IS 'TRUE si la línea la creó el repartidor durante la entrega (RN-032).';


-- ------------------------------------------
-- 13. APARTADOS DE INVENTARIO POR PEDIDO  [R2]
-- ------------------------------------------

CREATE TABLE apartados_pedido (
    id_apartado      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido        INT NOT NULL,
    id_lote          INT NOT NULL,
    -- NULL: apartado en la base. Con valor: apartado en ese vehículo.
    id_vehiculo      INT,
    cantidad         INT NOT NULL
                       CONSTRAINT chk_apartados_cantidad CHECK (cantidad > 0),
    fecha_apartado   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_apartados_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_apartados_lote
        FOREIGN KEY (id_lote)   REFERENCES lotes_inventario (id_lote),
    CONSTRAINT fk_apartados_vehiculo
        FOREIGN KEY (id_vehiculo, id_lote) REFERENCES inventario_vehiculo (id_vehiculo, id_lote)
);

COMMENT ON COLUMN apartados_pedido.id_vehiculo IS '[R2] NULL = el garrafón sigue en la base. Al cargarlo al vehículo solo se llena esta columna.';


-- ------------------------------------------
-- 14. BITÁCORAS: MOVIMIENTOS DE INVENTARIO E HISTORIAL DE PEDIDOS  [R2] [R5]
-- ------------------------------------------

-- [R8] Bitácora sin FKs: los ids son datos de lo que pasó. Se escribe en la misma
-- transacción que mueve las existencias (que sí están protegidas) y nunca cambia.
CREATE TABLE movimientos_inventario (
    id_movimiento              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo                       tipo_movimiento NOT NULL,
    id_lote                    INT NOT NULL,
    -- NULL: el movimiento solo toca la base. Con valor: también ese vehículo.
    id_vehiculo                INT,
    cantidad                   INT NOT NULL,
    id_pedido                  INT,
    id_repartidor_responsable  INT,
    notas                      VARCHAR(255),
    fecha                      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_mov_cantidad CHECK (cantidad > 0),
    -- El tipo determina si interviene un vehículo.
    CONSTRAINT chk_mov_origen CHECK (
        (tipo = 'entrada_proveedor' AND id_vehiculo IS NULL)
        OR (tipo IN ('traspaso_a_vehiculo', 'devolucion_a_base', 'salida_pedido') AND id_vehiculo IS NOT NULL)
        OR tipo IN ('salida_manual', 'ajuste')),
    CONSTRAINT chk_mov_salida_pedido CHECK (tipo <> 'salida_pedido' OR id_pedido IS NOT NULL)
);

COMMENT ON COLUMN movimientos_inventario.cantidad IS 'Siempre positiva; el tipo determina la dirección del movimiento';
COMMENT ON COLUMN movimientos_inventario.id_lote     IS '[R2] Siempre se conoce el lote. Proveedor y costo se leen de él.';
COMMENT ON COLUMN movimientos_inventario.id_vehiculo IS '[R10] En salida_pedido, el vehículo con que se entregó el pedido.';

CREATE TRIGGER trg_movimientos_inmutable
    BEFORE UPDATE OR DELETE ON movimientos_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_bitacora_inmutable();


CREATE TABLE historial_estados_pedido (
    id_historial          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido             INT NOT NULL,
    estado                estado_pedido NOT NULL,
    notas_adicionales     VARCHAR(500),
    ubicacion             geography(Point, 4326),
    fecha_cambio          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- [R5] Sin ON DELETE CASCADE: borrar un pedido con historial falla.
    CONSTRAINT fk_historial_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido)
);

COMMENT ON COLUMN historial_estados_pedido.notas_adicionales IS 'Ej. motivo de no entrega: cliente_ausente, direccion_incorrecta, sin_pago, etc.';
COMMENT ON COLUMN historial_estados_pedido.ubicacion         IS 'Ubicación del repartidor al momento del cambio de estado';

CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial_estados_pedido
    FOR EACH ROW EXECUTE FUNCTION fn_bitacora_inmutable();


-- ==========================================
-- 15. ÍNDICES  [I]
-- ==========================================
-- Las PK y UNIQUE ya crean su índice; aquí van los de consultas frecuentes y FKs.

-- Pedidos
CREATE INDEX idx_pedidos_cliente_fecha     ON pedidos (id_cliente, fecha_creacion DESC);
CREATE INDEX idx_pedidos_repartidor_estado ON pedidos (id_repartidor, estado_actual);
CREATE INDEX idx_pedidos_negocio_estado    ON pedidos (id_negocio_solicitado, estado_actual);
CREATE INDEX idx_pedidos_direccion         ON pedidos (id_direccion_entrega);
-- Pedidos que buscan repartidor (disponibles y agenda del día).
CREATE INDEX idx_pedidos_abiertos          ON pedidos (estado_actual, fecha_programada)
    WHERE estado_actual IN ('pendiente', 'pendiente_programado');
-- Programados que el proceso periódico aún debe activar.
CREATE INDEX idx_pedidos_por_activar       ON pedidos (fecha_programada)
    WHERE estado_actual = 'pendiente_programado' AND NOT notificado_programado;

CREATE INDEX idx_detalles_pedido           ON detalles_pedido (id_pedido);
CREATE INDEX idx_historial_pedido_fecha    ON historial_estados_pedido (id_pedido, fecha_cambio);
CREATE INDEX idx_apartados_pedido          ON apartados_pedido (id_pedido);

-- Inventario: FIFO por caducidad sobre lotes vigentes.
CREATE INDEX idx_lotes_fifo                ON lotes_inventario (id_negocio, id_marca, fecha_caducidad)
    WHERE activo;
CREATE INDEX idx_mov_lote_fecha            ON movimientos_inventario (id_lote, fecha DESC);
CREATE INDEX idx_mov_pedido                ON movimientos_inventario (id_pedido)
    WHERE id_pedido IS NOT NULL;

-- Por negocio
CREATE INDEX idx_direcciones_cliente       ON direcciones_clientes (id_cliente);
CREATE INDEX idx_repartidores_negocio      ON repartidores (id_negocio);
CREATE INDEX idx_vehiculos_negocio         ON vehiculos_negocio (id_negocio);
CREATE INDEX idx_solicitudes_estado        ON solicitudes_cambio_perfil (estado, fecha_solicitud DESC);
CREATE INDEX idx_solicitudes_negocio       ON solicitudes_cambio_perfil (id_negocio);

-- Limpieza de sesiones vencidas.
CREATE INDEX idx_usuarios_sesion_expiracion ON usuarios (sesion_fecha_expiracion)
    WHERE token_sesion_hash IS NOT NULL;

-- Índices espaciales (GiST)
CREATE INDEX idx_direcciones_ubicacion     ON direcciones_clientes USING GIST (ubicacion);
CREATE INDEX idx_negocios_ubicacion_base   ON negocios             USING GIST (ubicacion_base);
CREATE INDEX idx_repartidores_ubicacion    ON repartidores         USING GIST (ubicacion_actual);
CREATE INDEX idx_zonas_geom                ON zonas_cobertura      USING GIST (geom);


-- ==========================================
-- 16. SEGURIDAD EN LA BASE DE DATOS  [S]
-- ==========================================
-- Modelo: el backend se conecta con un usuario de login (h2togo_app) que no tiene
-- permisos propios. En cada conexión que toma del pool ejecuta
--     SELECT set_config('role', 'h2togo_<actor>', false),
--            set_config('h2togo.id_usuario', '<id>', false);
-- según quién hace la petición. Desde ese momento PostgreSQL aplica los permisos
-- y las políticas de ese actor, sin importar qué SQL envíe el código.
--
--   h2togo_publico     petición sin sesión: login, registro, códigos SMS, catálogo
--   h2togo_cliente     cliente con sesión
--   h2togo_repartidor  repartidor con sesión (dueño o miembro de una purificadora)
--   h2togo_admin       administrador con sesión
--   h2togo_sistema     procesos del backend sin petición: tareas programadas,
--                      consumidores de RabbitMQ y WebSocket
--
-- Los roles son del servidor (no de la BD): se crean solo si no existen.

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

-- Quien ejecuta este script (el dueño del esquema) puede adoptar los cinco roles.
GRANT h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema
    TO CURRENT_USER;

-- Las tablas nuevas no dan permisos a PUBLIC; tampoco se permite crear objetos.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor, h2togo_admin, h2togo_sistema;


-- ------------------------------------------
-- 16.1 Quién es el actor actual
-- ------------------------------------------

CREATE FUNCTION fn_usuario_actual() RETURNS INT
LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('h2togo.id_usuario', true), '')::INT
$$;

-- SECURITY DEFINER: leen repartidores sin pasar por su RLS (evita recursión en las políticas).
CREATE FUNCTION fn_negocio_actual() RETURNS INT
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT id_negocio FROM repartidores WHERE id_usuario = fn_usuario_actual()
$$;

CREATE FUNCTION fn_es_dueno_actual() RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT COALESCE((SELECT es_dueno FROM repartidores WHERE id_usuario = fn_usuario_actual()), FALSE)
$$;

-- Pedido que un repartidor puede atender: suyo, o sin repartidor y buscando uno, si es
-- abierto o es directo a su purificadora.
CREATE FUNCTION fn_pedido_al_alcance(p pedidos) RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
    SELECT p.id_repartidor = fn_usuario_actual()
        OR (p.id_repartidor IS NULL AND p.estado_actual IN ('pendiente', 'pendiente_programado')
            AND (p.tipo_solicitud = 'abierta' OR p.id_negocio_solicitado = fn_negocio_actual()))
$$;


-- ------------------------------------------
-- 16.2 Permisos por tabla y columna  [S1] [S2]
-- ------------------------------------------
-- admin y sistema: todo salvo borrar (las bajas son lógicas) y salvo modificar
-- las bitácoras. Los demás actores: solo lo que usan sus casos de uso.

GRANT SELECT, INSERT, UPDATE ON usuarios, clientes, repartidores, zonas_cobertura,
    direcciones_clientes, negocios, horarios_negocio, vehiculos_negocio, marcas, productos_negocio,
    lotes_inventario, inventario_vehiculo, solicitudes_cambio_perfil, pedidos, detalles_pedido,
    apartados_pedido
    TO h2togo_admin, h2togo_sistema;
GRANT SELECT, INSERT ON historial_estados_pedido, movimientos_inventario TO h2togo_admin, h2togo_sistema;
GRANT DELETE ON apartados_pedido, horarios_negocio, solicitudes_cambio_perfil TO h2togo_admin, h2togo_sistema;

-- Catálogo público (lo ve cualquiera, incluso sin sesión).
GRANT SELECT ON negocios, horarios_negocio, marcas, productos_negocio, zonas_cobertura
    TO h2togo_publico, h2togo_cliente, h2togo_repartidor;

-- Petición sin sesión: login, registro y códigos SMS.
GRANT SELECT, INSERT ON usuarios, clientes, repartidores TO h2togo_publico;
GRANT INSERT ON negocios, horarios_negocio TO h2togo_publico;
GRANT UPDATE (telefono_verificado, codigo_verificacion, codigo_verificacion_expiracion, codigo_intentos,
              password_hash, token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_publico;

-- Cliente.
GRANT SELECT ON usuarios, clientes, repartidores, vehiculos_negocio TO h2togo_cliente;
GRANT UPDATE (nombre, apellidos, url_foto_perfil, password_hash,
              token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_cliente;
GRANT SELECT, INSERT, UPDATE, DELETE ON direcciones_clientes TO h2togo_cliente;
GRANT SELECT, INSERT ON pedidos, detalles_pedido, historial_estados_pedido TO h2togo_cliente;
GRANT UPDATE (estado_actual) ON pedidos TO h2togo_cliente;
-- Sin ningún permiso sobre lotes, inventario de vehículos, apartados, movimientos
-- ni solicitudes.

-- Repartidor.
GRANT SELECT ON usuarios, clientes, repartidores, direcciones_clientes, vehiculos_negocio
    TO h2togo_repartidor;
GRANT UPDATE (nombre, apellidos, url_foto_perfil, password_hash,
              token_sesion_hash, token_fcm, sesion_fecha_creacion, sesion_fecha_expiracion)
    ON usuarios TO h2togo_repartidor;
GRANT UPDATE (ausencias_consecutivas, suspendido_hasta) ON clientes TO h2togo_repartidor;
-- Su propia operación. aprobado y es_dueno solo cambian vía funciones (16.4) o el admin.
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


-- ------------------------------------------
-- 16.3 Seguridad por filas (RLS)  [S3]
-- ------------------------------------------
-- Con RLS activa, un rol sin política aplicable no ve ninguna fila. admin y
-- sistema tienen una política abierta en cada tabla.

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

-- usuarios ------------------------------------------------------------
-- Sin sesión se busca por correo o por token (login, códigos): cualquier fila.
-- Pero nadie se registra como administrador.
CREATE POLICY pol_usuarios_publico_leer ON usuarios FOR SELECT TO h2togo_publico USING (true);
CREATE POLICY pol_usuarios_publico_alta ON usuarios FOR INSERT TO h2togo_publico
    WITH CHECK (rol IN ('cliente', 'repartidor'));
CREATE POLICY pol_usuarios_publico_cambio ON usuarios FOR UPDATE TO h2togo_publico USING (true);
-- El cliente se ve a sí mismo y a los repartidores de sus pedidos.
-- (Las subconsultas también pasan por la RLS de su tabla: "pedidos p" ya son solo
-- los del cliente y "repartidores r", solo los del negocio del repartidor.)
CREATE POLICY pol_usuarios_cliente ON usuarios FOR SELECT TO h2togo_cliente USING (
    usuarios.id_usuario = fn_usuario_actual()
    OR usuarios.id_usuario IN (SELECT p.id_repartidor FROM pedidos p));
-- El repartidor se ve a sí mismo, a su purificadora y a los clientes de pedidos a su alcance.
CREATE POLICY pol_usuarios_repartidor ON usuarios FOR SELECT TO h2togo_repartidor USING (
    usuarios.id_usuario = fn_usuario_actual()
    OR usuarios.id_usuario IN (SELECT r.id_usuario FROM repartidores r)
    OR EXISTS (SELECT 1 FROM pedidos p WHERE p.id_cliente = usuarios.id_usuario AND fn_pedido_al_alcance(p)));
CREATE POLICY pol_usuarios_propio ON usuarios FOR UPDATE TO h2togo_cliente, h2togo_repartidor
    USING (usuarios.id_usuario = fn_usuario_actual());

-- clientes ------------------------------------------------------------
CREATE POLICY pol_clientes_publico ON clientes FOR SELECT TO h2togo_publico USING (true);
CREATE POLICY pol_clientes_publico_alta ON clientes FOR INSERT TO h2togo_publico WITH CHECK (true);
CREATE POLICY pol_clientes_propio ON clientes FOR SELECT TO h2togo_cliente
    USING (clientes.id_usuario = fn_usuario_actual());
-- RN-006: el repartidor registra ausencias del cliente de su pedido.
CREATE POLICY pol_clientes_repartidor ON clientes FOR ALL TO h2togo_repartidor USING (
    EXISTS (SELECT 1 FROM pedidos p WHERE p.id_cliente = clientes.id_usuario AND fn_pedido_al_alcance(p)));

-- repartidores --------------------------------------------------------
CREATE POLICY pol_repartidores_publico ON repartidores FOR SELECT TO h2togo_publico USING (true);
-- Quien se une a un negocio existente nace pendiente; quien lo crea, como dueño aprobado.
CREATE POLICY pol_repartidores_publico_alta ON repartidores FOR INSERT TO h2togo_publico
    WITH CHECK (es_dueno = aprobado);
CREATE POLICY pol_repartidores_cliente ON repartidores FOR SELECT TO h2togo_cliente
    USING (repartidores.id_usuario IN (SELECT p.id_repartidor FROM pedidos p));
CREATE POLICY pol_repartidores_mi_negocio ON repartidores FOR SELECT TO h2togo_repartidor
    USING (repartidores.id_negocio = fn_negocio_actual());
CREATE POLICY pol_repartidores_propio ON repartidores FOR UPDATE TO h2togo_repartidor
    USING (repartidores.id_usuario = fn_usuario_actual());

-- direcciones_clientes ------------------------------------------------
CREATE POLICY pol_direcciones_propias ON direcciones_clientes FOR ALL TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual()) WITH CHECK (id_cliente = fn_usuario_actual());
CREATE POLICY pol_direcciones_repartidor ON direcciones_clientes FOR SELECT TO h2togo_repartidor USING (
    EXISTS (SELECT 1 FROM pedidos p
            WHERE p.id_direccion_entrega = direcciones_clientes.id_direccion AND fn_pedido_al_alcance(p)));

-- horarios y productos (lectura pública, edición del dueño) -----------
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

-- vehículos -----------------------------------------------------------
-- El cliente ve el vehículo en que viene el repartidor de sus pedidos (seguimiento).
CREATE POLICY pol_vehiculos_cliente ON vehiculos_negocio FOR SELECT TO h2togo_cliente USING (
    vehiculos_negocio.id_vehiculo IN (SELECT r.id_vehiculo_actual FROM repartidores r));
CREATE POLICY pol_vehiculos_repartidor ON vehiculos_negocio FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());

-- inventario: solo la purificadora del repartidor ---------------------
CREATE POLICY pol_lotes_leer ON lotes_inventario FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
CREATE POLICY pol_lotes_mover ON lotes_inventario FOR UPDATE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
-- Solo el dueño da de alta lotes (CU-018, RN-021).
CREATE POLICY pol_lotes_alta ON lotes_inventario FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual());

CREATE POLICY pol_iv_negocio ON inventario_vehiculo FOR ALL TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual()) WITH CHECK (id_negocio = fn_negocio_actual());

-- Apartados de pedidos que el repartidor atiende, sobre lotes de su negocio.
CREATE POLICY pol_apartados_repartidor ON apartados_pedido FOR ALL TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM pedidos p
                   WHERE p.id_pedido = apartados_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual())
           AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = apartados_pedido.id_lote))
    WITH CHECK (EXISTS (SELECT 1 FROM pedidos p
                        WHERE p.id_pedido = apartados_pedido.id_pedido AND p.id_repartidor = fn_usuario_actual())
                AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = apartados_pedido.id_lote));

CREATE POLICY pol_mov_leer ON movimientos_inventario FOR SELECT TO h2togo_repartidor
    USING (EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = movimientos_inventario.id_lote));
-- Cada movimiento queda a nombre de quien lo hizo.
CREATE POLICY pol_mov_registrar ON movimientos_inventario FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_repartidor_responsable = fn_usuario_actual()
                AND EXISTS (SELECT 1 FROM lotes_inventario l WHERE l.id_lote = movimientos_inventario.id_lote));

-- solicitudes de cambio -----------------------------------------------
CREATE POLICY pol_solicitudes_leer ON solicitudes_cambio_perfil FOR SELECT TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual());
CREATE POLICY pol_solicitudes_dueno_alta ON solicitudes_cambio_perfil FOR INSERT TO h2togo_repartidor
    WITH CHECK (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual() AND estado = 'pendiente');
CREATE POLICY pol_solicitudes_dueno_retira ON solicitudes_cambio_perfil FOR DELETE TO h2togo_repartidor
    USING (id_negocio = fn_negocio_actual() AND fn_es_dueno_actual() AND estado = 'pendiente');

-- pedidos -------------------------------------------------------------
-- El cliente: sus pedidos. Los crea pendientes y solo puede cancelarlos.
CREATE POLICY pol_pedidos_cliente ON pedidos FOR SELECT TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual());
CREATE POLICY pol_pedidos_cliente_alta ON pedidos FOR INSERT TO h2togo_cliente
    WITH CHECK (id_cliente = fn_usuario_actual() AND id_repartidor IS NULL
                AND estado_actual IN ('pendiente', 'pendiente_programado'));
CREATE POLICY pol_pedidos_cliente_cancela ON pedidos FOR UPDATE TO h2togo_cliente
    USING (id_cliente = fn_usuario_actual() AND estado_actual IN ('pendiente', 'pendiente_programado'))
    WITH CHECK (id_cliente = fn_usuario_actual() AND estado_actual = 'cancelado');
-- El repartidor ve el estado de cualquier pedido (compite por ellos); los datos
-- personales (nombre, teléfono, domicilio) solo los de pedidos a su alcance.
CREATE POLICY pol_pedidos_repartidor_leer ON pedidos FOR SELECT TO h2togo_repartidor USING (true);
CREATE POLICY pol_pedidos_repartidor ON pedidos FOR UPDATE TO h2togo_repartidor
    USING (id_repartidor IS NULL OR id_repartidor = fn_usuario_actual())
    WITH CHECK (id_repartidor IS NULL OR id_repartidor = fn_usuario_actual());

-- detalles e historial ------------------------------------------------
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


-- ------------------------------------------
-- 16.4 Operaciones entre actores (SECURITY DEFINER)  [S4]
-- ------------------------------------------
-- Corren con los permisos del dueño del esquema, pero solo después de validar
-- quién llama (current_setting('role') sigue siendo el rol del actor).

-- RN-023: libera lo apartado para un pedido (cancelación del cliente, no entrega,
-- baja de su repartidor). Suma por (vehículo, lote) para que dos apartados del
-- mismo lote se descuenten completos.
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

-- El dueño aprueba a quien pidió unirse a su purificadora.
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

-- El dueño rechaza la solicitud: baja lógica de la cuenta recién creada.
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


-- ------------------------------------------
-- 16.5 Lo que se publica del inventario  [S4]
-- ------------------------------------------
-- Las vistas se ejecutan con los permisos de su dueño: el cliente ve cuántos
-- garrafones hay por marca, pero no lotes, costos, proveedores ni caducidades.

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


-- ==========================================
-- CONSULTAS ESPACIALES DE REFERENCIA (documentación; no se ejecutan aquí)
-- ==========================================
-- Con geography las distancias están en METROS; todas usan el índice GiST.
--
-- (1) ¿El repartidor está a menos de 500 m de la dirección de entrega? (RF-005)
--     SELECT ST_DWithin(d.ubicacion, r.ubicacion_actual, 500) AS avisar
--     FROM pedidos p
--     JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
--     JOIN repartidores r         ON r.id_usuario   = p.id_repartidor
--     WHERE p.id_pedido = $1;
--
-- (2) Negocios más cercanos a una dirección (orden por distancia con KNN):
--     SELECT n.id_negocio, n.nombre_comercial
--     FROM negocios n
--     WHERE n.activo
--     ORDER BY n.ubicacion_base <-> (SELECT ubicacion FROM direcciones_clientes
--                                    WHERE id_direccion = $1)
--     LIMIT 5;
--
-- Nota: en WKT el orden es POINT(longitud latitud).
