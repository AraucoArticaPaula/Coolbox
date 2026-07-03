package coolbox.sistema.Conexion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Conexión JDBC a SQL Server.
 *
 * Orden de resolución de cada variable:
 *   1) Variable de entorno del sistema (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_ENCRYPT)
 *   2) Archivo .env en la raíz del proyecto (KEY=VALUE, líneas con # como comentario)
 *   3) Valor por defecto (solo para host/puerto/nombre/encriptación)
 *
 * Las credenciales (DB_USER, DB_PASSWORD) NO tienen valor por defecto: si no están
 * definidas en el entorno o en .env, la conexión falla con un mensaje claro.
 *
 * El archivo .env NO se sube al repo. Está en .gitignore.
 * Hay un .env.example en la raíz como plantilla.
 */
public class ConexionDB {

    private static final String DEFAULT_HOST     = "localhost";
    private static final String DEFAULT_PORT     = "1433";
    private static final String DEFAULT_DB       = "CoolboxDB";
    private static final String DEFAULT_ENCRYPT  = "false";

    private static final Map<String, String> CONFIG = cargarConfig();

    private static String url() {
        String host    = get("DB_HOST",    DEFAULT_HOST);
        String port    = get("DB_PORT",    DEFAULT_PORT);
        String db      = get("DB_NAME",    DEFAULT_DB);
        String encrypt = get("DB_ENCRYPT", DEFAULT_ENCRYPT);
        // trustServerCertificate=true para entornos de desarrollo sin certificados válidos
        return "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + db
                + ";encrypt=" + encrypt
                + ";trustServerCertificate=true;";
    }

    private static String get(String key, String def) {
        String v = CONFIG.get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    /** Devuelve una conexión nueva. Cada llamada abre una conexión; cerrar con try-with-resources. */
    public static Connection getConnection() throws SQLException {
        String user = CONFIG.get("DB_USER");
        String pass = CONFIG.get("DB_PASSWORD");
        if (user == null || user.isBlank() || pass == null) {
            throw new SQLException(
                "Faltan credenciales de BD. Define DB_USER y DB_PASSWORD " +
                "en variables de entorno o en el archivo .env (ver .env.example).");
        }
        return DriverManager.getConnection(url(), user, pass);
    }

    /** Carga .env desde la raíz del proyecto y del directorio de trabajo, y completa con env del sistema. */
    private static Map<String, String> cargarConfig() {
        Map<String, String> cfg = new HashMap<>();

        // 1) Variables de entorno del sistema (tienen prioridad máxima)
        for (String k : new String[]{"DB_HOST","DB_PORT","DB_NAME","DB_USER","DB_PASSWORD","DB_ENCRYPT"}) {
            String v = System.getenv(k);
            if (v != null && !v.isBlank()) cfg.put(k, v);
        }

        // 2) .env en la raíz del proyecto (subiendo desde el classpath hasta encontrar uno)
        Path envPath = localizarEnv();
        if (envPath != null && Files.exists(envPath)) {
            try (var lines = Files.lines(envPath)) {
                lines.forEach(line -> {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#")) return;
                    int eq = s.indexOf('=');
                    if (eq < 0) return;
                    String k = s.substring(0, eq).trim();
                    String v = s.substring(eq + 1).trim();
                    // quitar comillas si las trae
                    if ((v.startsWith("\"") && v.endsWith("\"")) ||
                        (v.startsWith("'")  && v.endsWith("'"))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    cfg.putIfAbsent(k, v); // el entorno del sistema gana
                });
            } catch (IOException e) {
                System.err.println("[ConexionDB] No se pudo leer .env: " + e.getMessage());
            }
        } else {
            // No es error: el usuario puede estar pasando todo por variables de entorno
            System.out.println("[ConexionDB] .env no encontrado; usando solo variables de entorno.");
        }
        return cfg;
    }

    /** Busca .env en el directorio de trabajo y, si no, junto al classpath (raíz del proyecto). */
    private static Path localizarEnv() {
        Path cwd = Path.of(System.getProperty("user.dir")).resolve(".env");
        if (Files.exists(cwd)) return cwd;

        // Classpath: suele ser la raíz del proyecto en dev
        try (InputStream in = ConexionDB.class.getResourceAsStream("/.env")) {
            // Si está empaquetado en el classpath, lo extraemos a un temporal
            if (in != null) {
                Path tmp = Files.createTempFile("coolbox-env-", ".env");
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return tmp;
            }
        } catch (IOException ignored) {}

        return null;
    }

    // === Diagnóstico: útil para mostrar la URL al usuario (sin password) ===
    public static String getUrlOculta() {
        String u = url();
        return u.replaceAll("(?i)(password=)[^;]*;", "$1***;");
    }
}
