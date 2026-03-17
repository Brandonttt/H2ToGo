-- Creación de la base de datos para el proyecto HTOGO (Trabajo Terminal 2026-B176)
CREATE DATABASE IF NOT EXISTS htogo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE htogo_db;

-- 1. Entidad de Identidad
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    rol ENUM('cliente', 'repartidor', 'admin') DEFAULT 'cliente',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Entidad de Detalles del Repartidor
CREATE TABLE detalles_repartidor (
    id_usuario INT PRIMARY KEY,
    licencia_conducir VARCHAR(50) UNIQUE NOT NULL,
    estado_operativo ENUM('desconectado', 'disponible', 'ocupado', 'suspendido') DEFAULT 'desconectado',
    calificacion_promedio DECIMAL(3, 2) DEFAULT 5.00,
    CONSTRAINT fk_repartidor_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 3. Entidad de Vehículos (Relación 1:N con Repartidores)
-- Permite a un repartidor registrar múltiples vehículos (ej. triciclo para distancias cortas, camioneta para largas)
CREATE TABLE vehiculos_repartidores (
    id_vehiculo INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    tipo_vehiculo ENUM('motocicleta', 'automovil', 'camioneta', 'triciclo_carga') NOT NULL,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50),
    color VARCHAR(30) NOT NULL,
    placas VARCHAR(20), -- NULLable porque un triciclo o diablito no tiene placas
    activo BOOLEAN DEFAULT TRUE, -- Borrado lógico
    CONSTRAINT fk_vehiculo_repartidor FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 4. Entidad de Direcciones (Relación 1:N con Usuarios)
-- Mejora la UX al evitar que el usuario ingrese la dirección cada vez.
CREATE TABLE direcciones_usuarios (
    id_direccion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    alias VARCHAR(50) NOT NULL, -- Ej: "Casa", "Oficina", "Local"
    calle VARCHAR(150) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(10) NOT NULL,
    referencias TEXT,
    latitud DECIMAL(10, 8) NOT NULL,
    longitud DECIMAL(11, 8) NOT NULL,
    activo BOOLEAN DEFAULT TRUE, -- Borrado lógico
    CONSTRAINT fk_direccion_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 5. Catálogo de Productos
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL, -- Ej: "Garrafón de Agua"
    marca VARCHAR(50) NOT NULL,   -- Ej: "Bonafont", "Epura", "Genérica"
    capacidad_litros DECIMAL(5, 2) NOT NULL,
    precio_actual DECIMAL(10, 2) NOT NULL,
    stock_disponible INT DEFAULT 0, -- Opcional, dependiendo de si llevarán control de inventario estricto
    activo BOOLEAN DEFAULT TRUE     -- Borrado lógico para no afectar historiales
);

-- 6. Dispositivos para Notificaciones (Tokens FCM)
CREATE TABLE dispositivos_usuarios (
    id_dispositivo INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    token_fcm VARCHAR(255) NOT NULL UNIQUE,
    plataforma ENUM('android', 'ios', 'web') NOT NULL,
    ultima_conexion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 7. Entidad Maestra de Pedidos (Optimizada)
CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_repartidor INT,
    id_direccion_entrega INT NOT NULL, -- Referencia a la dirección estructurada
    id_vehiculo_utilizado INT,         -- Trazabilidad de seguridad: en qué vehículo se entregó
    
    estado_actual ENUM('pendiente', 'aceptado', 'en_camino', 'entregado', 'cancelado') DEFAULT 'pendiente',
    total_pagar DECIMAL(10, 2) NOT NULL, -- Se calcula sumando los detalles del pedido
    
    -- Metadatos de Google Maps API (Caché para latencia y costos)
    ruta_polyline TEXT, 
    distancia_km DECIMAL(6, 2),
    tiempo_estimado_minutos INT,
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega DATETIME,
    
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_pedido_repartidor FOREIGN KEY (id_repartidor) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_pedido_direccion FOREIGN KEY (id_direccion_entrega) REFERENCES direcciones_usuarios(id_direccion),
    CONSTRAINT fk_pedido_vehiculo FOREIGN KEY (id_vehiculo_utilizado) REFERENCES vehiculos_repartidores(id_vehiculo)
);

-- 8. Entidad de Detalle de Pedido (Patrón Master-Detail)
CREATE TABLE detalles_pedido (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL, -- Guarda una "fotografía" del precio al momento de la compra
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 9. Historial de Estados del Pedido (Auditoría)
CREATE TABLE historial_estados_pedido (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    estado ENUM('pendiente', 'aceptado', 'en_camino', 'entregado', 'cancelado') NOT NULL,
    notas_adicionales VARCHAR(255), 
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE
);