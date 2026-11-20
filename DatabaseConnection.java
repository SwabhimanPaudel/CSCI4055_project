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

    /** XAMPP defaults; the Settings dialog in the window can change them. */
    public static Properties defaultSettings() {
        Properties profile = new Properties();
        profile.setProperty("DB_HOST", "localhost");
        profile.setProperty("DB_PORT", "3306");
        profile.setProperty("DB_USER", "root");
        profile.setProperty("DB_PASSWORD", "");
        return profile;
    }

    public static Connection getConnection(String databaseName, Properties profile)
            throws SQLException {
        if (databaseName == null ||
            !databaseName.matches("[A-Za-z0-9_]+")) {

            throw new SQLException("Use letters, numbers, and underscores for the database name.");
        }

        String url = "jdbc:mariadb://" + profile.getProperty("DB_HOST", "localhost")
                + ":" + profile.getProperty("DB_PORT", "3306") + "/" + databaseName;
        try {
            DriverManager.getDriver(url);
        } catch (SQLException error) {
            throw new SQLException("The JDBC driver is missing. Add lib/mariadb-java-client-3.5.11.jar to the classpath.",
                    "IM002", error);
        }
        return DriverManager.getConnection(url,
                profile.getProperty("DB_USER", "root"), profile.getProperty("DB_PASSWORD", ""));
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
