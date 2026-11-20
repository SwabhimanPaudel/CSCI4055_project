import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection(String databaseName)
            throws SQLException {

        if (databaseName == null ||
            !databaseName.matches("[A-Za-z0-9_]+")) {

            throw new SQLException("Use letters, numbers, and underscores for the database name.");
        }

        Properties local = readSettings();
        String port = setting(local, "DB_PORT", "3306");
        try {
            int number = Integer.parseInt(port);
            if (number < 1 || number > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException error) {
            throw new SQLException("DB_PORT must be a number between 1 and 65535.");
        }
        String url = "jdbc:mariadb://" + setting(local, "DB_HOST", "localhost")
                + ":" + port + "/" + databaseName;
        Properties settings = new Properties();
        settings.setProperty("user", setting(local, "DB_USER", "root"));
        settings.setProperty("password", setting(local, "DB_PASSWORD", ""));
        settings.setProperty("connectTimeout", "5000");
        settings.setProperty("socketTimeout", "15000");
        String sslMode = setting(local, "DB_SSL_MODE", "");
        if (!sslMode.isEmpty()) {
            settings.setProperty("sslMode", sslMode);
        }
        return DriverManager.getConnection(url, settings);
    }

    private static Properties readSettings() throws SQLException {
        Properties local = new Properties();
        Path file = Paths.get("database.properties");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                local.load(reader);
            } catch (IOException | IllegalArgumentException error) {
                throw new SQLException("Could not read database.properties.", error);
            }
        }
        return local;
    }

    private static String setting(Properties local, String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? local.getProperty(name, fallback) : value;
    }

    /** Check the COMPANY schema and handle differences in table capitalization. */
    public static Map<String, String> companyTables(Connection connection) throws SQLException {
        String[][] required = {
            {"DEPARTMENT", "Dnumber", "Dname"},
            {"EMPLOYEE", "Ssn", "Fname", "Minit", "Lname", "Dno"},
            {"PROJECT", "Pnumber", "Pname"},
            {"WORKS_ON", "Essn", "Pno"}
        };
        String database = connection.getCatalog();
        if (database == null || database.isEmpty()) {
            throw new SQLException("Select a database before loading COMPANY tables.");
        }
        DatabaseMetaData metadata = connection.getMetaData();
        Map<String, String> actualNames = new LinkedHashMap<String, String>();
        try (ResultSet tables = metadata.getTables(database, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                actualNames.put(name.toUpperCase(Locale.ROOT), name);
            }
        }
        Map<String, String> quotedNames = new LinkedHashMap<String, String>();
        String quote = metadata.getIdentifierQuoteString().trim();
        for (String[] table : required) {
            String actualName = actualNames.get(table[0]);
            if (actualName == null) {
                throw new SQLException("This database is missing the COMPANY table " + table[0] + ".");
            }
            Set<String> columns = new HashSet<String>();
            try (ResultSet results = metadata.getColumns(database, null, actualName, "%")) {
                while (results.next()) {
                    if (actualName.equals(results.getString("TABLE_NAME"))) {
                        columns.add(results.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
                    }
                }
            }
            for (int i = 1; i < table.length; i++) {
                if (!columns.contains(table[i].toUpperCase(Locale.ROOT))) {
                    throw new SQLException("This database is missing " + actualName + "." + table[i] + ".");
                }
            }
            quotedNames.put(table[0], quote + actualName + quote);
        }
        return quotedNames;
    }
}
