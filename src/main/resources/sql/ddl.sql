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
    CONSTRAINT uk_clientes_email                 UNIQUE (email)
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
    CONSTRAINT uk_productos_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_productos_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE IF NOT EXISTS transacciones (
    id                BIGSERIAL       PRIMARY KEY,
    tipo_transaccion  VARCHAR(20)     NOT NULL,
    monto             NUMERIC(19, 2)  NOT NULL,
    fecha             TIMESTAMP       NOT NULL,
    cuenta_origen_id  BIGINT          NOT NULL,
    cuenta_destino_id BIGINT,
    CONSTRAINT transacciones_tipo_transaccion_check
        CHECK (tipo_transaccion IN ('DEPOSITO', 'RETIRO', 'TRANSFERENCIA', 'DEBITO', 'CREDITO')),
    CONSTRAINT fk_transacciones_cuenta_origen
        FOREIGN KEY (cuenta_origen_id)  REFERENCES productos (id),
    CONSTRAINT fk_transacciones_cuenta_destino
        FOREIGN KEY (cuenta_destino_id) REFERENCES productos (id)
);
