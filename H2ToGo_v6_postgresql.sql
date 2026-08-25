-- ==========================================
-- BASE DE DATOS: H2ToGo (v6) — PostgreSQL 16 + PostGIS 3
-- Proyecto: Trabajo Terminal 2026-B176
-- ==========================================
--
-- Requisitos: PostgreSQL >= 14 con la extensión PostGIS instalada.
-- Ejecución:
--   CREATE DATABASE h2togo_v6;   (una sola vez, conectado a postgres)
--   \c h2togo_v6
--   \i H2ToGo_v6_postgresql.sql
--
-- Cambios respecto a v5 (MariaDB):
--
-- MOTOR [P]
--  [P1] Migración MariaDB → PostgreSQL:
--       - AUTO_INCREMENT               → GENERATED ALWAYS AS IDENTITY
--       - ENUM inline                  → tipos ENUM nativos (CREATE TYPE)
--       - DATETIME / TIMESTAMP         → timestamptz (zona horaria explícita)
--       - TINYINT                      → SMALLINT
--       - ON UPDATE CURRENT_TIMESTAMP  → trigger fn_touch_fecha_actualizacion
--         (PostgreSQL no tiene ON UPDATE en columnas)
--       - COMMENT inline               → COMMENT ON
--  [P2] FKs diferibles (DEFERRABLE INITIALLY DEFERRED) en el ciclo
--       negocios.id_dueno ↔ repartidores.id_negocio: el alta de un dueño y su
--       negocio se hace en UNA transacción y las FKs se verifican al COMMIT.
--       MariaDB no soporta FKs diferibles; en v5 este ciclo no podía
--       expresarse completo (id_dueno apuntaba a usuarios sin exigir rol).
--
-- ACTORES [A]  (observación de la asesora: relaciones que dependían del rol
--               solo se validaban en backend por la columna usuarios.rol)
--  [A1] usuarios deja de ser una tabla "completa": conserva identidad,
--       credenciales y sesión (comunes a los tres actores) y aparecen los
--       subtipos clientes, repartidores y administradores.
--       detalles_repartidor desaparece (absorbida por repartidores).
--       Los campos exclusivos del cliente (ausencias_consecutivas,
--       suspendido_hasta, RN-006) se mueven a clientes.
--  [A2] Las FKs con rol implícito ahora apuntan al subtipo correcto:
--         negocios.id_dueno                                → repartidores
--         pedidos.id_cliente                               → clientes
--         pedidos.id_repartidor                            → repartidores
--         movimientos_inventario.id_repartidor_responsable → repartidores
--         solicitudes_cambio_perfil.id_admin_revisor       → administradores
--       Un cliente con negocio ahora es imposible A NIVEL DE TABLAS,
--       no solo por validación del backend.
--  [A3] Exclusividad de rol garantizada por el esquema:
--       usuarios.rol + UNIQUE(id_usuario, rol) + FK compuesta (id_usuario, rol)
--       desde cada subtipo, cuyo rol está fijado con CHECK. Consecuencias:
--         - un mismo id no puede existir en dos subtipos a la vez;
--         - el subtipo no puede contradecir el rol declarado en usuarios;
--         - cambiar usuarios.rol falla mientras exista la fila del subtipo
--           anterior (el cambio de rol es: borrar subtipo viejo, actualizar
--           rol, insertar subtipo nuevo, en una transacción).
--  [A4] direcciones_usuarios → direcciones_clientes: solo el cliente tiene
--       libreta de direcciones de entrega. La dirección de la base pasa a
--       columnas propias de negocios (en v5 era una dirección "personal" del
--       dueño, lo que mezclaba actores y estorbaba al transferir el negocio).
--  [A5] FK compuesta pedidos(id_direccion_entrega, id_cliente): un pedido
--       solo puede entregarse en una dirección registrada por el MISMO cliente.
--  [A6] FK compuesta repartidores(id_vehiculo_actual, id_negocio): el vehículo
--       en uso debe pertenecer al negocio del propio repartidor.
--
-- GEORREFERENCIACIÓN [G]
--  [G1] Los pares DECIMAL(10,8) / DECIMAL(11,8) se reemplazan por
--       geography(Point, 4326)  (WGS 84, el sistema de coordenadas de OSM y
--       de los GPS de los teléfonos; las distancias se expresan en metros):
--         direcciones_clientes.ubicacion
--         negocios.ubicacion_base
--         historial_estados_pedido.ubicacion
--  [G2] Nueva tabla zonas_cobertura (polígonos). en_zona_cobertura deja de
--       capturarse a mano: la calcula un trigger con ST_Covers al insertar o
--       actualizar la ubicación de una dirección. El polígono de la Alcaldía
--       Benito Juárez puede exportarse de OSM (boundary=administrative).
--  [G3] repartidores.ubicacion_actual: última posición reportada por la app.
--       Soporta la notificación de proximidad a 500 m (RF-005) con ST_DWithin
--       y el filtrado de pedidos cercanos al repartidor (RF-008).
--  [G4] Índices GiST en todas las columnas espaciales.
--
-- NO cambia: el motor de ruteo (A* en Java sobre el grafo OSM en memoria)
-- sigue igual. PostGIS resuelve preguntas sobre PUNTOS (¿está en la zona?,
-- ¿qué tan lejos?, ¿qué hay cerca?); el cálculo de la ruta sigue siendo
-- responsabilidad del algoritmo A* del proyecto.
--
-- Resumen: 16 tablas → 19 tablas
--   (- detalles_repartidor, - direcciones_usuarios,
--    + clientes, + repartidores, + administradores,
--    + direcciones_clientes, + zonas_cobertura)
-- ==========================================

