
@echo off
REM ============================================================
REM  Coolbox - Lanzador robusto para Windows
REM  - Soporta rutas con espacios y evita el error de escape en javac
REM  - Usa render por software (compatible con Windows 10 y 11)
REM  - Soporta JDK 17 portable incluido en el proyecto (jdk-portable)
REM  - Si no hay portable, busca C:\Program Files\Java\jdk-17
REM ============================================================
 
cd /d "%~dp0"

REM --- 0) Usar JDK portable incluido en el proyecto (prioridad) ---
set "PORTABLE_JAVA=%~dp0jdk-portable"
if exist "%PORTABLE_JAVA%\bin\java.exe" (
    set "JAVA_HOME=%PORTABLE_JAVA%"
) else (
    set "CUSTOM_JAVA=C:\Program Files\Java\jdk-17"
    if exist "%CUSTOM_JAVA%\bin\java.exe" (
        set "JAVA_HOME=%CUSTOM_JAVA%"
    )
)
 
REM --- 1) Detectar JAVA ---
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
) else (
    set "JAVA_EXE=java"
    set "JAVAC_EXE=javac"
)
 
if not exist "%JAVAC_EXE%" (
    where javac >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] No se encontro javac. Necesitas el JDK, no solo el JRE.
        echo [ERROR] Si estas en Windows 10, asegurate de que la carpeta jdk-portable este presente.
        pause
        exit /b 1
    )
    set "JAVAC_EXE=javac"
)

REM --- 1b) Informar version de Java detectada (util para soporte Win10/Win11) ---
echo [INFO] Usando Java:
"%JAVA_EXE%" -version 2>&1
if errorlevel 1 (
    echo [ERROR] No se pudo ejecutar java. Verifica la instalacion.
    pause
    exit /b 1
)
 
REM --- 2) Cargar .env si existe ---
if exist ".env" (
    echo [INFO] Cargando .env ...
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        if not "%%a"=="" set "%%a=%%b"
    )
) else (
    echo [WARN] No se encontro .env.
)
 
REM --- 3) Compilar (Soportando rutas con espacios en javac) ---
if exist "bin" rmdir /s /q "bin"
mkdir bin
 
echo [INFO] Buscando archivos fuente...
if exist sources.txt del sources.txt
setlocal enabledelayedexpansion
for /f "delims=" %%i in ('dir /s /b src\*.java') do (
    set "FILE=%%i"
    REM Cambiamos barras invertidas por normales para que javac no las borre
    set "FILE=!FILE:\=/!"
    echo "!FILE!" >> sources.txt
)
endlocal
 
echo [INFO] Compilando ...
"%JAVAC_EXE%" -d bin -cp "lib\*" -encoding UTF-8 @sources.txt
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion. Revisa la sintaxis de tu codigo.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt
 
REM --- 4) Copiar recursos ---
echo [INFO] Copiando recursos ...
if exist src\coolbox\sistema\Vistas xcopy /E /I /Y /Q src\coolbox\sistema\Vistas bin\coolbox\sistema\Vistas >nul
if exist src\coolbox\sistema\css     xcopy /E /I /Y /Q src\coolbox\sistema\css     bin\coolbox\sistema\css     >nul
if exist src\coolbox\sistema\fonts   xcopy /E /I /Y /Q src\coolbox\sistema\fonts   bin\coolbox\sistema\fonts   >nul
 
REM --- 5) Lanzar (intentar D3D/escritorio nativo; sw solo como ultimo recurso) ---
echo [INFO] Iniciando Coolbox ...
"%JAVA_EXE%" -Dprism.order=sw -Dprism.text=t2k -Dprism.lcdtext=false --module-path "lib" --add-modules javafx.controls,javafx.fxml -Djava.library.path="lib\bin" -cp "lib\*;bin" coolbox.sistema.Main
 
pause
 
