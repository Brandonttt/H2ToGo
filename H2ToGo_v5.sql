-- ==========================================
-- BASE DE DATOS: HToGo (v5)
-- Proyecto: Trabajo Terminal 2026-B176
-- ==========================================
--
-- Cambios respecto a v4:
--  + Tabla horarios_negocio: una fila por día de la semana, permite
--    horarios distintos por día y marcar días cerrados.
--  + Columna notificado_programado en pedidos: flag que el cron job marca
--    como TRUE cuando ya activó/notificó el pedido programado a los
--    repartidores. Evita procesarlo dos veces.
-- ==========================================


-- ------------------------------------------
-- 1. USUARIOS, SESIONES Y DISPOSITIVOS
-- ------------------------------------------

CREATE TABLE usuarios (
    id_usuario                       INT AUTO_INCREMENT PRIMARY KEY,
    nombre                           VARCHAR(100) NOT NULL,
    apellidos                        VARCHAR(100) NOT NULL,
    correo                           VARCHAR(150) NOT NULL UNIQUE,
    password_hash                    VARCHAR(255) NOT NULL,
    telefono                         VARCHAR(20)  NOT NULL UNIQUE,
    rol                              VARCHAR(50)  NOT NULL DEFAULT 'cliente'
                                       COMMENT 'ENUM: cliente, repartidor, admin',
    url_foto_perfil                  VARCHAR(500),
    correo_verificado                BOOLEAN      NOT NULL DEFAULT FALSE,
    cuenta_activa                    BOOLEAN      NOT NULL DEFAULT TRUE,
    ausencias_consecutivas           INT          NOT NULL DEFAULT 0,
    token_verificacion               VARCHAR(255),
    token_verificacion_expiracion    DATETIME,
    motivo_baja                      VARCHAR(255),
    fecha_baja                       DATETIME,
    id_admin_baja                    INT,
    fecha_registro                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuarios_admin_baja
        FOREIGN KEY (id_admin_baja) REFERENCES usuarios(id_usuario)
);


CREATE TABLE sesiones_activas (
    id_sesion          INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario         INT NOT NULL,
    token_sesion       VARCHAR(512) NOT NULL UNIQUE,
    plataforma         VARCHAR(50)  NOT NULL DEFAULT 'android'
                          COMMENT 'ENUM: android, ios, web',
    fecha_creacion     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion   DATETIME     NOT NULL,

    CONSTRAINT fk_sesiones_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);


CREATE TABLE dispositivos_usuarios (
    id_dispositivo     INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario         INT NOT NULL,
    token_fcm          VARCHAR(255) NOT NULL UNIQUE,
    ultima_conexion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dispositivos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);


-- ------------------------------------------
-- 2. DIRECCIONES (clientes y bases de negocios)
-- ------------------------------------------

CREATE TABLE direcciones_usuarios (
    id_direccion        INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario          INT NOT NULL,
    alias               VARCHAR(50)  NOT NULL,
    calle               VARCHAR(150) NOT NULL,
    numero_exterior     VARCHAR(20)  NOT NULL,
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100) NOT NULL,
    codigo_postal       VARCHAR(10)  NOT NULL,
    referencias         TEXT
                          COMMENT 'Nota fija a la dirección, ej. "casa azul, frente al parque"',
    latitud             DECIMAL(10,8) NOT NULL,
    longitud            DECIMAL(11,8) NOT NULL,
    en_zona_cobertura   BOOLEAN NOT NULL DEFAULT TRUE,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_direcciones_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);


-- ------------------------------------------
-- 3. NEGOCIOS (purificadoras) Y SUS REPARTIDORES
-- ------------------------------------------