CREATE EXTENSION IF NOT EXISTS postgis;


-- ------------------------------------------
-- 0. TIPOS ENUM  [P1]
-- ------------------------------------------
-- En PostgreSQL los ENUM son tipos con nombre, reutilizables entre tablas.
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
-- 0b. FUNCIONES DE TRIGGER  [P1] [G2]
-- ------------------------------------------

-- Sustituye el ON UPDATE CURRENT_TIMESTAMP de MariaDB.
CREATE FUNCTION fn_touch_fecha_actualizacion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END $$;

-- [G2] en_zona_cobertura se deriva del polígono, no se captura a mano.
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


-- ------------------------------------------
-- 1. USUARIOS (núcleo de identidad y sesión)  [A1]
-- ------------------------------------------
-- Solo lo común a los tres actores: identidad, credenciales, verificación,
-- baja y sesión (un usuario = un dispositivo activo, decisión de diseño).

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
    motivo_baja                      VARCHAR(255),
    fecha_baja                       TIMESTAMPTZ,
    fecha_registro                   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Sesión activa (un solo dispositivo por cuenta)
    token_sesion                     VARCHAR(512) UNIQUE,
    token_fcm                        VARCHAR(255),
    sesion_fecha_creacion            TIMESTAMPTZ,
    sesion_fecha_expiracion          TIMESTAMPTZ,

    -- [A3] Habilita las FKs compuestas (id_usuario, rol) de los subtipos.
    CONSTRAINT uk_usuarios_id_rol UNIQUE (id_usuario, rol)
);

COMMENT ON COLUMN usuarios.rol                 IS 'Discriminador del subtipo. Sin DEFAULT: el alta siempre declara el rol y crea la fila del subtipo en la misma transacción.';
COMMENT ON COLUMN usuarios.telefono_verificado IS 'TRUE cuando el usuario validó el OTP recibido por SMS (RN-002)';
COMMENT ON COLUMN usuarios.cuenta_activa       IS 'FALSE solo cuando el admin da de baja al usuario (CU-015)';
COMMENT ON COLUMN usuarios.codigo_verificacion IS 'OTP numérico enviado por SMS para verificar el teléfono';
COMMENT ON COLUMN usuarios.token_sesion        IS 'NULL cuando no hay sesión activa. Un dispositivo por cuenta (decisión de diseño).';
COMMENT ON COLUMN usuarios.token_fcm           IS 'Token Firebase Cloud Messaging del dispositivo activo. Se actualiza si Firebase lo rota.';


-- ------------------------------------------
-- 2. SUBTIPOS POR ACTOR  [A1] [A3]
-- ------------------------------------------
-- Patrón: herencia por tablas (class table inheritance) con exclusividad.
-- Cada subtipo fija su rol con CHECK y lo empata contra usuarios con una
-- FK compuesta. Así el esquema garantiza subtipo ↔ rol y un solo subtipo
-- por usuario.

