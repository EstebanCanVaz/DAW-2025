# Proyecto Desarrollo de Aplicaciones Web

Este proyecto es desarrollado como parte del curso de Desarrollo de Aplicaciones Web del quinto semestre de la Facultad de Matemáticas.

# Sistema POS - Punto de Venta Web

Sistema de punto de venta desarrollado con Java Servlets, MySQL y JavaScript vanilla.

## Requisitos

- JDK 22
- Maven 3.x
- MySQL 8.0+
- Apache Tomcat 7+

## Instalación

### 1. Base de Datos

```bash
mysql -u root -p < "SQL File 1.sql"
```

### 2. Configuración

Editar `src/main/java/Configuracion/CConexion.java`:

```java
String usuario = "root";
String contrasena = "tu_password";
String bd = "dbpos1";
```

### 3. Ejecutar

```bash
mvn clean package
mvn tomcat7:run
```

Acceder a: `http://localhost:8080/ProyectoPOO/login.html`

## Usuarios de Prueba

| Usuario  | Contraseña  | Rol           |
| -------- | ----------- | ------------- |
| admin    | admin123    | Administrador |
| vendedor | vendedor123 | Vendedor      |

## Funcionalidades

- Gestión de productos (CRUD + búsqueda)
- Gestión de clientes (CRUD + documentos)
- Punto de venta con carrito
- Reportes y facturas con QR
- Sistema de sesiones
- APIs externas (tipo de cambio, códigos QR)

## Estructura del Proyecto

```
src/main/
├── java/
│   ├── Configuracion/    # Conexión a BD
│   ├── Modelo/           # Entidades
│   └── Servlet/          # Controladores REST
└── webapp/
    ├── *.html            # Vistas
    ├── *.js              # Lógica cliente
    ├── styles.css        # Estilos
    └── WEB-INF/
        └── web.xml       # Configuración
```

## Tecnologías

**Backend:** Java Servlets, MySQL  
**Frontend:** HTML5, CSS3, JavaScript ES6+  
**APIs:** Exchange Rate API, QuickChart.io

## Integrantes del Proyecto

| Nombre del Integrante               | Foto                                                                                                                    |
| ----------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Esteban Canto Vázquez               | <img src="assets/Esteban.jpeg" alt="Esteban" width="150" height="150" style="border-radius: 50%; object-fit: cover;">   |
| José Manuel Ceballos Medina         | <img src="assets/Jose.jpeg" alt="José" width="150" height="150" style="border-radius: 50%; object-fit: cover;">         |
| Ángel Leandro Puch Uribe            | <img src="assets/Angel.jpeg" alt="Ángel" width="150" height="150" style="border-radius: 50%; object-fit: cover;">       |
| Mauricio Emiliano Ramírez Ceciliano | <img src="assets/Mauricio.jpeg" alt="Mauricio" width="150" height="150" style="border-radius: 50%; object-fit: cover;"> |
| Becky Zhu Wu                        | <img src="assets/Becky.jpeg" alt="Becky" width="150" height="150" style="border-radius: 50%; object-fit: cover;">       |
