# 🛒 Coolbox — Sistema de Gestión de Tiendas

Sistema de escritorio desarrollado en **Java + JavaFX** con base de datos **Microsoft SQL Server**, como proyecto académico para la asignatura de Base de Datos II.

Toma como caso de estudio a la empresa peruana **Coolbox (Rash Perú S.R.L.)**, cadena de tiendas de tecnología con presencia nacional.

---

## 📋 Descripción

El sistema centraliza en una sola plataforma cinco procesos operativos críticos que en la empresa real se gestionan con hasta 4 sistemas independientes:

| Módulo | Funcionalidad |
|---|---|
| 🔐 Seguridad | Login, roles (RBAC), recuperación de contraseña, preguntas de seguridad |
| 📦 Almacén | Inventario por tienda, movimientos de stock, traslados entre sucursales |
| 💰 Operaciones | Registro de ventas, cálculo automático de comisiones (Llena la Bolsa + Garantías), cuadre de caja |
| 👥 Personal | Registro de empleados, asignación de cargos, horarios, validación de horas (FT/PT) |
| 📊 Reportes | Indicadores de rendimiento por empleado y tienda |

---

## 🛠️ Tecnologías

- **Java 17+** con **JavaFX** (interfaz gráfica)
- **Microsoft SQL Server 2022** (base de datos)
- **JDBC** con driver `mssql-jdbc` para la conexión
- **Patrón MVC** (Modelo - Vista - Controlador)
- **Scene Builder** para el diseño de vistas `.fxml`
- **Visual Studio Code** como entorno de desarrollo

---

## 🗂️ Estructura del proyecto

```
src/coolbox/sistema/
├── Conexion/
│   └── ConexionDB.java          # Singleton de conexión JDBC
├── Controladores/
│   ├── LoginController.java
│   ├── AlmacenController.java
│   ├── OperacionesController.java
│   ├── PersonalController.java
│   ├── ReportesController.java
│   ├── SeguridadController.java
│   ├── SesionUsuario.java       # Sesión activa del usuario
│   └── Modales/                 # Controladores de ventanas emergentes
├── Modelos/                     # POJOs de las entidades
├── Vistas/
│   ├── Login.fxml
│   ├── Almacen.fxml
│   ├── Operaciones.fxml
│   ├── Personal.fxml
│   ├── Reportes.fxml
│   ├── Seguridad.fxml
│   └── Modales/                 # Ventanas emergentes (.fxml)
└── css/
```

---

## 🗃️ Base de datos

El script SQL está en la raíz del repositorio: `dbcoolbox.sql`

Incluye:
- **16 tablas** normalizadas hasta 3FN
- **13 procedimientos almacenados** transaccionales (`sp_RegistrarVenta`, `sp_RealizarTraslado`, etc.)
- **Triggers** de auditoría de inventario
- **Índices no agrupados** en columnas de búsqueda frecuente
- **Esquema de seguridad RBAC** (Roles → Permisos)
- Modelo de recuperación `FULL` para consistencia ante fallos

### Tablas principales

`TIENDAS` · `EMPLEADOS` · `USUARIOS` · `ROLES` · `PERMISOS` · `PRODUCTOS` · `CATEGORIAS` · `INVENTARIO` · `VENTAS` · `DETALLE_VENTA` · `COMISIONES` · `HORARIOS` · `CUADRE_CAJA` · `MOVIMIENTOS_INVENTARIO` · `TRASLADOS` · `PREGUNTAS_SEGURIDAD`

---

## ⚙️ Requisitos previos

- Java 17 o superior
- JavaFX SDK 17+
- Microsoft SQL Server 2022 (o Express)
- Driver JDBC: `mssql-jdbc-12.x.x.jre11.jar`

---

## 🚀 Configuración e instalación

**1. Clonar el repositorio**
```bash
git clone https://github.com/tu-usuario/coolbox.git
```

**2. Crear la base de datos**

Ejecutar el script en SQL Server Management Studio (SSMS):
```sql
-- Ejecutar en orden:
dbcoolbox.sql
dbcoolbox_preguntas_seguridad.sql   -- tabla de recuperación de contraseña
```

**3. Configurar la conexión**

Editar `ConexionDB.java` con tus credenciales:
```java
String url = "jdbc:sqlserver://localhost:1433;databaseName=CoolboxDB;encrypt=false";
String user = "tu_usuario";
String password = "tu_contraseña";
```

**4. Agregar el driver JDBC al classpath**

En VS Code, añadir el `.jar` de `mssql-jdbc` en `.vscode/settings.json` o en el `classpath` del proyecto.

**5. Ejecutar**

Correr la clase principal del proyecto desde VS Code o tu IDE favorito.

---

## 👤 Primer acceso

Al ejecutar el sistema por primera vez, crear un usuario administrador directamente en la BD:
```sql
EXEC sp_CrearUsuario
  @id_empleado   = 1,
  @nombre_usuario = 'admin',
  @contrasena    = 'tu_clave',
  @correo        = 'admin@coolbox.pe';
```

---