CREATE TABLE clientes (
    id_usuario               INT PRIMARY KEY,
    rol                      rol_usuario NOT NULL DEFAULT 'cliente'
                               CONSTRAINT chk_clientes_rol CHECK (rol = 'cliente'),
    ausencias_consecutivas   INT NOT NULL DEFAULT 0,
    suspendido_hasta         TIMESTAMPTZ,

    CONSTRAINT fk_clientes_usuario
        FOREIGN KEY (id_usuario, rol) REFERENCES usuarios (id_usuario, rol)
        ON DELETE CASCADE
);

COMMENT ON COLUMN clientes.ausencias_consecutivas IS 'Contador de ausencias consecutivas del cliente (RN-006). Se reinicia a 0 con una entrega exitosa.';
COMMENT ON COLUMN clientes.suspendido_hasta       IS 'Suspensión temporal automática por ausencias (RN-006). NULL si no está suspendido.';


CREATE TABLE administradores (
    id_usuario   INT PRIMARY KEY,
    rol          rol_usuario NOT NULL DEFAULT 'admin'
                   CONSTRAINT chk_administradores_rol CHECK (rol = 'admin'),

    CONSTRAINT fk_administradores_usuario
        FOREIGN KEY (id_usuario, rol) REFERENCES usuarios (id_usuario, rol)
        ON DELETE CASCADE
);

COMMENT ON TABLE administradores IS 'Sin atributos propios por ahora; existe para que las FKs que exigen rol admin (ej. id_admin_revisor) apunten aquí.';

-- repartidores se crea más abajo: participa en el ciclo con negocios.


-- ------------------------------------------
-- 3. ZONAS DE COBERTURA  [G2]
-- ------------------------------------------

CREATE TABLE zonas_cobertura (
    id_zona   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL UNIQUE,
    geom      geography(MultiPolygon, 4326) NOT NULL,
    activo    BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE zonas_cobertura IS 'Polígonos de la zona de servicio (ej. límite de la Alcaldía Benito Juárez exportado de OSM, boundary=administrative). Una dirección está en cobertura si alguna zona activa la cubre (ST_Covers).';


-- ------------------------------------------
-- 4. DIRECCIONES DE ENTREGA (solo clientes)  [A4] [G1]
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

    -- [A5] Permite que pedidos exija "dirección del mismo cliente".
    CONSTRAINT uk_direcciones_id_cliente UNIQUE (id_direccion, id_cliente),
    CONSTRAINT fk_direcciones_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes (id_usuario)
        ON DELETE CASCADE
);

COMMENT ON COLUMN direcciones_clientes.referencias       IS 'Nota fija a la dirección, ej. "casa azul, frente al parque"';
COMMENT ON COLUMN direcciones_clientes.en_zona_cobertura IS 'Derivada: la asigna el trigger trg_direcciones_zona con ST_Covers contra zonas_cobertura. No la escribe el backend.';

CREATE TRIGGER trg_direcciones_zona
    BEFORE INSERT OR UPDATE OF ubicacion ON direcciones_clientes
    FOR EACH ROW EXECUTE FUNCTION fn_calcula_zona_cobertura();


-- ------------------------------------------
-- 5. NEGOCIOS (purificadoras)  [A2] [A4] [G1]
-- ------------------------------------------

CREATE TABLE negocios (
    id_negocio          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_comercial    VARCHAR(150) NOT NULL,
    id_dueno            INT NOT NULL,
    -- [A4] Dirección de la base como atributo propio del negocio
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
    -- FK de id_dueno → repartidores se agrega tras crear repartidores (ciclo).
);

COMMENT ON COLUMN negocios.id_dueno       IS 'Repartidor administrador del negocio. Único que puede solicitar cambios (RN-021).';
COMMENT ON COLUMN negocios.ubicacion_base IS 'Domicilio físico donde se almacenan los garrafones.';


-- ------------------------------------------
-- 6. REPARTIDORES (subtipo + datos operativos)  [A1] [A3] [G3]
-- ------------------------------------------
-- Absorbe detalles_repartidor de v5.