CREATE TABLE negocios (
    id_negocio          INT AUTO_INCREMENT PRIMARY KEY,
    nombre_comercial    VARCHAR(150) NOT NULL,
    id_direccion_base   INT
                          COMMENT 'Domicilio físico donde se almacenan los garrafones',
    id_dueño            INT NOT NULL
                          COMMENT 'Repartidor administrador del negocio. Único que puede solicitar cambios.',
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_negocios_direccion
        FOREIGN KEY (id_direccion_base) REFERENCES direcciones_usuarios(id_direccion),
    CONSTRAINT fk_negocios_dueño
        FOREIGN KEY (id_dueño) REFERENCES usuarios(id_usuario)
);


CREATE TABLE horarios_negocio (
    id_horario          INT AUTO_INCREMENT PRIMARY KEY,
    id_negocio          INT NOT NULL,
    dia_semana          TINYINT NOT NULL
                          COMMENT '1=lunes, 2=martes, 3=miércoles, 4=jueves, 5=viernes, 6=sábado, 7=domingo',
    hora_apertura       TIME
                          COMMENT 'NULL si el negocio está cerrado ese día',
    hora_cierre         TIME
                          COMMENT 'NULL si el negocio está cerrado ese día',
    cerrado             BOOLEAN NOT NULL DEFAULT FALSE
                          COMMENT 'TRUE si el negocio no opera ese día',

    CONSTRAINT uk_horario_negocio_dia UNIQUE (id_negocio, dia_semana),
    CONSTRAINT fk_horarios_negocio FOREIGN KEY (id_negocio) REFERENCES negocios(id_negocio) ON DELETE CASCADE,
    CONSTRAINT chk_horarios_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horarios_consistencia
        CHECK ((cerrado = TRUE AND hora_apertura IS NULL AND hora_cierre IS NULL)
            OR (cerrado = FALSE AND hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL))
);


CREATE TABLE detalles_repartidor (
    id_usuario              INT PRIMARY KEY,
    id_negocio              INT NOT NULL
                              COMMENT 'Negocio al que pertenece este repartidor',
    licencia_conducir       VARCHAR(50) NOT NULL UNIQUE,
    estado_operativo        VARCHAR(50) NOT NULL DEFAULT 'desconectado'
                              COMMENT 'ENUM: desconectado, disponible, ocupado, suspendido',
    id_vehiculo_actual      INT
                              COMMENT 'Vehículo que está usando el repartidor en su jornada actual; NULL si está desconectado',

    CONSTRAINT fk_detalles_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios(id_negocio)
    -- FK a vehiculos_negocio se agrega al final por dependencia circular
);


-- ------------------------------------------
-- 4. VEHÍCULOS DEL NEGOCIO
-- ------------------------------------------

CREATE TABLE vehiculos_negocio (
    id_vehiculo            INT AUTO_INCREMENT PRIMARY KEY,
    id_negocio             INT NOT NULL,
    tipo_vehiculo          VARCHAR(50)  NOT NULL
                              COMMENT 'ENUM: motocicleta, automovil, camioneta, triciclo_carga, bicicleta_carga',
    marca                  VARCHAR(50)  NOT NULL,
    modelo                 VARCHAR(50),
    color                  VARCHAR(30)  NOT NULL,
    placas                 VARCHAR(20),
    capacidad_garrafones   INT          NOT NULL,
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_vehiculos_negocio
        FOREIGN KEY (id_negocio) REFERENCES negocios(id_negocio) ON DELETE CASCADE
);

-- Cierre de la dependencia circular: detalles_repartidor.id_vehiculo_actual → vehiculos_negocio
ALTER TABLE detalles_repartidor
    ADD CONSTRAINT fk_detalles_vehiculo_actual
        FOREIGN KEY (id_vehiculo_actual) REFERENCES vehiculos_negocio(id_vehiculo);


-- ------------------------------------------
-- 5. CATÁLOGO DE PRODUCTOS Y PRECIOS POR NEGOCIO
-- ------------------------------------------

CREATE TABLE marcas (
    id_marca   INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL UNIQUE,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE
);


