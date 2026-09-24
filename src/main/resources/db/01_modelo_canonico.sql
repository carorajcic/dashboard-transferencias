-- ============================================================
-- DASHBOARD DE TRANSFERENCIAS - MODELO CANÓNICO DE BASE DE DATOS
-- Proyecto Final - Tableros y Dashboards - UAI
-- Motor: PostgreSQL
-- ============================================================

-- Usuarios
CREATE TABLE usuarios (
    id_usuario      SERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150),
    fecha_alta      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE usuarios IS 'Usuarios habilitados para iniciar sesión y acceder al tablero. Tabla independiente del modelo de negocio, usada solo por el sistema de autenticación.';
COMMENT ON COLUMN usuarios.id_usuario IS 'Identificador único del usuario.';
COMMENT ON COLUMN usuarios.username IS 'Nombre de usuario utilizado para iniciar sesión. Único en el sistema.';
COMMENT ON COLUMN usuarios.password_hash IS 'Contraseña hasheada con BCrypt. Nunca se almacena en texto plano.';
COMMENT ON COLUMN usuarios.nombre_completo IS 'Nombre completo de la persona, solo para referencia visual en el tablero.';
COMMENT ON COLUMN usuarios.fecha_alta IS 'Fecha y hora en que se dió de alta el usuario.';

-- Bancos
CREATE TABLE bancos (
    id_banco        SERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    cuit            VARCHAR(20)  UNIQUE,
    habilitado      BOOLEAN DEFAULT TRUE,
    fecha_alta      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE bancos IS 'Bancos originantes que inician transferencias.';
COMMENT ON COLUMN bancos.id_banco IS 'Identificador único del banco.';
COMMENT ON COLUMN bancos.nombre IS 'Razón social del banco.';
COMMENT ON COLUMN bancos.cuit IS 'CUIT del banco.';
COMMENT ON COLUMN bancos.habilitado IS 'Indica si el banco está actualmente habilitado para operar en el tablero.';
COMMENT ON COLUMN bancos.fecha_alta IS 'Fecha y hora en que el banco fue dado de alta en el sistema.';

-- Paises
CREATE TABLE paises (
    id_pais         SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    codigo_iso      VARCHAR(3)   NOT NULL UNIQUE,
    es_local        BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE paises IS 'Dimension geográfica utilizada para distinguir operaciones locales de operaciones transfronterizas.';
COMMENT ON COLUMN paises.id_pais IS 'Identificador único del pais.';
COMMENT ON COLUMN paises.nombre IS 'Nombre del país.';
COMMENT ON COLUMN paises.codigo_iso IS 'Código ISO de 3 letras del pais (ej: ARG, BRA).';
COMMENT ON COLUMN paises.es_local IS 'TRUE si es el pais local (Argentina), FALSE si la operación es cross-border.';

-- Productos
CREATE TABLE productos (
    id_producto     SERIAL PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    descripcion     VARCHAR(150) NOT NULL
);

COMMENT ON TABLE productos IS 'Tipos de operacion que un banco puede tener habilitados.';
COMMENT ON COLUMN productos.id_producto IS 'Identificador único del producto.';
COMMENT ON COLUMN productos.codigo IS 'Código corto e interno del producto.';
COMMENT ON COLUMN productos.descripcion IS 'Descripción legible del producto.';

-- Bancos - Productos
CREATE TABLE bancos_productos (
    id_banco        INT NOT NULL REFERENCES bancos(id_banco),
    id_producto     INT NOT NULL REFERENCES productos(id_producto),
    habilitado      BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (id_banco, id_producto)
);

COMMENT ON TABLE bancos_productos IS 'Tabla intermedia que resuelve qué productos tiene habilitado cada banco.';
COMMENT ON COLUMN bancos_productos.id_banco IS 'Referencia al banco.';
COMMENT ON COLUMN bancos_productos.id_producto IS 'Referencia al producto.';
COMMENT ON COLUMN bancos_productos.habilitado IS 'Indica si ese producto esta actualmente habilitado para ese banco en particular.';

-- Transferencias
CREATE TABLE transferencias (
    id_transferencia   SERIAL PRIMARY KEY,
    id_banco           INT NOT NULL REFERENCES bancos(id_banco),
    id_producto        INT NOT NULL REFERENCES productos(id_producto),
    id_pais            INT NOT NULL REFERENCES paises(id_pais),
    monto              NUMERIC(15,2) NOT NULL,
    moneda             VARCHAR(3) DEFAULT 'ARS',
    stan                VARCHAR(20),
    rrn                 VARCHAR(30),
    estado              VARCHAR(20) NOT NULL,
    codigo_respuesta    VARCHAR(10),
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','REVERTIDA'))
);

COMMENT ON TABLE transferencias IS 'Tabla donde se registran las transferencias. Cada fila es una operación individual vinculada a un banco, un producto y un país.';
COMMENT ON COLUMN transferencias.id_transferencia IS 'Identificador único de la transferencia.';
COMMENT ON COLUMN transferencias.id_banco IS 'Banco originante de la transferencia (FK bancos).';
COMMENT ON COLUMN transferencias.id_producto IS 'Producto/tipo de operación utilizado (FK productos).';
COMMENT ON COLUMN transferencias.id_pais IS 'País de origen de los fondos (FK paises).';
COMMENT ON COLUMN transferencias.monto IS 'Monto transferido.';
COMMENT ON COLUMN transferencias.moneda IS 'Moneda de la operación.';
COMMENT ON COLUMN transferencias.estado IS 'Estado actual de la transferencia: PENDIENTE, APROBADA, RECHAZADA o REVERTIDA.';
COMMENT ON COLUMN transferencias.fecha_creacion IS 'Fecha y hora en que se registró la transferencia.';
