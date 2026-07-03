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
| 💰 Operaciones | Registro de ventas, cálculo automático de comisiones, cuadre de caja |
| 👥 Personal | Registro de empleados, asignación de cargos, horarios, validación de horas |
| 📊 Reportes | Indicadores de rendimiento por empleado y tienda |

---

## 🛠️ Tecnologías

- **Java 17** (JDK portable incluido en el repo, no requiere instalación aparte)
- **JavaFX 21** (interfaz gráfica)
- **Microsoft SQL Server** (base de datos)
- **JDBC** con driver `mssql-jdbc` para la conexión
- **Patrón MVC** (Modelo - Vista - Controlador)
- **Scene Builder** para el diseño de vistas `.fxml`
- Configuración por variables de entorno (`.env`)

---

## 🗂️ Estructura del proyecto

```
Coolbox-master/
├── src/coolbox/sistema/
│   ├── Conexion/
│   │   └── ConexionDB.java          # Conexión JDBC (lee credenciales desde .env)
│   ├── Controladores/
│   │   ├── LoginController.java
│   │   ├── AlmacenController.java
│   │   ├── OperacionesController.java
│   │   ├── PersonalController.java
│   │   ├── ReportesController.java
│   │   ├── SeguridadController.java
│   │   ├── SesionUsuario.java        # Sesión activa del usuario
│   │   └── Modales/                  # Controladores de ventanas emergentes
│   ├── Modelos/                      # POJOs de las entidades (Empleado, Venta, Tienda, etc.)
│   ├── Vistas/
│   │   ├── Login.fxml
│   │   ├── Almacen.fxml
│   │   ├── Operaciones.fxml
│   │   ├── Personal.fxml
│   │   ├── Reportes.fxml
│   │   ├── Seguridad.fxml
│   │   └── Modales/                  # Ventanas emergentes (.fxml)
│   ├── css/
│   └── fonts/                        # Fuentes personalizadas (Tw Cen MT, Font Awesome)
├── lib/                               # JavaFX SDK + drivers (mssql-jdbc, etc.)
├── jdk-portable/                      # JDK 17 portable, no requiere instalar Java
├── bin/                               # Salida de compilación (generado por run.bat)
├── run.bat                            # Lanzador para Windows
├── run.sh                             # Lanzador para Linux/Mac
├── .env.example                       # Plantilla de variables de entorno
└── .env                                # Tus credenciales locales (NO se sube al repo)
```

---

## 🗃️ Base de datos

- Motor: **Microsoft SQL Server**
- Esquema con tablas normalizadas (Tiendas, Empleados, Usuarios, Roles, Productos, Inventario, Ventas, Comisiones, Horarios, Cuadre de caja, Movimientos de inventario, Traslados, Preguntas de seguridad, etc.)
- Esquema de seguridad **RBAC** (Roles → Permisos)

> El script de creación de la base de datos se gestiona por separado; consulta con el equipo si necesitas la última versión del `.sql`.

---

## ⚙️ Requisitos previos

- Windows 10/11 (el proyecto incluye JDK portable, no necesitas instalar Java aparte)
- Microsoft SQL Server (local o remoto — ver `GUIA_BD_REMOTA.md` para conexión remota)
- Git con soporte **Git LFS** (el repo incluye binarios pesados: JDK portable y librerías nativas de JavaFX)

---

## 🚀 Configuración e instalación

**1. Clonar el repositorio**

Asegúrate de tener [Git LFS](https://git-lfs.github.com/) instalado antes de clonar, ya que el JDK portable y las DLLs de JavaFX se manejan con LFS:

```bash
git lfs install
git clone https://github.com/AraucoArticaPaula/Coolbox.git
```

**2. Crear la base de datos**

Ejecuta el script SQL correspondiente en SQL Server Management Studio (SSMS) para crear el esquema `CoolboxDB`.

**3. Configurar la conexión**

Copia `.env.example` a `.env` y completa tus credenciales:

```bash
copy .env.example .env
```

```env
DB_HOST=localhost
DB_PORT=1433
DB_NAME=CoolboxDB
DB_USER=coolbox_app
DB_PASSWORD=TuContraseñaAqui
DB_ENCRYPT=false
```

> Para conexión a un servidor SQL en otra PC de la red, consulta `GUIA_BD_REMOTA.md`.

**4. Ejecutar**

En Windows, simplemente corre:

```bash
run.bat
```

Este script:
- Detecta y usa el JDK portable incluido en `jdk-portable/`
- Carga las variables de entorno desde `.env`
- Compila el proyecto en `bin/`
- Copia los recursos (Vistas, CSS, fuentes)
- Lanza la aplicación

En Linux/Mac usa `run.sh` (requiere Java 17+ instalado en el sistema).

---

## 👤 Primer acceso

Al ejecutar el sistema por primera vez, crea un usuario administrador directamente en la base de datos usando el procedimiento almacenado correspondiente, o consulta con el equipo el usuario de pruebas ya configurado.

---

## 📝 Notas

- El proyecto usa codificación **UTF-8 sin BOM** en todos los archivos `.fxml` y `.java` — respeta esto al editar para evitar problemas de tildes/caracteres especiales.
- No subas tu archivo `.env` al repositorio (ya está en `.gitignore`).