CREATE TABLE productos (
    id_producto         INT AUTO_INCREMENT PRIMARY KEY,
    id_marca            INT NOT NULL,
    nombre              VARCHAR(100) NOT NULL,
    capacidad_litros    DECIMAL(5,2) NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_productos_marca
        FOREIGN KEY (id_marca) REFERENCES marcas(id_marca)
);


CREATE TABLE productos_negocio (
    id_producto_negocio   INT AUTO_INCREMENT PRIMARY KEY,
    id_negocio            INT NOT NULL,
    id_producto           INT NOT NULL,
    precio                DECIMAL(10,2) NOT NULL
                             COMMENT 'Precio que cobra ESTE negocio por ESTE producto. Compartido entre todos sus repartidores.',
    activo                BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_producto_negocio UNIQUE (id_negocio, id_producto),
    CONSTRAINT fk_pn_negocio  FOREIGN KEY (id_negocio)  REFERENCES negocios(id_negocio),
    CONSTRAINT fk_pn_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);


-- ------------------------------------------
-- 6. INVENTARIO EN DOS NIVELES: BASE Y VEHÍCULO
-- ------------------------------------------

CREATE TABLE inventario_base (
    id_inventario_base    INT AUTO_INCREMENT PRIMARY KEY,
    id_negocio            INT NOT NULL,
    id_producto           INT NOT NULL,
    cantidad_actual       INT NOT NULL DEFAULT 0
                             COMMENT 'Garrafones físicos en la base del negocio',
    cantidad_apartada     INT NOT NULL DEFAULT 0
                             COMMENT 'Reservados para pedidos aceptados pero aún no cargados al vehículo',
    capacidad_maxima      INT NOT NULL,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_inventario_base UNIQUE (id_negocio, id_producto),
    CONSTRAINT fk_ib_negocio  FOREIGN KEY (id_negocio)  REFERENCES negocios(id_negocio),
    CONSTRAINT fk_ib_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);


CREATE TABLE inventario_vehiculo (
    id_inventario_vehiculo   INT AUTO_INCREMENT PRIMARY KEY,
    id_vehiculo              INT NOT NULL,
    id_producto              INT NOT NULL,
    cantidad_actual          INT NOT NULL DEFAULT 0
                                COMMENT 'Garrafones físicos en el vehículo',
    cantidad_apartada        INT NOT NULL DEFAULT 0
                                COMMENT 'Reservados para pedidos aceptados aún no entregados',
    activo                   BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_inventario_vehiculo UNIQUE (id_vehiculo, id_producto),
    CONSTRAINT fk_iv_vehiculo FOREIGN KEY (id_vehiculo) REFERENCES vehiculos_negocio(id_vehiculo),
    CONSTRAINT fk_iv_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);


-- ------------------------------------------
-- 7. CATÁLOGO Y SOLICITUDES DE CAMBIO DE PERFIL (RN-020)
-- ------------------------------------------
-- Solo el dueño del negocio (negocios.id_dueño) puede levantar solicitudes.

