-- DATOS INICIALES DE CARGA

INSERT INTO usuarios (username, password_hash, nombre_completo) VALUES
('admin', '$2b$10$SwiBYkHQHfFe9PfaaDGs2./UzLXEKIfi0k3W6jHqM5hvWFh0a06Ce', 'Administrador del Tablero');

-- Países
INSERT INTO paises (nombre, codigo_iso, es_local) VALUES
('Argentina', 'ARG', TRUE),
('Brasil', 'BRA', FALSE),
('Chile', 'CHL', FALSE),
('Uruguay', 'URY', FALSE);

-- Productos
INSERT INTO productos (codigo, descripcion) VALUES
('FONDEO_CUENTA', 'Fondeo de cuenta propia'),
('TRANSFERENCIA_INTERBANCARIA', 'Transferencia entre bancos');

-- Bancos
INSERT INTO bancos (nombre, cuit, habilitado) VALUES
('Banco Galicia', '30-50000001-1', TRUE),
('Banco Santander', '30-50000002-2', TRUE),
('Brubank', '30-50000003-3', TRUE),
('Banco Nación', '30-50000004-4', TRUE);

-- Productos habilitados por banco
INSERT INTO bancos_productos (id_banco, id_producto, habilitado) VALUES
(1, 1, TRUE), (1, 2, TRUE),
(2, 1, TRUE),
(3, 1, TRUE), (3, 2, TRUE),
(4, 1, TRUE);

-- Transferencias
INSERT INTO transferencias (id_banco, id_producto, id_pais, monto, estado, codigo_respuesta, fecha_creacion) VALUES
(1, 1, 1, 15000.00, 'APROBADA', '00', NOW() - INTERVAL '2 days'),
(1, 1, 1, 8000.00, 'APROBADA', '00', NOW() - INTERVAL '2 days'),
(1, 1, 2, 22000.00, 'RECHAZADA', '51', NOW() - INTERVAL '1 days'),
(2, 1, 1, 5000.00, 'APROBADA', '00', NOW() - INTERVAL '1 days'),
(2, 1, 1, 3000.00, 'RECHAZADA', '61', NOW()),
(2, 1, 3, 12000.00, 'RECHAZADA', '96', NOW()),
(3, 1, 1, 9000.00, 'APROBADA', '00', NOW()),
(3, 2, 1, 4000.00, 'PENDIENTE', NULL, NOW()),
(4, 1, 4, 7000.00, 'APROBADA', '00', NOW());
