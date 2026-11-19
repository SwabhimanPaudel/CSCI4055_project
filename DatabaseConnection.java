import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3307/";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection(String databaseName)
            throws SQLException {

        if (databaseName == null ||
            !databaseName.matches("[A-Za-z0-9_]+")) {

            throw new SQLException("Invalid database name!!");
        }

        return DriverManager.getConnection(
                URL + databaseName,
                USER,
                PASSWORD
        );
    }
}