-- =============================================================
-- ESQUEMA COMPLETO - Eventos MDP
-- Crea una base nueva o completa una ejecución parcial de este mismo script.
-- También migra servicios del esquema anterior si pertenecían a establecimientos.
-- No elimina bases ni tablas. MySQL 8.0.16 o superior.
-- Ejecutar el archivo completo en MySQL Workbench.
-- Cuentas de demostración: contraseña 123456.
-- =============================================================

CREATE DATABASE IF NOT EXISTS mdp_eventos_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mdp_eventos_db;

SET NAMES utf8mb4;
SET @SQL_SAFE_UPDATES_ANTERIOR = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rol_id INT NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS zonas_mdp (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    codigo_postal VARCHAR(10) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- CREATE TABLE IF NOT EXISTS no modifica tablas antiguas; agrega la columna
-- si zonas_mdp ya existía antes de esta versión del esquema.
SET @ddl_zona_activo = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'zonas_mdp'
          AND COLUMN_NAME = 'activo'
    ),
    'SELECT 1',
    'ALTER TABLE zonas_mdp ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE'
);
PREPARE stmt_zona_activo FROM @ddl_zona_activo;
EXECUTE stmt_zona_activo;
DEALLOCATE PREPARE stmt_zona_activo;

CREATE TABLE IF NOT EXISTS tipos_evento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Mismo ajuste condicional para instalaciones previas del catálogo.
SET @ddl_tipo_evento_activo = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tipos_evento'
          AND COLUMN_NAME = 'activo'
    ),
    'SELECT 1',
    'ALTER TABLE tipos_evento ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE'
);
PREPARE stmt_tipo_evento_activo FROM @ddl_tipo_evento_activo;
EXECUTE stmt_tipo_evento_activo;
DEALLOCATE PREPARE stmt_tipo_evento_activo;

