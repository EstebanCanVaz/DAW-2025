create database dbpos;

use dbpos;

create table producto (
idproducto int auto_increment not null primary key,
nombre varchar(100),
precioProducto decimal (5,2),
stock int
);

select producto.idproducto,producto.nombre,producto.precioProducto, producto.stock from producto;

insert into producto(nombre,precioProducto,stock) values ("Lapiz", 6.5, 50);

UPDATE producto set producto.nombre="libreta", producto.precioProducto=40, producto.stock=20 where producto.idproducto=2;

delete from producto where producto.idproducto=2;

select * from producto WHERE producto.nombre like concat('%',"t",'%');

create table cliente (
idcliente int auto_increment not null primary key,
nombre varchar(100),
appaterno varchar(100),
apmaterno varchar(100)
);

select cliente.idcliente,cliente.nombre,cliente.appaterno,cliente.apmaterno from cliente;

insert into cliente (nombre,appaterno,apmaterno) values ("Esteban", "Canto", "Vazquez");

update cliente set cliente.nombre="Edgar", cliente.appaterno="Aguilar", cliente.apmaterno="Moguel" where cliente.idcliente=2;

delete from cliente where cliente.idcliente=2;

select * from cliente WHERE cliente.nombre like concat('%',"F",'%');

create table factura (
idfactura int auto_increment not null primary key,
fechaFactura date,
fkcliente int, 
foreign key (fkcliente) references cliente(idcliente)
);

INSERT INTO factura (fechaFactura,fkcliente)values(curdate(),5);
select*from factura;

create table detalle (
iddetalle int auto_increment not null primary key,
fkfactura int,
foreign key (fkfactura) references factura(idfactura),
fkproducto int,
foreign key (fkproducto) references producto(idproducto),
cantidad int,
precioVenta decimal(5,2)
);

INSERT INTO detalle (fkfactura,fkproducto,cantidad,precioVenta) values((SELECT MAX(idfactura) from factura),?,?,?);
UPDATE producto SET producto.stock = stock - ? WHERE idproducto =?;

select * from detalle;

SELECT MAX(idfactura) as UltimaFactura from factura;

select factura.idfactura, factura.fechaFactura, cliente.nombre, cliente.appaterno, cliente.apmaterno from factura
INNER JOIN cliente ON cliente.idcliente = factura.fkcliente WHERE factura.idfactura = 5;

select producto.nombre, detalle.cantidad, detalle.precioVenta from detalle
INNER JOIN factura ON factura.idfactura = detalle.fkfactura
INNER JOIN producto ON producto.idproducto = detalle.fkproducto
where factura.idfactura = 7;

select factura.idfactura, factura.fechaFactura, producto.nombre, detalle.cantidad, detalle.precioVenta from detalle
INNER JOIN factura ON factura.idfactura = detalle.fkfactura
INNER JOIN producto ON producto.idproducto = detalle.fkproducto
where factura.fechaFactura Between ? AND ?;

select * from factura;