CREATE TABLE repartidores (
    id_usuario               INT PRIMARY KEY,
    rol                      rol_usuario NOT NULL DEFAULT 'repartidor'
                               CONSTRAINT chk_repartidores_rol CHECK (rol = 'repartidor'),
    id_negocio               INT NOT NULL,
    estado_operativo         BOOLEAN NOT NULL DEFAULT FALSE,
    id_vehiculo_actual       INT,
    ubicacion_actual         geography(Point, 4326),
    ubicacion_reportada_en   TIMESTAMPTZ,

    CONSTRAINT fk_repartidores_usuario
        FOREIGN KEY (id_usuario, rol) REFERENCES usuarios (id_usuario, rol)
        ON DELETE CASCADE,
    -- [P2] Diferible: el alta dueño+negocio ocurre en una sola transacción.
    CONSTRAINT fk_repartidores_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio)
        DEFERRABLE INITIALLY DEFERRED
    -- [A6] FK compuesta a vehiculos_negocio se agrega tras crear esa tabla.
);

COMMENT ON COLUMN repartidores.estado_operativo       IS 'FALSE = desconectado, TRUE = conectado y disponible';
COMMENT ON COLUMN repartidores.id_vehiculo_actual     IS 'Vehículo en uso en la jornada actual. NULL si está desconectado.';
COMMENT ON COLUMN repartidores.ubicacion_actual       IS '[G3] Última posición reportada por la app del repartidor. Alimenta el aviso de proximidad a 500 m (RF-005, ST_DWithin) y el filtrado de pedidos cercanos (RF-008).';
COMMENT ON COLUMN repartidores.ubicacion_reportada_en IS 'Cuándo se reportó ubicacion_actual. Permite descartar posiciones viejas.';

-- Cierre del ciclo negocios ↔ repartidores  [A2] [P2]
ALTER TABLE negocios
    ADD CONSTRAINT fk_negocios_dueno
        FOREIGN KEY (id_dueno) REFERENCES repartidores (id_usuario)
        DEFERRABLE INITIALLY DEFERRED;


-- ------------------------------------------
-- 7. HORARIOS DEL NEGOCIO
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
        CHECK ((cerrado = TRUE  AND hora_apertura IS NULL     AND hora_cierre IS NULL)
            OR (cerrado = FALSE AND hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL))
);

COMMENT ON COLUMN horarios_negocio.dia_semana IS '1=lunes ... 7=domingo';
COMMENT ON COLUMN horarios_negocio.cerrado    IS 'TRUE si el negocio no opera ese día';


-- ------------------------------------------
-- 8. VEHÍCULOS DEL NEGOCIO  [A6]
-- ------------------------------------------

CREATE TABLE vehiculos_negocio (
    id_vehiculo            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_negocio             INT NOT NULL,
    tipo_vehiculo          tipo_vehiculo NOT NULL,
    marca                  VARCHAR(50)  NOT NULL,
    modelo                 VARCHAR(50),
    color                  VARCHAR(30)  NOT NULL,
    placas                 VARCHAR(20),
    capacidad_garrafones   INT          NOT NULL,
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,

    -- [A6] Habilita la FK compuesta desde repartidores.
    CONSTRAINT uk_vehiculo_negocio UNIQUE (id_vehiculo, id_negocio),
    CONSTRAINT fk_vehiculos_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio) ON DELETE CASCADE
);

-- [A6] El vehículo en uso debe pertenecer al negocio del propio repartidor.
--      (Si id_vehiculo_actual es NULL la FK no aplica.)
ALTER TABLE repartidores
    ADD CONSTRAINT fk_repartidores_vehiculo_actual
        FOREIGN KEY (id_vehiculo_actual, id_negocio)
        REFERENCES vehiculos_negocio (id_vehiculo, id_negocio);


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
    CONSTRAINT fk_pn_marca   FOREIGN KEY (id_marca)   REFERENCES marcas (id_marca)
);

COMMENT ON COLUMN productos_negocio.precio           IS 'Precio del agua cuando el cliente trae envase propio (RN-025).';
COMMENT ON COLUMN productos_negocio.precio_envase    IS 'Precio adicional del envase cuando el cliente NO trae envase propio (RN-031).';
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

    CONSTRAINT fk_lote_negocio FOREIGN KEY (id_negocio) REFERENCES negocios (id_negocio),
    CONSTRAINT fk_lote_marca   FOREIGN KEY (id_marca)   REFERENCES marcas (id_marca)
);

