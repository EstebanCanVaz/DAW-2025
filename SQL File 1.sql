create database dbpos1;

use dbpos1;

-- Tabla de usuarios para el sistema de login
create table usuario (
    idusuario int auto_increment not null primary key,
    username varchar(50) unique not null,
    password varchar(255) not null,
    nombre varchar(100) not null,
    rol enum('admin', 'vendedor') default 'vendedor',
    activo boolean default true,
    fecha_creacion timestamp default current_timestamp
);

-- Insertar usuario administrador por defecto (password: admin123)
insert into usuario (username, password, nombre, rol) 
values ('admin', 'admin123', 'Administrador', 'admin');

-- Insertar usuario vendedor de ejemplo (password: vendedor123)
insert into usuario (username, password, nombre, rol) 
values ('vendedor', 'vendedor123', 'Vendedor', 'vendedor');

create table producto (
idproducto int auto_increment not null primary key,
nombre varchar(100),
precioProducto decimal (10,2),
stock int
);

-- Datos de ejemplo
insert into producto(nombre,precioProducto,stock) values 
("Lapiz", 6.50, 50),
("Libreta", 40.00, 30),
("Borrador", 3.50, 100),
("Pluma", 12.00, 45),
("Marcador", 15.00, 25);

create table cliente (
idcliente int auto_increment not null primary key,
nombre varchar(100),
appaterno varchar(100),
apmaterno varchar(100)
);

-- Datos de ejemplo
insert into cliente (nombre,appaterno,apmaterno) values 
("Esteban", "Canto", "Vazquez"),
("Edgar", "Aguilar", "Moguel"),
("Maria", "Lopez", "Garcia"),
("Juan", "Perez", "Martinez"),
("Ana", "Rodriguez", "Sanchez");

create table factura (
idfactura int auto_increment not null primary key,
fechaFactura date,
fkcliente int, 
foreign key (fkcliente) references cliente(idcliente)
);

-- Datos de ejemplo de facturas
insert into factura (fechaFactura,fkcliente) values
(curdate(), 1),
(curdate(), 2),
(DATE_SUB(curdate(), INTERVAL 1 DAY), 3);

create table detalle (
iddetalle int auto_increment not null primary key,
fkfactura int,
foreign key (fkfactura) references factura(idfactura),
fkproducto int,
foreign key (fkproducto) references producto(idproducto),
cantidad int,
precioVenta decimal(10,2)
);

-- Datos de ejemplo de detalles (sin reducir stock aún)
insert into detalle (fkfactura,fkproducto,cantidad,precioVenta) values
(1, 1, 10, 6.50),
(1, 2, 5, 40.00),
(2, 3, 20, 3.50),
(2, 4, 8, 12.00),
(3, 5, 15, 15.00);

-- ============================================
-- TABLA PARA DOCUMENTOS DE CLIENTES (Subida/Descarga de archivos)
-- ============================================
create table documento_cliente (
    iddocumento int auto_increment not null primary key,
    fkcliente int not null,
    nombre_archivo varchar(255) not null,
    tipo_archivo varchar(50),
    ruta_archivo varchar(500) not null,
    fecha_subida timestamp default current_timestamp,
    foreign key (fkcliente) references cliente(idcliente) on delete cascade
);

-- ============================================
-- VISTAS (VIEWS) - Snapshot de datos
-- ============================================

-- Vista de resumen de ventas por producto
CREATE VIEW vista_ventas_producto AS
SELECT 
    p.idproducto,
    p.nombre as producto,
    COUNT(d.iddetalle) as total_ventas,
    SUM(d.cantidad) as cantidad_vendida,
    SUM(d.cantidad * d.precioVenta) as ingresos_totales,
    AVG(d.precioVenta) as precio_promedio
FROM producto p
LEFT JOIN detalle d ON p.idproducto = d.fkproducto
GROUP BY p.idproducto, p.nombre;

-- Vista de clientes con sus compras
CREATE VIEW vista_clientes_compras AS
SELECT 
    c.idcliente,
    c.nombre,
    c.appaterno,
    c.apmaterno,
    COUNT(DISTINCT f.idfactura) as total_compras,
    COALESCE(SUM(d.cantidad * d.precioVenta), 0) as total_gastado,
    MAX(f.fechaFactura) as ultima_compra
FROM cliente c
LEFT JOIN factura f ON c.idcliente = f.fkcliente
LEFT JOIN detalle d ON f.idfactura = d.fkfactura
GROUP BY c.idcliente, c.nombre, c.appaterno, c.apmaterno;

-- Vista de facturas con detalles completos
CREATE VIEW vista_facturas_completas AS
SELECT 
    f.idfactura,
    f.fechaFactura,
    CONCAT(c.nombre, ' ', c.appaterno, ' ', c.apmaterno) as cliente,
    p.nombre as producto,
    d.cantidad,
    d.precioVenta,
    (d.cantidad * d.precioVenta) as subtotal
FROM factura f
INNER JOIN cliente c ON f.fkcliente = c.idcliente
INNER JOIN detalle d ON f.idfactura = d.fkfactura
INNER JOIN producto p ON d.fkproducto = p.idproducto;

-- ============================================
-- PROCEDIMIENTOS ALMACENADOS (STORED PROCEDURES)
-- ============================================

DELIMITER //