CREATE TABLE tipos_cambio_perfil (
    id_tipo_cambio          INT AUTO_INCREMENT PRIMARY KEY,
    codigo                  VARCHAR(50)  NOT NULL UNIQUE
                              COMMENT 'Identificador interno usado por el código',
    nombre                  VARCHAR(100) NOT NULL
                              COMMENT 'Texto visible en la app',
    descripcion             VARCHAR(255),
    entidad_afectada        VARCHAR(30)  NOT NULL
                              COMMENT 'ENUM: usuario, vehiculo, negocio, producto_negocio',
    requiere_aprobacion     BOOLEAN NOT NULL DEFAULT TRUE,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tipos iniciales del catálogo
INSERT INTO tipos_cambio_perfil (codigo, nombre, entidad_afectada) VALUES
    ('FOTO_PERFIL',       'Cambio de foto de perfil',                     'usuario'),
    ('DATOS_VEHICULO',    'Cambio de datos de vehículo',                  'vehiculo'),
    ('AGREGAR_VEHICULO',  'Agregar nuevo vehículo al negocio',            'negocio'),
    ('ELIMINAR_VEHICULO', 'Eliminar vehículo del negocio',                'vehiculo'),
    ('NOMBRE_NEGOCIO',    'Cambio de nombre comercial del negocio',       'negocio'),
    ('DIRECCION_BASE',    'Cambio de dirección de la base',               'negocio'),
    ('PRECIO_PRODUCTO',   'Cambio de precio de un producto',              'producto_negocio'),
    ('AGREGAR_PRODUCTO',  'Agregar producto al catálogo del negocio',     'negocio');


CREATE TABLE solicitudes_cambio_perfil (
    id_solicitud             INT AUTO_INCREMENT PRIMARY KEY,
    id_tipo_cambio           INT NOT NULL,
    id_repartidor            INT NOT NULL
                                COMMENT 'Debe ser el dueño del negocio (validar en backend)',
    id_negocio               INT NOT NULL,
    id_admin_revisor         INT,
    -- Referencias opcionales según tipo de cambio
    id_vehiculo              INT
                                COMMENT 'Si la solicitud afecta un vehículo específico',
    id_producto_negocio      INT
                                COMMENT 'Si la solicitud afecta un producto específico del negocio',
    -- Payload de la solicitud
    valor_anterior           TEXT
                                COMMENT 'Valor previo (puede ser JSON si son varios campos)',
    valor_nuevo              TEXT NOT NULL
                                COMMENT 'Valor propuesto (puede ser JSON si son varios campos)',
    estado                   VARCHAR(50)  NOT NULL DEFAULT 'pendiente'
                                COMMENT 'ENUM: pendiente, aprobado, rechazado',
    comentario_admin         VARCHAR(500),
    fecha_solicitud          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion         TIMESTAMP NULL,

    CONSTRAINT fk_solicitudes_tipo
        FOREIGN KEY (id_tipo_cambio)      REFERENCES tipos_cambio_perfil(id_tipo_cambio),
    CONSTRAINT fk_solicitudes_repartidor
        FOREIGN KEY (id_repartidor)       REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_solicitudes_negocio
        FOREIGN KEY (id_negocio)          REFERENCES negocios(id_negocio),
    CONSTRAINT fk_solicitudes_admin
        FOREIGN KEY (id_admin_revisor)    REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_solicitudes_vehiculo
        FOREIGN KEY (id_vehiculo)         REFERENCES vehiculos_negocio(id_vehiculo),
    CONSTRAINT fk_solicitudes_producto_neg
        FOREIGN KEY (id_producto_negocio) REFERENCES productos_negocio(id_producto_negocio)
);


-- ------------------------------------------
-- 8. PEDIDOS Y DETALLES
-- ------------------------------------------

CREATE TABLE pedidos (
    id_pedido                  INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente                 INT NOT NULL,
    id_negocio_solicitado      INT
                                  COMMENT 'Si el cliente eligió un negocio específico (modo directo)',
    id_repartidor              INT
                                  COMMENT 'Repartidor del negocio que finalmente aceptó el pedido',
    id_direccion_entrega       INT NOT NULL,
    id_vehiculo_utilizado      INT
                                  COMMENT 'Vehículo con que se realizó la entrega',
    tipo_solicitud             VARCHAR(20)  NOT NULL DEFAULT 'directa'
                                  COMMENT 'ENUM: directa (eligió negocio), abierta (con precio máximo)',
    precio_maximo_garrafon     DECIMAL(10,2)
                                  COMMENT 'Solo aplica si tipo_solicitud = abierta',
    estado_actual              VARCHAR(50)  NOT NULL DEFAULT 'pendiente'
                                  COMMENT 'ENUM: pendiente, aceptado, en_camino, entregado, cancelado, no_entregado',
    total_pagar                DECIMAL(10,2) NOT NULL,
    garrafones_totales         INT NOT NULL,
    distancia_km               DECIMAL(6,2),
    tiempo_estimado_minutos    INT,
    indicaciones_entrega       VARCHAR(500)
                                  COMMENT 'Notas del cliente para este pedido específico',
    es_programado              BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_programada           DATETIME
                                  COMMENT 'Fecha y hora del pedido programado',
    notificado_programado      BOOLEAN NOT NULL DEFAULT FALSE
                                  COMMENT 'TRUE cuando el cron job ya activó este pedido programado y notificó a los repartidores. Solo aplica si es_programado=TRUE',
    fecha_creacion             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega              DATETIME,

    CONSTRAINT fk_pedidos_cliente
        FOREIGN KEY (id_cliente)              REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_pedidos_negocio_solicitado
        FOREIGN KEY (id_negocio_solicitado)   REFERENCES negocios(id_negocio),
    CONSTRAINT fk_pedidos_repartidor
        FOREIGN KEY (id_repartidor)           REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_pedidos_direccion
        FOREIGN KEY (id_direccion_entrega)    REFERENCES direcciones_usuarios(id_direccion),
    CONSTRAINT fk_pedidos_vehiculo
        FOREIGN KEY (id_vehiculo_utilizado)   REFERENCES vehiculos_negocio(id_vehiculo)
);


CREATE TABLE detalles_pedido (
    id_detalle         INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido          INT NOT NULL,
    id_producto        INT NOT NULL,
    cantidad           INT NOT NULL,
    precio_unitario    DECIMAL(10,2) NOT NULL
                          COMMENT 'Precio del negocio al momento del pedido (snapshot histórico)',

    CONSTRAINT fk_detalles_pedido
        FOREIGN KEY (id_pedido)   REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_producto
        FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);


-- ------------------------------------------
-- 9. APARTADOS DE INVENTARIO POR PEDIDO
-- ------------------------------------------

CREATE TABLE apartados_pedido (
    id_apartado              INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido                INT NOT NULL,
    id_inventario_base       INT
                                COMMENT 'Solo si el apartado vino de la base',
    id_inventario_vehiculo   INT
                                COMMENT 'Solo si el apartado vino del vehículo',
    cantidad                 INT NOT NULL,
    fecha_apartado           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_apartados_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_apartados_base
        FOREIGN KEY (id_inventario_base) REFERENCES inventario_base(id_inventario_base),
    CONSTRAINT fk_apartados_vehiculo
        FOREIGN KEY (id_inventario_vehiculo) REFERENCES inventario_vehiculo(id_inventario_vehiculo),
    CONSTRAINT chk_apartados_origen
        CHECK ((id_inventario_base IS NOT NULL AND id_inventario_vehiculo IS NULL)
            OR (id_inventario_base IS NULL AND id_inventario_vehiculo IS NOT NULL))
);


-- ------------------------------------------
-- 10. BITÁCORAS: MOVIMIENTOS DE INVENTARIO E HISTORIAL DE PEDIDOS
-- ------------------------------------------

CREATE TABLE movimientos_inventario (
    id_movimiento              INT AUTO_INCREMENT PRIMARY KEY,
    id_inventario_base         INT
                                  COMMENT 'Si el movimiento afecta la base',
    id_inventario_vehiculo     INT
                                  COMMENT 'Si el movimiento afecta un vehículo',
    tipo                       VARCHAR(30) NOT NULL
                                  COMMENT 'ENUM: entrada_proveedor, salida_a_vehiculo, entrada_desde_base, salida_pedido, devolucion_a_base, salida_manual, ajuste',
    cantidad                   INT NOT NULL
                                  COMMENT 'Siempre positivo, el tipo determina el signo',
    id_pedido                  INT
                                  COMMENT 'Solo si el movimiento es por un pedido',
    id_movimiento_par          INT
                                  COMMENT 'Enlaza el par de movimientos cuando es un traspaso',
    id_repartidor_responsable  INT
                                  COMMENT 'Quién registró/causó el movimiento',
    costo_unitario             DECIMAL(10,2)
                                  COMMENT 'Solo en entradas de proveedor',
    proveedor                  VARCHAR(150)
                                  COMMENT 'Solo en entradas de proveedor',
    notas                      VARCHAR(255),
    fecha                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mov_base
        FOREIGN KEY (id_inventario_base) REFERENCES inventario_base(id_inventario_base),
    CONSTRAINT fk_mov_vehiculo
        FOREIGN KEY (id_inventario_vehiculo) REFERENCES inventario_vehiculo(id_inventario_vehiculo),
    CONSTRAINT fk_mov_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    CONSTRAINT fk_mov_par
        FOREIGN KEY (id_movimiento_par) REFERENCES movimientos_inventario(id_movimiento),
    CONSTRAINT fk_mov_repartidor
        FOREIGN KEY (id_repartidor_responsable) REFERENCES usuarios(id_usuario),
    CONSTRAINT chk_mov_origen
        CHECK ((id_inventario_base IS NOT NULL AND id_inventario_vehiculo IS NULL)
            OR (id_inventario_base IS NULL AND id_inventario_vehiculo IS NOT NULL))
);


CREATE TABLE historial_estados_pedido (
    id_historial          INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido             INT NOT NULL,
    estado                VARCHAR(50) NOT NULL,
    notas_adicionales     VARCHAR(500)
                             COMMENT 'Ej. motivo de no entrega: cliente_ausente, direccion_incorrecta, sin_pago, etc.',
    latitud               DECIMAL(10,8)
                             COMMENT 'Ubicación del repartidor al momento del cambio de estado',
    longitud              DECIMAL(11,8),
    fecha_cambio          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE
);


-- ==========================================
-- ÍNDICES RECOMENDADOS
-- ==========================================

-- Pedidos
CREATE INDEX idx_pedidos_estado            ON pedidos(estado_actual);
CREATE INDEX idx_pedidos_repartidor_estado ON pedidos(id_repartidor, estado_actual);
CREATE INDEX idx_pedidos_cliente_fecha     ON pedidos(id_cliente, fecha_creacion DESC);
CREATE INDEX idx_pedidos_negocio_estado    ON pedidos(id_negocio_solicitado, estado_actual);
-- Crítico para el cron job que activa pedidos programados
CREATE INDEX idx_pedidos_programados       ON pedidos(es_programado, notificado_programado, fecha_programada);

-- Historial
CREATE INDEX idx_historial_pedido_fecha    ON historial_estados_pedido(id_pedido, fecha_cambio);

-- Movimientos de inventario
CREATE INDEX idx_mov_base_fecha            ON movimientos_inventario(id_inventario_base, fecha DESC);
CREATE INDEX idx_mov_vehiculo_fecha        ON movimientos_inventario(id_inventario_vehiculo, fecha DESC);

-- Apartados
CREATE INDEX idx_apartados_pedido          ON apartados_pedido(id_pedido);

-- Direcciones por usuario
CREATE INDEX idx_direcciones_usuario       ON direcciones_usuarios(id_usuario);

-- Repartidores y vehículos por negocio
CREATE INDEX idx_detalles_negocio          ON detalles_repartidor(id_negocio);
CREATE INDEX idx_vehiculos_negocio         ON vehiculos_negocio(id_negocio);

-- Solicitudes de cambio (panel admin)
CREATE INDEX idx_solicitudes_estado        ON solicitudes_cambio_perfil(estado, fecha_solicitud DESC);
CREATE INDEX idx_solicitudes_negocio       ON solicitudes_cambio_perfil(id_negocio);

-- Horarios por negocio (consulta frecuente para validar si el negocio está abierto)
CREATE INDEX idx_horarios_negocio          ON horarios_negocio(id_negocio);