COMMENT ON COLUMN lotes_inventario.fecha_caducidad IS 'Fecha de caducidad del lote (RN-030)';
COMMENT ON COLUMN lotes_inventario.cantidad_actual IS 'Garrafones de este lote actualmente en la base';
COMMENT ON COLUMN lotes_inventario.activo          IS 'FALSE cuando el lote se agotó o se descartó por caducidad';

CREATE TRIGGER trg_lotes_touch
    BEFORE UPDATE ON lotes_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_touch_fecha_actualizacion();

CREATE INDEX idx_lotes_caducidad ON lotes_inventario (id_negocio, id_marca, fecha_caducidad);
COMMENT ON INDEX idx_lotes_caducidad IS 'Soporta consultas FIFO: lote más próximo a vencer primero';


CREATE TABLE inventario_vehiculo (
    id_inventario_vehiculo   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_vehiculo              INT NOT NULL,
    id_lote                  INT NOT NULL,
    cantidad_actual          INT NOT NULL DEFAULT 0,
    cantidad_apartada        INT NOT NULL DEFAULT 0,
    activo                   BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_inventario_vehiculo_lote UNIQUE (id_vehiculo, id_lote),
    CONSTRAINT fk_iv_vehiculo FOREIGN KEY (id_vehiculo) REFERENCES vehiculos_negocio (id_vehiculo),
    CONSTRAINT fk_iv_lote     FOREIGN KEY (id_lote)     REFERENCES lotes_inventario (id_lote)
);

COMMENT ON COLUMN inventario_vehiculo.id_lote IS 'Lote al que pertenecen estos garrafones. Marca y negocio se obtienen vía lotes_inventario.';

CREATE TRIGGER trg_inventario_vehiculo_touch
    BEFORE UPDATE ON inventario_vehiculo
    FOR EACH ROW EXECUTE FUNCTION fn_touch_fecha_actualizacion();


-- ------------------------------------------
-- 11. SOLICITUDES DE CAMBIO DE PERFIL (RN-019)  [A2]
-- ------------------------------------------

CREATE TABLE solicitudes_cambio_perfil (
    id_solicitud             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_cambio            codigo_cambio_perfil NOT NULL,
    id_negocio               INT NOT NULL,
    id_admin_revisor         INT,
    id_vehiculo              INT,
    id_producto_negocio      INT,
    valor_anterior           TEXT,
    valor_nuevo              TEXT NOT NULL,
    estado                   estado_solicitud NOT NULL DEFAULT 'pendiente',
    comentario_admin         VARCHAR(500),
    fecha_solicitud          TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_resolucion         TIMESTAMPTZ,

    CONSTRAINT fk_solicitudes_negocio
        FOREIGN KEY (id_negocio)          REFERENCES negocios (id_negocio),
    -- [A2] Solo un administrador puede revisar solicitudes.
    CONSTRAINT fk_solicitudes_admin
        FOREIGN KEY (id_admin_revisor)    REFERENCES administradores (id_usuario),
    CONSTRAINT fk_solicitudes_vehiculo
        FOREIGN KEY (id_vehiculo)         REFERENCES vehiculos_negocio (id_vehiculo),
    CONSTRAINT fk_solicitudes_producto_neg
        FOREIGN KEY (id_producto_negocio) REFERENCES productos_negocio (id_producto_negocio)
);

COMMENT ON COLUMN solicitudes_cambio_perfil.codigo_cambio  IS 'Tipo de cambio solicitado. El solicitante siempre es negocios.id_dueno (RN-021).';
COMMENT ON COLUMN solicitudes_cambio_perfil.valor_anterior IS 'Valor previo (puede ser JSON si son varios campos). Se conserva TEXT para no cambiar el contrato del backend; jsonb es una opción futura.';


-- ------------------------------------------
-- 12. PEDIDOS Y DETALLES  [A2] [A5]
-- ------------------------------------------

