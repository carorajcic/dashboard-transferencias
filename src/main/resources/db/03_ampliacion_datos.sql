-- ============================================================
-- AMPLIACION DE DATOS (ejecutar UNA sola vez, despues del 02)
-- 1) Tabla de codigos de respuesta (para el detalle del Nivel 3)
-- 2) ~600 transferencias de los ultimos 30 dias
-- Los datos se generan con SQL (generate_series) con semilla fija,
-- por lo que el resultado es reproducible.
-- ============================================================

BEGIN;

CREATE TABLE IF NOT EXISTS codigos_respuesta (
    codigo          VARCHAR(10)  PRIMARY KEY,
    descripcion     VARCHAR(150) NOT NULL
);

COMMENT ON TABLE codigos_respuesta IS 'Catalogo de codigos de respuesta devueltos por el banco en cada transferencia.';
COMMENT ON COLUMN codigos_respuesta.codigo IS 'Codigo de respuesta (estilo ISO 8583).';
COMMENT ON COLUMN codigos_respuesta.descripcion IS 'Significado del codigo de respuesta.';

INSERT INTO codigos_respuesta (codigo, descripcion) VALUES
('00', 'Operacion aprobada'),
('05', 'No honrar: rechazada por el banco'),
('51', 'Fondos insuficientes'),
('61', 'Excede el limite de monto permitido'),
('91', 'Banco emisor no disponible'),
('96', 'Error de sistema'),
('22', 'Operacion revertida por sospecha de mal funcionamiento')
ON CONFLICT (codigo) DO NOTHING;

-- Semilla fija para que el random() sea reproducible
SELECT setseed(0.42);

WITH pares AS (
    SELECT bp.id_banco, bp.id_producto,
           ROW_NUMBER() OVER (ORDER BY bp.id_banco, bp.id_producto) AS rn
    FROM bancos_productos bp
    WHERE bp.habilitado
),
cant AS (
    SELECT COUNT(*) AS c FROM pares
),
base AS (
    SELECT g,
           1 + FLOOR(random() * (SELECT c FROM cant))::int AS par,
           random() AS r_estado,
           random() AS r_pais,
           random() AS r_monto,
           random() AS r_dia,
           random() AS r_hora,
           random() AS r_cod
    FROM generate_series(1, 600) g
),
calc AS (
    SELECT b.*, p.id_banco, p.id_producto,
           -- probabilidad de aprobacion segun banco (para que el semaforo muestre los 3 estados)
           CASE p.id_banco WHEN 1 THEN 0.95
                           WHEN 2 THEN 0.84
                           WHEN 3 THEN 0.68
                           ELSE 0.94 END AS p_aprob
    FROM base b
    JOIN pares p ON p.rn = b.par
)
INSERT INTO transferencias
    (id_banco, id_producto, id_pais, monto, moneda, estado, codigo_respuesta, fecha_creacion)
SELECT
    c.id_banco,
    c.id_producto,
    (SELECT id_pais FROM paises WHERE codigo_iso =
        CASE WHEN c.r_pais < 0.70 THEN 'ARG'
             WHEN c.r_pais < 0.85 THEN 'BRA'
             WHEN c.r_pais < 0.95 THEN 'CHL'
             ELSE 'URY' END),
    ROUND((1000 + c.r_monto * c.r_monto * 200000)::numeric, 2),
    'ARS',
    est.estado,
    CASE est.estado
        WHEN 'APROBADA'  THEN '00'
        WHEN 'REVERTIDA' THEN '22'
        WHEN 'RECHAZADA' THEN (ARRAY['05','51','51','61','91','96'])[1 + FLOOR(c.r_cod * 6)::int]
        ELSE NULL
    END,
    NOW() - (c.r_dia * 30) * INTERVAL '1 day' - (c.r_hora * 12) * INTERVAL '1 hour'
FROM calc c
CROSS JOIN LATERAL (
    SELECT CASE
        WHEN c.r_estado < 0.03 THEN 'PENDIENTE'
        WHEN c.r_estado < 0.05 THEN 'REVERTIDA'
        WHEN (c.r_estado - 0.05) / 0.95 < c.p_aprob THEN 'APROBADA'
        ELSE 'RECHAZADA'
    END AS estado
) est;

COMMIT;
