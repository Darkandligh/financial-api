-- ============================================================
--  MiniBanco — DML (Data Manipulation Language)
--  Datos de prueba — ejecutar DESPUÉS del DDL
-- ============================================================

-- Clientes
INSERT INTO clientes (tipo_identificacion, numero_identificacion, nombres, apellidos,
                      email, fecha_nacimiento, fecha_creacion, fecha_modificacion)
VALUES
  ('CC',  '1099887766', 'Juan',   'Pérez García',    'juan.perez@email.com',  '1990-05-15', NOW(), NOW()),
  ('CC',  '987654321',  'María',  'López Rodríguez', 'maria.lopez@email.com', '1985-08-22', NOW(), NOW()),
  ('NIT', '900123456',  'Carlos', 'Martínez Ruiz',   'c.martinez@email.com',  '1992-11-30', NOW(), NOW());

-- Productos (cuentas)
INSERT INTO productos (tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf,
                       cliente_id, fecha_creacion, fecha_modificacion)
VALUES
  ('AHORRO',    '5312345678', 'ACTIVA',   500000.00, FALSE, 1, NOW(), NOW()),
  ('CORRIENTE', '3398765432', 'ACTIVA',  1200000.00, TRUE,  1, NOW(), NOW()),
  ('AHORRO',    '5387654321', 'ACTIVA',   250000.00, FALSE, 2, NOW(), NOW());

-- Transacciones
INSERT INTO transacciones (tipo_transaccion, monto, fecha, cuenta_origen_id, cuenta_destino_id)
VALUES
  ('DEPOSITO', 500000.00, NOW(), 1, NULL),
  ('RETIRO',   100000.00, NOW(), 1, NULL),
  ('DEBITO',   200000.00, NOW(), 2, 3),
  ('CREDITO',  200000.00, NOW(), 2, 3);

-- Consultas de verificación
SELECT 'Clientes:'    AS tabla, COUNT(*) AS total FROM clientes
UNION ALL
SELECT 'Productos:',  COUNT(*) FROM productos
UNION ALL
SELECT 'Transacciones:', COUNT(*) FROM transacciones;