CREATE TABLE pedidos (
    id_pedido                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente                 INT NOT NULL,
    id_negocio_solicitado      INT,
    id_repartidor              INT,
    id_direccion_entrega       INT NOT NULL,
    id_vehiculo_utilizado      INT,
    tipo_solicitud             tipo_solicitud_pedido NOT NULL DEFAULT 'directa',
    precio_maximo_garrafon     NUMERIC(10,2),
    estado_actual              estado_pedido NOT NULL DEFAULT 'pendiente',
    total_pagar                NUMERIC(10,2) NOT NULL,
    garrafones_totales         INT NOT NULL,
    indicaciones_entrega       VARCHAR(500),
    es_programado              BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_programada           TIMESTAMPTZ,
    notificado_programado      BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion             TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_entrega              TIMESTAMPTZ,

    -- [A2] Solo un cliente pide; solo un repartidor entrega.
    CONSTRAINT fk_pedidos_cliente
        FOREIGN KEY (id_cliente)              REFERENCES clientes (id_usuario),
    CONSTRAINT fk_pedidos_negocio_solicitado
        FOREIGN KEY (id_negocio_solicitado)   REFERENCES negocios (id_negocio),
    CONSTRAINT fk_pedidos_repartidor
        FOREIGN KEY (id_repartidor)           REFERENCES repartidores (id_usuario),
    -- [A5] La dirección de entrega debe ser del MISMO cliente que pide.
    CONSTRAINT fk_pedidos_direccion_del_cliente
        FOREIGN KEY (id_direccion_entrega, id_cliente)
        REFERENCES direcciones_clientes (id_direccion, id_cliente),
    CONSTRAINT fk_pedidos_vehiculo
        FOREIGN KEY (id_vehiculo_utilizado)   REFERENCES vehiculos_negocio (id_vehiculo)
);

COMMENT ON COLUMN pedidos.id_negocio_solicitado  IS 'Si el cliente eligió un negocio específico (modalidad directa)';
COMMENT ON COLUMN pedidos.id_repartidor          IS 'Repartidor que aceptó el pedido';
COMMENT ON COLUMN pedidos.id_vehiculo_utilizado  IS 'Snapshot del vehículo con que se realizó la entrega (necesario para histórico)';
COMMENT ON COLUMN pedidos.tipo_solicitud         IS 'directa = eligió negocio; abierta = con precio máximo';
COMMENT ON COLUMN pedidos.precio_maximo_garrafon IS 'Solo aplica si tipo_solicitud = abierta';
COMMENT ON COLUMN pedidos.total_pagar            IS 'Total ESTIMADO al crear el pedido. El definitivo se recalcula al cerrar usando cantidad_entregada (RN-033).';
COMMENT ON COLUMN pedidos.notificado_programado  IS 'TRUE cuando el proceso periódico del backend ya activó este pedido y notificó a los repartidores. Solo aplica si es_programado=TRUE.';


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

    CONSTRAINT chk_detalles_cantidad_entregada
        CHECK (cantidad_entregada IS NULL
            OR cantidad_entregada <= cantidad_solicitada
            OR agregada_en_sitio = TRUE),

    CONSTRAINT chk_detalles_agregada_en_sitio
        CHECK (agregada_en_sitio = FALSE
            OR (agregada_en_sitio = TRUE AND cantidad_solicitada = 0)),

    CONSTRAINT chk_detalles_precio_envase
        CHECK (tiene_envase = TRUE
            OR (tiene_envase = FALSE AND precio_envase_unitario IS NOT NULL))
);

COMMENT ON COLUMN detalles_pedido.id_marca               IS 'Marca solicitada. En modalidad abierta es la única referencia disponible al crear el pedido.';
COMMENT ON COLUMN detalles_pedido.cantidad_solicitada    IS 'Garrafones pedidos originalmente. Inmutable después de creado el pedido.';
COMMENT ON COLUMN detalles_pedido.cantidad_entregada     IS 'Garrafones realmente entregados. NULL hasta estado entregado o no_entregado. Permite entregas parciales (RN-032).';
COMMENT ON COLUMN detalles_pedido.precio_unitario        IS 'Snapshot del precio del agua al momento del pedido (RN-025).';
COMMENT ON COLUMN detalles_pedido.agregada_en_sitio      IS 'TRUE si esta línea fue creada por el repartidor durante la entrega (RN-032).';


