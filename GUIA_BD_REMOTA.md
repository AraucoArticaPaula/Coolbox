# 🌐 Guía: acceso remoto a CoolboxDB (SQL Server)

Esta guía deja `CoolboxDB` accesible desde otras PCs de la red LAN. La idea:

```
[ PC Servidor]                              [ PC Cliente]
SQL Server en :1433   ◄──── LAN/Wi-Fi ────  Java + Coolbox
192.168.1.50                                (lee .env con DB_HOST=192.168.1.50)
```

Todo se hace **una sola vez en la PC servidor**. Los clientes solo configuran su `.env`.

---

## 0) Averigua la IP LAN de la PC servidor

En la PC donde corre SQL Server, abrí CMD y ejecutá:

```cmd
ipconfig
```

Buscá el adaptador **Ethernet** o **Wi-Fi** y anotá la **Dirección IPv4**, por ejemplo `192.168.1.50`. Esa IP va a ir en el `.env` de los clientes.

---

## 1) Habilitar TCP/IP en SQL Server Configuration Manager

Por defecto SQL Server Express no escucha por red. Hay que activar TCP/IP.

1. Abrí **SQL Server Configuration Manager** (buscalo en el menú Inicio).
2. Expansión: **SQL Server Network Configuration** → **Protocols for SQLEXPRESS** (o el nombre de tu instancia).
3. Click derecho en **TCP/IP** → **Enable**.
4. Doble click en **TCP/IP** → pestaña **IP Addresses**:
   - Bajá hasta **IPAll** y dejá **TCP Port = 1433** (borrá el 0 de TCP Dynamic Ports si está).
   - En **IP1 / IP2 / IP4** (la 127.0.0.1 y la IP LAN): asegurá **Enabled = Yes**.
5. Click **OK** y reiniciá el servicio **SQL Server** desde `services.msc`.

> Sin esto, ningún cliente podrá conectarse aunque abras el firewall.

---

## 2) Crear un login SQL dedicado (NO uses `sa`)

En SSMS, conectate a tu instancia y ejecutá:

```sql
USE master;
GO

CREATE LOGIN coolbox_app
    WITH PASSWORD = 'CambiaEsto123!',
         DEFAULT_DATABASE = CoolboxDB,
         CHECK_POLICY = OFF;   -- solo desarrollo; en prod dejá ON
GO

USE CoolboxDB;
GO

CREATE USER coolbox_app FOR LOGIN coolbox_app;
GO

ALTER ROLE db_owner ADD MEMBER coolbox_app;
-- db_owner es lo más simple para un proyecto académico.
-- En producción: db_datareader + db_datawriter + EXECUTE en los stored procedures.
GO
```

Anotá el usuario y contraseña — los vas a poner en el `.env` de cada cliente.

---

## 3) Abrir el puerto 1433 en el Firewall de Windows

En la **PC servidor**, abrí PowerShell **como administrador** y pegá:

```powershell
New-NetFirewallRule -DisplayName "SQL Server 1433" `
    -Direction Inbound `
    -Protocol TCP `
    -LocalPort 1433 `
    -Action Allow `
    -Profile Any
```

> Si tu instancia es **SQLEXPRESS** y usás el SQL Browser, también abrí UDP 1434.

### Forma rápida (CMD como admin)

```cmd
netsh advfirewall firewall add rule name="SQL Server 1433" dir=in action=allow protocol=TCP localport=1433
```

---

## 4) Verificar que la PC servidor está escuchando

En la **PC servidor**, CMD:

```cmd
netstat -an | findstr 1433
```

Deberías ver una línea `0.0.0.0:1433 ... LISTENING`. Si solo aparece `127.0.0.1:1433`, TCP/IP no quedó bien habilitado — volvé al paso 1.

---

## 5) Probar conexión desde otra PC

Desde la **PC cliente** (cualquier otra de la red), en un CMD:

```cmd
Test-NetConnection 192.168.1.50 -Port 1433
```

Resultado esperado: `TcpTestSucceeded: True`.

Si falla:
- ¿Las dos PCs están en la misma red/Wi-Fi?
- ¿El firewall corporativo o el antivirus de la servidor está bloqueando?
- ¿La IP que anotaste cambió? Volvé a correr `ipconfig` (DHCP suele renovarla).

---

## 6) Configurar el `.env` en cada PC cliente

En la carpeta del proyecto, copiá `.env.example` a `.env` y editalo:

```ini
DB_HOST=192.168.1.50     # IP LAN de tu PC servidor
DB_PORT=1433
DB_NAME=CoolboxDB
DB_USER=coolbox_app
DB_PASSWORD=CambiaEsto123!
DB_ENCRYPT=false
```

> ⚠️ **Importante**: si la IP del servidor cambia (porque tu router usa DHCP), los clientes se rompen. Para evitarlo, asigná una IP fija en el router (reservá la MAC) o configurá IP estática en la PC servidor.

---

## 7) Arrancar la app

Doble click en `run.bat` (o `bash run.sh` en Linux/Mac). La consola va a mostrar:

```
[INFO] URL BD: jdbc:sqlserver://192.168.1.50:1433;databaseName=CoolboxDB;
[INFO] Iniciando Coolbox ...
```

Si ves `[ERROR] La aplicacion finalizo con codigo ...` lo más común es:
1. IP mal escrita o servidor apagado.
2. Firewall del servidor bloqueando.
3. `coolbox_app` no creado o sin permisos sobre `CoolboxDB`.

---

## 🆘 Comprobación rápida de SQL desde la PC cliente (sin Java)

En la PC cliente, abrí SSMS e intentá conectarte a `192.168.1.50,1433` con autenticación SQL y el usuario `coolbox_app`. Si SSMS entra, Java también va a entrar.

---

## 📋 Checklist final

- [ ] TCP/IP habilitado y puerto 1433 fijo
- [ ] Servicio de SQL Server reiniciado
- [ ] Login `coolbox_app` creado y mapeado a `CoolboxDB`
- [ ] Firewall abierto para TCP 1433
- [ ] `netstat` muestra `0.0.0.0:1433 LISTENING`
- [ ] `Test-NetConnection` desde otra PC da `True`
- [ ] `.env` de cada cliente apunta a la IP del servidor