CREATE TABLE IF NOT EXISTS establecimientos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id INT NOT NULL,
    zona_id INT NOT NULL,
    cuit VARCHAR(13) NOT NULL UNIQUE,
    nombre_comercial VARCHAR(120) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_est_proveedor FOREIGN KEY (proveedor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_est_zona FOREIGN KEY (zona_id) REFERENCES zonas_mdp(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS salones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    establecimiento_id INT NOT NULL,
    nombre_salon VARCHAR(100) NOT NULL,
    capacidad_minima INT NOT NULL,
    capacidad_maxima INT NOT NULL,
    hora_apertura TIME NOT NULL,
    hora_cierre TIME NOT NULL,
    tarifa_base_hora DECIMAL(12, 2) NOT NULL,
    costo_por_persona DECIMAL(12, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_capacidad_min CHECK (capacidad_minima > 0),
    CONSTRAINT chk_capacidad_max CHECK (capacidad_maxima >= capacidad_minima),
    CONSTRAINT chk_tarifas CHECK (tarifa_base_hora > 0 AND costo_por_persona > 0),
    CONSTRAINT uk_salon_establecimiento_nombre UNIQUE (establecimiento_id, nombre_salon),
    CONSTRAINT fk_salon_establecimiento
        FOREIGN KEY (establecimiento_id) REFERENCES establecimientos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS salon_tipo_evento (
    salon_id INT NOT NULL,
    tipo_evento_id INT NOT NULL,
    PRIMARY KEY (salon_id, tipo_evento_id),
    CONSTRAINT fk_ste_salon FOREIGN KEY (salon_id) REFERENCES salones(id) ON DELETE CASCADE,
    CONSTRAINT fk_ste_tipo_evento FOREIGN KEY (tipo_evento_id) REFERENCES tipos_evento(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS disponibilidad_semanal (
    id INT AUTO_INCREMENT PRIMARY KEY,
    salon_id INT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_salon_dia (salon_id, dia_semana),
    CONSTRAINT chk_dia_semana
        CHECK (dia_semana IN ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),
    CONSTRAINT fk_disp_salon FOREIGN KEY (salon_id) REFERENCES salones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS servicios_base (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    precio DECIMAL(12, 2) NOT NULL,
    cobro_por_invitado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_precio_servicio_base CHECK (precio > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS servicios_adicionales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    salon_id INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    precio DECIMAL(12, 2) NOT NULL,
    cobro_por_invitado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_servicio_salon_nombre (salon_id, nombre),
    CONSTRAINT chk_precio_servicio CHECK (precio > 0),
    CONSTRAINT fk_serv_salon FOREIGN KEY (salon_id) REFERENCES salones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Migra una tabla heredada de la versión anterior, donde los servicios
-- pertenecían al establecimiento. En una base nueva no hace nada.
SET @servicios_heredados = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'servicios_adicionales'
      AND COLUMN_NAME = 'establecimiento_id'
);

SET @sql_migrar_servicios = IF(
    @servicios_heredados > 0,
    'ALTER TABLE servicios_adicionales
        DROP FOREIGN KEY fk_serv_establecimiento,
        DROP INDEX uk_servicio_establecimiento_nombre,
        DROP CHECK chk_precio_servicio,
        ADD COLUMN salon_id INT NULL,
        ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1'
);
PREPARE stmt_migrar_servicios FROM @sql_migrar_servicios;
EXECUTE stmt_migrar_servicios;
DEALLOCATE PREPARE stmt_migrar_servicios;

SET @sql_mapa_servicios = IF(
    @servicios_heredados > 0,
    'CREATE TEMPORARY TABLE _servicios_por_salon AS
        SELECT sa.id AS servicio_original_id, s.id AS salon_id,
               CAST(NULL AS SIGNED) AS servicio_nuevo_id
        FROM servicios_adicionales sa
        JOIN salones s ON s.establecimiento_id = sa.establecimiento_id',
    'SELECT 1'
);
PREPARE stmt_mapa_servicios FROM @sql_mapa_servicios;
EXECUTE stmt_mapa_servicios;
DEALLOCATE PREPARE stmt_mapa_servicios;

SET @sql_indice_mapa = IF(
    @servicios_heredados > 0,
    'ALTER TABLE _servicios_por_salon ADD PRIMARY KEY (servicio_original_id, salon_id)',
    'SELECT 1'
);
PREPARE stmt_indice_mapa FROM @sql_indice_mapa;
EXECUTE stmt_indice_mapa;
DEALLOCATE PREPARE stmt_indice_mapa;

SET @sql_asignar_servicios = IF(
    @servicios_heredados > 0,
    'UPDATE servicios_adicionales sa
        JOIN (
            SELECT servicio_original_id, MIN(salon_id) AS salon_id
            FROM _servicios_por_salon
            GROUP BY servicio_original_id
        ) asignacion ON asignacion.servicio_original_id = sa.id
        SET sa.salon_id = asignacion.salon_id',
    'SELECT 1'
);
PREPARE stmt_asignar_servicios FROM @sql_asignar_servicios;
EXECUTE stmt_asignar_servicios;
DEALLOCATE PREPARE stmt_asignar_servicios;

SET @sql_borrar_servicios_sin_salon = IF(
    @servicios_heredados > 0,
    'DELETE FROM servicios_adicionales
        WHERE salon_id IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM detalle_reserva_servicios d
              WHERE d.servicio_id = servicios_adicionales.id
          )',
    'SELECT 1'
);
PREPARE stmt_borrar_servicios_sin_salon FROM @sql_borrar_servicios_sin_salon;
EXECUTE stmt_borrar_servicios_sin_salon;
DEALLOCATE PREPARE stmt_borrar_servicios_sin_salon;

SET @sql_clonar_servicios = IF(
    @servicios_heredados > 0,
    'INSERT INTO servicios_adicionales
        (establecimiento_id, salon_id, nombre, descripcion, precio, cobro_por_invitado, activo)
     SELECT original.establecimiento_id, mapa.salon_id, original.nombre, original.descripcion,
            original.precio, original.cobro_por_invitado, original.activo
     FROM _servicios_por_salon mapa
     JOIN servicios_adicionales original ON original.id = mapa.servicio_original_id
     WHERE mapa.salon_id <> original.salon_id',
    'SELECT 1'
);
PREPARE stmt_clonar_servicios FROM @sql_clonar_servicios;
EXECUTE stmt_clonar_servicios;
DEALLOCATE PREPARE stmt_clonar_servicios;

SET @sql_actualizar_mapa_servicios = IF(
    @servicios_heredados > 0,
    'UPDATE _servicios_por_salon mapa
        JOIN servicios_adicionales original ON original.id = mapa.servicio_original_id
        JOIN servicios_adicionales copia
          ON copia.establecimiento_id = original.establecimiento_id
         AND copia.salon_id = mapa.salon_id
         AND copia.nombre = original.nombre
        SET mapa.servicio_nuevo_id = copia.id',
    'SELECT 1'
);
PREPARE stmt_actualizar_mapa_servicios FROM @sql_actualizar_mapa_servicios;
EXECUTE stmt_actualizar_mapa_servicios;
DEALLOCATE PREPARE stmt_actualizar_mapa_servicios;

SET @sql_actualizar_detalle_servicios = IF(
    @servicios_heredados > 0,
    'UPDATE detalle_reserva_servicios detalle
        JOIN reservas reserva ON reserva.id = detalle.reserva_id
        JOIN _servicios_por_salon mapa
          ON mapa.servicio_original_id = detalle.servicio_id
         AND mapa.salon_id = reserva.salon_id
        SET detalle.servicio_id = mapa.servicio_nuevo_id',
    'SELECT 1'
);
PREPARE stmt_actualizar_detalle_servicios FROM @sql_actualizar_detalle_servicios;
EXECUTE stmt_actualizar_detalle_servicios;
DEALLOCATE PREPARE stmt_actualizar_detalle_servicios;

SET @sql_eliminar_mapa_servicios = IF(
    @servicios_heredados > 0,
    'DROP TEMPORARY TABLE _servicios_por_salon',
    'SELECT 1'
);
PREPARE stmt_eliminar_mapa_servicios FROM @sql_eliminar_mapa_servicios;
EXECUTE stmt_eliminar_mapa_servicios;
DEALLOCATE PREPARE stmt_eliminar_mapa_servicios;

SET @sql_finalizar_migracion_servicios = IF(
    @servicios_heredados > 0,
    'ALTER TABLE servicios_adicionales
        MODIFY COLUMN salon_id INT NOT NULL,
        DROP COLUMN establecimiento_id,
        ADD UNIQUE KEY uk_servicio_salon_nombre (salon_id, nombre),
        ADD CONSTRAINT chk_precio_servicio CHECK (precio > 0),
        ADD CONSTRAINT fk_serv_salon
            FOREIGN KEY (salon_id) REFERENCES salones(id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt_finalizar_migracion_servicios FROM @sql_finalizar_migracion_servicios;
EXECUTE stmt_finalizar_migracion_servicios;
DEALLOCATE PREPARE stmt_finalizar_migracion_servicios;

CREATE TABLE IF NOT EXISTS reservas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo_reserva VARCHAR(20) NOT NULL UNIQUE,
    cliente_id INT NOT NULL,
    salon_id INT NOT NULL,
    tipo_evento_id INT NOT NULL,
    fecha_evento DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    cantidad_invitados INT NOT NULL,
    monto_total DECIMAL(12, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    vence_en DATETIME NOT NULL,
    motivo_rechazo VARCHAR(500),
    motivo_cancelacion VARCHAR(500),
    cancelada_por_usuario_id INT,
    cancelada_en DATETIME,
    requiere_revision_admin BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invitados CHECK (cantidad_invitados > 0),
    CONSTRAINT chk_monto_total CHECK (monto_total >= 0),
    CONSTRAINT chk_estado_reserva
        CHECK (estado IN ('PENDIENTE','CONFIRMADA','RECHAZADA','CANCELADA','VENCIDA','FINALIZADA')),
    CONSTRAINT fk_res_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_res_salon FOREIGN KEY (salon_id) REFERENCES salones(id),
    CONSTRAINT fk_res_tipo_evento FOREIGN KEY (tipo_evento_id) REFERENCES tipos_evento(id),
    CONSTRAINT fk_res_cancelada_por FOREIGN KEY (cancelada_por_usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

-- Agrega la marca si se parte de la versión previa del esquema.
SET @sql_revision_admin = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'reservas'
          AND COLUMN_NAME = 'requiere_revision_admin'
    ),
    'SELECT 1',
    'ALTER TABLE reservas ADD COLUMN requiere_revision_admin BOOLEAN NOT NULL DEFAULT FALSE'
);
PREPARE stmt_revision_admin FROM @sql_revision_admin;
EXECUTE stmt_revision_admin;
DEALLOCATE PREPARE stmt_revision_admin;

CREATE TABLE IF NOT EXISTS detalle_reserva_servicios (
    reserva_id INT NOT NULL,
    servicio_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario_pactado DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (reserva_id, servicio_id),
    CONSTRAINT chk_cant_serv CHECK (cantidad > 0),
    CONSTRAINT fk_det_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE CASCADE,
    CONSTRAINT fk_det_servicio FOREIGN KEY (servicio_id) REFERENCES servicios_adicionales(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pagos_sena (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    metodo_pago VARCHAR(40) NOT NULL,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_monto_sena CHECK (monto > 0),
    CONSTRAINT fk_pago_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id)
) ENGINE=InnoDB;

-- Roles que la aplicación consulta en la tabla roles.
INSERT INTO roles (nombre, descripcion) VALUES
    ('ROLE_ADMIN', 'Administrador general del sistema'),
    ('ROLE_PROVEEDOR', 'Proveedor de establecimientos y salones'),
    ('ROLE_CLIENTE', 'Cliente que busca y reserva salones')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- Usuarios de demostración. La aplicación acepta este hash PBKDF2.
-- Cambiar las contraseñas de demostración al comenzar a usar el sistema.
INSERT INTO usuarios (rol_id, nombre_completo, email, password_hash, telefono)
SELECT r.id, datos.nombre_completo, datos.email, datos.password_hash, datos.telefono
FROM (
    SELECT 'ROLE_ADMIN' AS rol, 'Administrador MDP' AS nombre_completo,
           'admin@mdp.com' AS email,
           'pbkdf2$210000$ASNFZ4mrze8BI0VniavN7w==$+H8Qc+0JqEtp2+QI+dttx+xfk7cuHAzrjb6mk209amk=' AS password_hash,
           '223-4000001' AS telefono
    UNION ALL
    SELECT 'ROLE_PROVEEDOR', 'Proveedor de prueba', 'proveedor@mdp.com',
           'pbkdf2$210000$ASNFZ4mrze8BI0VniavN7w==$+H8Qc+0JqEtp2+QI+dttx+xfk7cuHAzrjb6mk209amk=',
           '223-4000002'
    UNION ALL
    SELECT 'ROLE_CLIENTE', 'Gastón Tiberi', 'cliente@mdp.com',
           'pbkdf2$210000$ASNFZ4mrze8BI0VniavN7w==$+H8Qc+0JqEtp2+QI+dttx+xfk7cuHAzrjb6mk209amk=',
           '223-4000003'
) AS datos
JOIN roles r ON r.nombre = datos.rol
ON DUPLICATE KEY UPDATE
    rol_id = VALUES(rol_id),
    nombre_completo = VALUES(nombre_completo),
    password_hash = VALUES(password_hash),
    telefono = VALUES(telefono),
    activo = TRUE;

INSERT INTO zonas_mdp (nombre, codigo_postal) VALUES
    ('Centro / Peatonal San Martín', '7600'),
    ('Playa Grande / Alem', '7600'),
    ('Güemes / Olavarría', '7600'),
    ('Av. Constitución', '7600'),
    ('Torreón / Paseo Jesús de Galíndez', '7600')
ON DUPLICATE KEY UPDATE codigo_postal = VALUES(codigo_postal), activo = TRUE;

INSERT INTO tipos_evento (nombre, descripcion) VALUES
    ('Cumpleaños Infantil / Juvenil', 'Festejos con juegos, animación y menú infantil'),
    ('Casamiento / Fiesta de 15', 'Eventos sociales formales con recepción y pista'),
    ('Evento Corporativo / Reunión', 'Jornadas empresariales y cenas de fin de año'),
    ('Despedida / Egresados', 'Celebraciones grupales con gastronomía y música')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion), activo = TRUE;

INSERT INTO establecimientos
    (proveedor_id, zona_id, cuit, nombre_comercial, direccion)
SELECT u.id, z.id, datos.cuit, datos.nombre_comercial, datos.direccion
FROM (
    SELECT '30-55555555-1' AS cuit, 'Sacoa Peatonal San Martín' AS nombre_comercial,
           'Centro / Peatonal San Martín' AS zona, 'San Martín 2345, Mar del Plata' AS direccion
    UNION ALL
    SELECT '30-66666666-2', 'Manolo Playa Grande', 'Playa Grande / Alem',
           'Blvd. Marítimo Patricio Peralta Ramos 5101, Mar del Plata'
    UNION ALL
    SELECT '30-77777777-3', 'Torreón del Monje', 'Torreón / Paseo Jesús de Galíndez',
           'Paseo Jesús de Galíndez s/n, Mar del Plata'
    UNION ALL
    SELECT '30-88888888-4', 'Multiespacio Constitución', 'Av. Constitución',
           'Av. Constitución 4820, Mar del Plata'
) AS datos
JOIN usuarios u ON u.email = 'proveedor@mdp.com'
JOIN zonas_mdp z ON z.nombre = datos.zona
ON DUPLICATE KEY UPDATE
    zona_id = VALUES(zona_id),
    nombre_comercial = VALUES(nombre_comercial),
    direccion = VALUES(direccion),
    activo = TRUE;

INSERT INTO salones (
    establecimiento_id, nombre_salon, capacidad_minima, capacidad_maxima,
    hora_apertura, hora_cierre, tarifa_base_hora, costo_por_persona
)
SELECT e.id, datos.nombre_salon, datos.capacidad_minima, datos.capacidad_maxima,
       datos.hora_apertura, datos.hora_cierre, datos.tarifa_base_hora, datos.costo_por_persona
FROM (
    SELECT '30-55555555-1' AS cuit, 'Sector Cumpleaños VIP + Tarjetas' AS nombre_salon,
           10 AS capacidad_minima, 45 AS capacidad_maxima, '10:00:00' AS hora_apertura,
           '23:00:00' AS hora_cierre, 25000.00 AS tarifa_base_hora, 8500.00 AS costo_por_persona
    UNION ALL
    SELECT '30-66666666-2', 'Salón Vista al Mar Planta Alta', 15, 80, '12:00:00',
           '23:59:00', 40000.00, 14500.00
    UNION ALL
    SELECT '30-77777777-3', 'Salón Piedra del Torreón', 40, 220, '11:00:00',
           '23:59:00', 95000.00, 22000.00
    UNION ALL
    SELECT '30-88888888-4', 'Salón Principal Constitución', 30, 150, '12:00:00',
           '23:59:00', 60000.00, 16000.00
) AS datos
JOIN establecimientos e ON e.cuit = datos.cuit
ON DUPLICATE KEY UPDATE
    capacidad_minima = VALUES(capacidad_minima),
    capacidad_maxima = VALUES(capacidad_maxima),
    hora_apertura = VALUES(hora_apertura),
    hora_cierre = VALUES(hora_cierre),
    tarifa_base_hora = VALUES(tarifa_base_hora),
    costo_por_persona = VALUES(costo_por_persona),
    activo = TRUE;

INSERT INTO salon_tipo_evento (salon_id, tipo_evento_id)
SELECT s.id, t.id
FROM (
    SELECT '30-55555555-1' AS cuit, 'Sector Cumpleaños VIP + Tarjetas' AS nombre_salon,
           'Cumpleaños Infantil / Juvenil' AS tipo
    UNION ALL SELECT '30-55555555-1', 'Sector Cumpleaños VIP + Tarjetas', 'Despedida / Egresados'
    UNION ALL SELECT '30-66666666-2', 'Salón Vista al Mar Planta Alta', 'Cumpleaños Infantil / Juvenil'
    UNION ALL SELECT '30-66666666-2', 'Salón Vista al Mar Planta Alta', 'Evento Corporativo / Reunión'
    UNION ALL SELECT '30-66666666-2', 'Salón Vista al Mar Planta Alta', 'Despedida / Egresados'
    UNION ALL SELECT '30-77777777-3', 'Salón Piedra del Torreón', 'Casamiento / Fiesta de 15'
    UNION ALL SELECT '30-77777777-3', 'Salón Piedra del Torreón', 'Evento Corporativo / Reunión'
    UNION ALL SELECT '30-88888888-4', 'Salón Principal Constitución', 'Cumpleaños Infantil / Juvenil'
    UNION ALL SELECT '30-88888888-4', 'Salón Principal Constitución', 'Casamiento / Fiesta de 15'
    UNION ALL SELECT '30-88888888-4', 'Salón Principal Constitución', 'Despedida / Egresados'
) AS datos
JOIN establecimientos e ON e.cuit = datos.cuit
JOIN salones s ON s.establecimiento_id = e.id AND s.nombre_salon = datos.nombre_salon
JOIN tipos_evento t ON t.nombre = datos.tipo AND t.activo = TRUE
ON DUPLICATE KEY UPDATE salon_id = VALUES(salon_id);

INSERT INTO disponibilidad_semanal (salon_id, dia_semana, activo)
SELECT s.id, dias.dia_semana, TRUE
FROM salones s
JOIN establecimientos e ON e.id = s.establecimiento_id
JOIN (
    SELECT 'LUNES' AS dia_semana
    UNION ALL SELECT 'MARTES'
    UNION ALL SELECT 'MIERCOLES'
    UNION ALL SELECT 'JUEVES'
    UNION ALL SELECT 'VIERNES'
    UNION ALL SELECT 'SABADO'
    UNION ALL SELECT 'DOMINGO'
) AS dias
WHERE e.cuit IN ('30-55555555-1','30-66666666-2','30-77777777-3','30-88888888-4')
ON DUPLICATE KEY UPDATE activo = TRUE;

INSERT INTO servicios_base (nombre, descripcion, precio, cobro_por_invitado) VALUES
    ('DJ e iluminación', 'Servicio de música e iluminación para eventos', 180000.00, FALSE),
    ('Cabina de fotos', 'Cabina de fotos y animación', 95000.00, FALSE),
    ('Menú por invitado', 'Servicio gastronómico por persona', 7800.00, TRUE)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    precio = VALUES(precio),
    cobro_por_invitado = VALUES(cobro_por_invitado),
    activo = TRUE;

INSERT INTO servicios_adicionales
    (salon_id, nombre, descripcion, precio, cobro_por_invitado, activo)
SELECT s.id, datos.nombre, datos.descripcion, datos.precio, datos.cobro_por_invitado, TRUE
FROM (
    SELECT '30-55555555-1' AS cuit, 'Sector Cumpleaños VIP + Tarjetas' AS nombre_salon,
           'Tarjeta Libre 2hs + 500 Tickets' AS nombre,
           'Acceso a videojuegos por invitado' AS descripcion, 6500.00 AS precio,
           TRUE AS cobro_por_invitado
    UNION ALL
    SELECT '30-66666666-2', 'Salón Vista al Mar Planta Alta',
           'Menú Churros + Submarino / Hamburguesa',
           'Merienda o cena clásica por persona', 7800.00, TRUE
    UNION ALL
    SELECT '30-77777777-3', 'Salón Piedra del Torreón',
           'Servicio DJ e Iluminación Profesional',
           'Equipamiento para todo el evento', 180000.00, FALSE
    UNION ALL
    SELECT '30-88888888-4', 'Salón Principal Constitución',
           'Cabina de Fotos y Animación',
           'Incluye cotillón y coordinación', 95000.00, FALSE
) AS datos
JOIN establecimientos e ON e.cuit = datos.cuit
JOIN salones s ON s.establecimiento_id = e.id AND s.nombre_salon = datos.nombre_salon
WHERE s.id IS NOT NULL
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    precio = VALUES(precio),
    cobro_por_invitado = VALUES(cobro_por_invitado),
    activo = TRUE;

SET SQL_SAFE_UPDATES = @SQL_SAFE_UPDATES_ANTERIOR;