-- ------------------------------------------
-- 13. APARTADOS DE INVENTARIO POR PEDIDO
-- ------------------------------------------

CREATE TABLE apartados_pedido (
    id_apartado              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido                INT NOT NULL,
    id_lote_base             INT,
    id_inventario_vehiculo   INT,
    cantidad                 INT NOT NULL,
    fecha_apartado           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_apartados_pedido
        FOREIGN KEY (id_pedido)              REFERENCES pedidos (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_apartados_lote_base
        FOREIGN KEY (id_lote_base)           REFERENCES lotes_inventario (id_lote),
    CONSTRAINT fk_apartados_vehiculo
        FOREIGN KEY (id_inventario_vehiculo) REFERENCES inventario_vehiculo (id_inventario_vehiculo),
    CONSTRAINT chk_apartados_origen
        CHECK ((id_lote_base IS NOT NULL AND id_inventario_vehiculo IS NULL)
            OR (id_lote_base IS NULL     AND id_inventario_vehiculo IS NOT NULL))
);

COMMENT ON COLUMN apartados_pedido.id_lote_base           IS 'Solo si el apartado vino de la base (referencia a lotes_inventario)';
COMMENT ON COLUMN apartados_pedido.id_inventario_vehiculo IS 'Solo si el apartado vino del vehículo';


-- ------------------------------------------
-- 14. BITÁCORAS: MOVIMIENTOS DE INVENTARIO E HISTORIAL DE PEDIDOS  [A2] [G1]
-- ------------------------------------------

CREATE TABLE movimientos_inventario (
    id_movimiento              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_lote_base               INT,
    id_inventario_vehiculo     INT,
    tipo                       tipo_movimiento NOT NULL,
    cantidad                   INT NOT NULL,
    id_pedido                  INT,
    id_repartidor_responsable  INT,
    costo_unitario             NUMERIC(10,2),
    proveedor                  VARCHAR(150),
    notas                      VARCHAR(255),
    fecha                      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_mov_lote_base
        FOREIGN KEY (id_lote_base)              REFERENCES lotes_inventario (id_lote),
    CONSTRAINT fk_mov_vehiculo
        FOREIGN KEY (id_inventario_vehiculo)    REFERENCES inventario_vehiculo (id_inventario_vehiculo),
    CONSTRAINT fk_mov_pedido
        FOREIGN KEY (id_pedido)                 REFERENCES pedidos (id_pedido),
    -- [A2] Solo un repartidor registra movimientos.
    CONSTRAINT fk_mov_repartidor
        FOREIGN KEY (id_repartidor_responsable) REFERENCES repartidores (id_usuario),

    -- Cada tipo de movimiento determina qué columnas de ubicación deben estar presentes
    CONSTRAINT chk_mov_origen CHECK (
        (tipo = 'entrada_proveedor'
            AND id_lote_base IS NOT NULL AND id_inventario_vehiculo IS NULL)
        OR (tipo = 'salida_pedido'
            AND id_lote_base IS NULL AND id_inventario_vehiculo IS NOT NULL)
        OR (tipo IN ('traspaso_a_vehiculo', 'devolucion_a_base')
            AND id_lote_base IS NOT NULL AND id_inventario_vehiculo IS NOT NULL)
        OR (tipo IN ('salida_manual', 'ajuste')
            AND ((id_lote_base IS NOT NULL AND id_inventario_vehiculo IS NULL)
              OR (id_lote_base IS NULL     AND id_inventario_vehiculo IS NOT NULL)))
    )
);

COMMENT ON COLUMN movimientos_inventario.cantidad IS 'Siempre positiva; el tipo determina la dirección del movimiento';


CREATE TABLE historial_estados_pedido (
    id_historial          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido             INT NOT NULL,
    estado                estado_pedido NOT NULL,
    notas_adicionales     VARCHAR(500),
    ubicacion             geography(Point, 4326),
    fecha_cambio          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_historial_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido) ON DELETE CASCADE
);

COMMENT ON COLUMN historial_estados_pedido.notas_adicionales IS 'Ej. motivo de no entrega: cliente_ausente, direccion_incorrecta, sin_pago, etc.';
COMMENT ON COLUMN historial_estados_pedido.ubicacion         IS '[G1] Ubicación del repartidor al momento del cambio de estado';


