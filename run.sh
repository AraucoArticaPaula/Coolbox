#!/usr/bin/env bash
# ============================================================
#  Coolbox - lanzador para Linux / macOS
#  Uso:  ./run.sh
# ============================================================
set -e

cd "$(dirname "$0")"

# 1) Java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_EXE="java"
else
    echo "[ERROR] Java no encontrado. Instala Java 17+ o define JAVA_HOME."
    exit 1
fi

# 2) .env
if [ -f ".env" ]; then
    echo "[INFO] Cargando .env ..."
    set -a
    . ./.env
    set +a
else
    echo "[WARN] No se encontro .env; usando variables de entorno del sistema."
fi

# 3) Compilar
mkdir -p bin
echo "[INFO] Compilando ..."
mapfile -t SOURCES < <(find src -name "*.java")
JAVAC_EXE="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
[ -z "$JAVAC_EXE" ] && JAVAC_EXE="$(command -v javac || true)"
if [ -z "$JAVAC_EXE" ]; then
    echo "[ERROR] javac no encontrado. Necesitas el JDK, no solo el JRE."
    exit 1
fi
"$JAVAC_EXE" -d bin -cp "lib/*" "${SOURCES[@]}"

# 4) Recursos
echo "[INFO] Copiando recursos ..."
[ -d src/coolbox/sistema/Vistas ] && cp -r src/coolbox/sistema/Vistas bin/coolbox/sistema/
[ -d src/coolbox/sistema/css     ] && cp -r src/coolbox/sistema/css     bin/coolbox/sistema/

# 5) Lanzar
echo "[INFO] Iniciando Coolbox ..."
echo "[INFO] URL BD: jdbc:sqlserver://${DB_HOST:-localhost}:${DB_PORT:-1433};databaseName=${DB_NAME:-CoolboxDB};"
"$JAVA_EXE" --module-path lib --add-modules javafx.controls,javafx.fxml \
   -Djava.library.path=lib/bin -cp "lib/*:bin" coolbox.sistema.Main
