-- ============================================================
--  MiniBanco — DDL (Data Definition Language)
--  Base de datos: PostgreSQL 14+
--  Nota: Hibernate genera estas tablas automáticamente con
--        ddl-auto=update. Este script sirve para referencia,
--        revisión en entrevista y despliegue manual.
-- ============================================================

CREATE TABLE IF NOT EXISTS clientes (
    id                    BIGSERIAL    PRIMARY KEY,
    tipo_identificacion   VARCHAR(20)  NOT NULL,
    numero_identificacion VARCHAR(20)  NOT NULL,
    nombres               VARCHAR(100) NOT NULL,
    apellidos             VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    fecha_nacimiento      DATE         NOT NULL,
    fecha_creacion        TIMESTAMP    NOT NULL,
    fecha_modificacion    TIMESTAMP,
    CONSTRAINT uk_clientes_numero_identificacion UNIQUE (numero_identificacion),
    CONSTRAINT uk_clientes_email                 UNIQUE (email),
    CONSTRAINT chk_cliente_mayor_de_edad
        CHECK (fecha_nacimiento <= CURRENT_DATE - INTERVAL '18 years')
);

CREATE TABLE IF NOT EXISTS productos (
    id                 BIGSERIAL       PRIMARY KEY,
    tipo_cuenta        VARCHAR(20)     NOT NULL,
    numero_cuenta      VARCHAR(20)     NOT NULL,
    estado             VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    saldo              NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    exenta_gmf         BOOLEAN         NOT NULL DEFAULT FALSE,
    cliente_id         BIGINT          NOT NULL,
    fecha_creacion     TIMESTAMP       NOT NULL,
    fecha_modificacion TIMESTAMP,
    CONSTRAINT uk_productos_numero_cuenta        UNIQUE (numero_cuenta),
    CONSTRAINT fk_productos_cliente              FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT chk_productos_tipo_cuenta         CHECK (tipo_cuenta IN ('AHORRO', 'CORRIENTE')),
    CONSTRAINT chk_productos_estado              CHECK (estado IN ('ACTIVA', 'INACTIVA', 'CANCELADA')),
    CONSTRAINT chk_productos_saldo_no_negativo   CHECK (saldo >= 0),
    CONSTRAINT chk_productos_cancelada_saldo_cero
        CHECK (estado != 'CANCELADA' OR saldo = 0),
    CONSTRAINT chk_productos_numero_cuenta_prefijo
        CHECK (
            (tipo_cuenta = 'AHORRO'    AND numero_cuenta LIKE '53%') OR
            (tipo_cuenta = 'CORRIENTE' AND numero_cuenta LIKE '33%')
        )
);

CREATE TABLE IF NOT EXISTS transacciones (
    id                BIGSERIAL       PRIMARY KEY,
    tipo_transaccion  VARCHAR(20)     NOT NULL,
    monto             NUMERIC(19, 2)  NOT NULL,
    fecha             TIMESTAMP       NOT NULL,
    cuenta_origen_id  BIGINT          NOT NULL,
    cuenta_destino_id BIGINT,
    CONSTRAINT fk_transacciones_cuenta_origen
        FOREIGN KEY (cuenta_origen_id)  REFERENCES productos (id),
    CONSTRAINT fk_transacciones_cuenta_destino
        FOREIGN KEY (cuenta_destino_id) REFERENCES productos (id),
    CONSTRAINT chk_transacciones_monto_positivo  CHECK (monto > 0),
    CONSTRAINT chk_transacciones_tipo
        CHECK (tipo_transaccion IN ('DEPOSITO', 'RETIRO', 'DEBITO', 'CREDITO')),
    CONSTRAINT chk_transacciones_cuentas_distintas
        CHECK (cuenta_destino_id IS NULL OR cuenta_origen_id != cuenta_destino_id)
);


-- ============================================================
--  TRIGGERs
--  Reglas que requieren comparar OLD vs NEW o cruzar tablas
-- ============================================================

-- Impide cualquier UPDATE sobre una cuenta ya CANCELADA
CREATE OR REPLACE FUNCTION fn_no_modificar_cancelada()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado = 'CANCELADA' THEN
        RAISE EXCEPTION 'No se puede modificar una cuenta cancelada';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_no_modificar_cancelada
    BEFORE UPDATE ON productos
    FOR EACH ROW
    EXECUTE FUNCTION fn_no_modificar_cancelada();


-- Impide registrar transacciones sobre cuentas que no estén ACTIVAS
CREATE OR REPLACE FUNCTION fn_verificar_cuentas_activas()
RETURNS TRIGGER AS $$
DECLARE
    estado_origen  VARCHAR(20);
    estado_destino VARCHAR(20);
BEGIN
    SELECT estado INTO estado_origen
    FROM productos WHERE id = NEW.cuenta_origen_id;

    IF estado_origen != 'ACTIVA' THEN
        RAISE EXCEPTION 'La cuenta origen no está activa';
    END IF;

    IF NEW.cuenta_destino_id IS NOT NULL THEN
        SELECT estado INTO estado_destino
        FROM productos WHERE id = NEW.cuenta_destino_id;

        IF estado_destino != 'ACTIVA' THEN
            RAISE EXCEPTION 'La cuenta destino no está activa';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_verificar_cuentas_activas
    BEFORE INSERT ON transacciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_verificar_cuentas_activas();