-- Procedimiento para procesar una venta completa
CREATE PROCEDURE sp_procesar_venta(
    IN p_idcliente INT,
    IN p_idproducto INT,
    IN p_cantidad INT,
    IN p_precioVenta DECIMAL(10,2),
    OUT p_idfactura INT,
    OUT p_mensaje VARCHAR(255)
)
BEGIN
    DECLARE v_stock_actual INT;
    DECLARE v_error BOOLEAN DEFAULT FALSE;
    
    -- Manejador de errores
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION 
    BEGIN
        SET v_error = TRUE;
        ROLLBACK;
    END;
    
    START TRANSACTION;
    
    -- Verificar stock disponible
    SELECT stock INTO v_stock_actual 
    FROM producto 
    WHERE idproducto = p_idproducto;
    
    IF v_stock_actual IS NULL THEN
        SET p_mensaje = 'Producto no encontrado';
        SET p_idfactura = -1;
        ROLLBACK;
    ELSEIF v_stock_actual < p_cantidad THEN
        SET p_mensaje = 'Stock insuficiente';
        SET p_idfactura = -1;
        ROLLBACK;
    ELSE
        -- Crear factura
        INSERT INTO factura (fechaFactura, fkcliente) 
        VALUES (CURDATE(), p_idcliente);
        
        SET p_idfactura = LAST_INSERT_ID();
        
        -- Insertar detalle
        INSERT INTO detalle (fkfactura, fkproducto, cantidad, precioVenta)
        VALUES (p_idfactura, p_idproducto, p_cantidad, p_precioVenta);
        
        -- Actualizar stock
        UPDATE producto 
        SET stock = stock - p_cantidad 
        WHERE idproducto = p_idproducto;
        
        IF v_error THEN
            SET p_mensaje = 'Error al procesar la venta';
            SET p_idfactura = -1;
        ELSE
            SET p_mensaje = 'Venta procesada exitosamente';
            COMMIT;
        END IF;
    END IF;
END//

-- Procedimiento para obtener estadísticas del dashboard
CREATE PROCEDURE sp_obtener_estadisticas_dashboard(
    OUT p_total_productos INT,
    OUT p_total_clientes INT,
    OUT p_ventas_hoy INT,
    OUT p_monto_hoy DECIMAL(10,2)
)
BEGIN
    -- Total de productos
    SELECT COUNT(*) INTO p_total_productos FROM producto;
    
    -- Total de clientes
    SELECT COUNT(*) INTO p_total_clientes FROM cliente;
    
    -- Ventas de hoy
    SELECT COUNT(DISTINCT f.idfactura) INTO p_ventas_hoy
    FROM factura f
    WHERE f.fechaFactura = CURDATE();
    
    -- Monto total de hoy
    SELECT COALESCE(SUM(d.cantidad * d.precioVenta), 0) INTO p_monto_hoy
    FROM factura f
    INNER JOIN detalle d ON f.idfactura = d.fkfactura
    WHERE f.fechaFactura = CURDATE();
END//

-- Procedimiento para obtener productos con bajo stock
CREATE PROCEDURE sp_productos_bajo_stock(
    IN p_stock_minimo INT
)
BEGIN
    SELECT 
        idproducto,
        nombre,
        precioProducto,
        stock,
        CASE 
            WHEN stock = 0 THEN 'SIN STOCK'
            WHEN stock < p_stock_minimo THEN 'BAJO STOCK'
            ELSE 'STOCK OK'
        END as estado
    FROM producto
    WHERE stock <= p_stock_minimo
    ORDER BY stock ASC;
END//

DELIMITER ;

-- ============================================
-- TRIGGERS (DISPARADORES)
-- ============================================

-- Trigger para validar stock antes de insertar en detalle
DELIMITER //

CREATE TRIGGER tr_validar_stock_antes_venta
BEFORE INSERT ON detalle
FOR EACH ROW
BEGIN
    DECLARE v_stock_actual INT;
    
    -- Obtener stock actual
    SELECT stock INTO v_stock_actual 
    FROM producto 
    WHERE idproducto = NEW.fkproducto;
    
    -- Validar que hay suficiente stock
    IF v_stock_actual < NEW.cantidad THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Stock insuficiente para realizar la venta';
    END IF;
    
    -- Validar que la cantidad sea positiva
    IF NEW.cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad debe ser mayor a cero';
    END IF;
    
    -- Validar que el precio sea positivo
    IF NEW.precioVenta <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El precio de venta debe ser mayor a cero';
    END IF;
END//

-- Trigger para actualizar stock automáticamente después de una venta
CREATE TRIGGER tr_actualizar_stock_despues_venta
AFTER INSERT ON detalle
FOR EACH ROW
BEGIN
    UPDATE producto 
    SET stock = stock - NEW.cantidad 
    WHERE idproducto = NEW.fkproducto;
END//

-- Trigger para registrar auditoría de cambios en productos
CREATE TRIGGER tr_auditoria_producto_update
AFTER UPDATE ON producto
FOR EACH ROW
BEGIN
    -- Aquí podrías insertar en una tabla de auditoría si la crearas
    -- Por ahora solo validamos cambios negativos en stock
    IF NEW.stock < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El stock no puede ser negativo';
    END IF;
END//

DELIMITER ;

-- ============================================
-- CONSULTAS DE PRUEBA PARA LAS VISTAS
-- ============================================

-- Ver resumen de ventas por producto
SELECT * FROM vista_ventas_producto;

-- Ver clientes con sus compras
SELECT * FROM vista_clientes_compras;

-- Ver facturas completas
SELECT * FROM vista_facturas_completas;