-- ==========================================
-- ÍNDICES
-- ==========================================

-- Pedidos
CREATE INDEX idx_pedidos_estado            ON pedidos (estado_actual);
CREATE INDEX idx_pedidos_repartidor_estado ON pedidos (id_repartidor, estado_actual);
CREATE INDEX idx_pedidos_cliente_fecha     ON pedidos (id_cliente, fecha_creacion DESC);
CREATE INDEX idx_pedidos_negocio_estado    ON pedidos (id_negocio_solicitado, estado_actual);
CREATE INDEX idx_pedidos_programados       ON pedidos (es_programado, notificado_programado, fecha_programada);

-- Historial
CREATE INDEX idx_historial_pedido_fecha    ON historial_estados_pedido (id_pedido, fecha_cambio);

-- Movimientos de inventario
CREATE INDEX idx_mov_lote_base_fecha       ON movimientos_inventario (id_lote_base, fecha DESC);
CREATE INDEX idx_mov_vehiculo_fecha        ON movimientos_inventario (id_inventario_vehiculo, fecha DESC);

-- Apartados
CREATE INDEX idx_apartados_pedido          ON apartados_pedido (id_pedido);

-- Direcciones por cliente
CREATE INDEX idx_direcciones_cliente       ON direcciones_clientes (id_cliente);

-- Repartidores y vehículos por negocio
CREATE INDEX idx_repartidores_negocio      ON repartidores (id_negocio);
CREATE INDEX idx_vehiculos_negocio         ON vehiculos_negocio (id_negocio);

-- Solicitudes de cambio (panel admin)
CREATE INDEX idx_solicitudes_estado        ON solicitudes_cambio_perfil (estado, fecha_solicitud DESC);
CREATE INDEX idx_solicitudes_negocio       ON solicitudes_cambio_perfil (id_negocio);

-- Horarios por negocio
CREATE INDEX idx_horarios_negocio          ON horarios_negocio (id_negocio);

-- Suspensiones temporales (consulta al iniciar sesión / hacer pedido)
CREATE INDEX idx_clientes_suspendido       ON clientes (suspendido_hasta);

-- Sesiones activas: limpieza de sesiones expiradas
CREATE INDEX idx_usuarios_sesion_expiracion ON usuarios (sesion_fecha_expiracion);

-- [G4] Índices espaciales (GiST)
CREATE INDEX idx_direcciones_ubicacion     ON direcciones_clientes USING GIST (ubicacion);
CREATE INDEX idx_negocios_ubicacion_base   ON negocios             USING GIST (ubicacion_base);
CREATE INDEX idx_repartidores_ubicacion    ON repartidores         USING GIST (ubicacion_actual);
CREATE INDEX idx_zonas_geom                ON zonas_cobertura      USING GIST (geom);


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
-- (2) Pedidos pendientes con entrega a menos de 3 km del repartidor (RF-008):
--     SELECT p.id_pedido,
--            ST_Distance(d.ubicacion, r.ubicacion_actual) AS distancia_m
--     FROM pedidos p
--     JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega,
--          repartidores r
--     WHERE r.id_usuario = $1
--       AND p.estado_actual = 'pendiente'
--       AND d.en_zona_cobertura
--       AND ST_DWithin(d.ubicacion, r.ubicacion_actual, 3000)
--     ORDER BY distancia_m;
--
-- (3) Negocios más cercanos a una dirección (orden por distancia con KNN):
--     SELECT n.id_negocio, n.nombre_comercial
--     FROM negocios n
--     WHERE n.activo
--     ORDER BY n.ubicacion_base <-> (SELECT ubicacion FROM direcciones_clientes
--                                    WHERE id_direccion = $1)
--     LIMIT 5;
--
-- (4) Verificar manualmente si un punto cae en la zona de cobertura
--     (el trigger ya lo hace al guardar direcciones):
--     SELECT EXISTS (
--         SELECT 1 FROM zonas_cobertura z
--         WHERE z.activo
--           AND ST_Covers(z.geom, ST_GeogFromText('SRID=4326;POINT(-99.1671 19.3727)'))
--     );
--
-- Nota: en WKT el orden es POINT(longitud latitud).
