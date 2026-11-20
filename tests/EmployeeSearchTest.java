import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Integration checks using synthetic data on the temporary server from the runner. */
public class EmployeeSearchTest {
    private static int passed;
    private static final List<String> NONE = Collections.emptyList();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Run tests/run-search-tests.py with a JDBC driver JAR.");
        }
        String database = "employee_search_test_" + Long.toUnsignedString(System.nanoTime());
        String password = System.getenv("DB_PASSWORD");
        if (password == null) {
            password = "";
        }
        try (Connection connection = DriverManager.getConnection(args[0], "root", password);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + database);
            try {
                connection.setCatalog(database);
                try (Statement fixture = connection.createStatement()) {
                    createFixture(fixture);
                }
                check(connection, "no selections", NONE, false, NONE, false,
                        "Alice A Adams", "Alice A Adams", "Bob Brown", "Cara Clark",
                        "Dan Davis", "Eve Evans", "Fay Foster", "Gus Green");
                check(connection, "empty selections ignore Not", NONE, true, NONE, true,
                        "Alice A Adams", "Alice A Adams", "Bob Brown", "Cara Clark",
                        "Dan Davis", "Eve Evans", "Fay Foster", "Gus Green");
                check(connection, "one department", names("Research"), false, NONE, false,
                        "Alice A Adams", "Bob Brown");
                check(connection, "multiple departments", names("Research", "Headquarters"),
                        false, NONE, false, "Alice A Adams", "Alice A Adams", "Bob Brown",
                        "Cara Clark", "Dan Davis");
                check(connection, "Department Not includes unassigned employees",
                        names("Research"), true, NONE, false, "Alice A Adams", "Cara Clark",
                        "Dan Davis", "Eve Evans", "Fay Foster", "Gus Green");
                check(connection, "one project gives one row per employee", NONE, false,
                        names("ProductX"), false, "Alice A Adams", "Cara Clark", "Gus Green");
                check(connection, "multiple projects match any", NONE, false,
                        names("ProductX", "ProductY"), false, "Alice A Adams", "Alice A Adams",
                        "Bob Brown", "Cara Clark", "Gus Green");
                check(connection, "Project Not excludes mixed assignments", NONE, false,
                        names("ProductX"), true, "Alice A Adams", "Bob Brown", "Dan Davis",
                        "Eve Evans", "Fay Foster");
                check(connection, "multiple projects with Not", NONE, false,
                        names("ProductX", "ProductY"), true, "Dan Davis", "Eve Evans", "Fay Foster");
                check(connection, "department AND project", names("Research"), false,
                        names("ProductX"), false, "Alice A Adams");
                check(connection, "Department Not AND project", names("Research"), true,
                        names("ProductX"), false, "Cara Clark", "Gus Green");
                check(connection, "department AND Project Not", names("Research"), false,
                        names("ProductX"), true, "Bob Brown");
                check(connection, "both Not checkboxes", names("Research"), true,
                        names("ProductX"), true, "Alice A Adams", "Dan Davis", "Eve Evans", "Fay Foster");
                check(connection, "multiple selections with both Not checkboxes",
                        names("Research", "Headquarters"), true,
                        names("ProductX", "ProductY"), true, "Eve Evans", "Fay Foster");
                check(connection, "quoted department name", names("Director's Office"), false,
                        NONE, false, "Eve Evans", "Gus Green");
                check(connection, "quoted project name", NONE, false,
                        names("Owner's Report"), false, "Cara Clark", "Eve Evans");
                check(connection, "no matches", NONE, false, names("Missing project"), false);
                check(connection, "department is a parameter", names("Research') OR 1=1 --"),
                        false, NONE, false);
                check(connection, "project is a parameter", NONE, false,
                        names("ProductX') OR 1=1 --"), false);
                if (connection.isClosed()) {
                    throw new AssertionError("Search closed the connection owned by Fill.");
                }
                passed++;
                int guiChecks = IntegratedFrameTest.run(connection, database);
                System.out.println("Passed " + guiChecks + " integrated GUI checks.");
            } finally {
                connection.setCatalog("mysql");
                try (Statement cleanup = connection.createStatement()) {
                    cleanup.executeUpdate("DROP DATABASE " + database);
                }
            }
        }
        Connection closed = DriverManager.getConnection(args[0], "root", password);
        closed.close();
        try {
            EmployeeSearch.search(closed, NONE, false, NONE, false);
            throw new AssertionError("A closed connection should report SQLException.");
        } catch (SQLException expected) {
            passed++;
        }
        System.out.println("Passed " + passed + " employee search checks.");
    }

    private static List<String> names(String... values) {
        return Arrays.asList(values);
    }

    private static void check(Connection connection, String description,
            List<String> departments, boolean notDepartments,
            List<String> projects, boolean notProjects, String... expected) throws SQLException {
        List<String> actual = EmployeeSearch.search(connection, departments,
                notDepartments, projects, notProjects);
        if (!actual.equals(Arrays.asList(expected))) {
            throw new AssertionError(description + ": expected "
                    + Arrays.toString(expected) + ", received " + actual);
        }
        passed++;
    }

    private static void createFixture(Statement statement) throws SQLException {
        statement.executeUpdate("CREATE TABLE department (Dnumber INT PRIMARY KEY, Dname VARCHAR(60))");
        statement.executeUpdate("CREATE TABLE EMPLOYEE (Ssn CHAR(9) PRIMARY KEY, Fname VARCHAR(30), "
                + "Minit CHAR(1), Lname VARCHAR(30), Dno INT)");
        statement.executeUpdate("CREATE TABLE PROJECT (Pnumber INT PRIMARY KEY, Pname VARCHAR(60))");
        statement.executeUpdate("CREATE TABLE WORKS_ON (Essn CHAR(9), Pno INT, "
                + "PRIMARY KEY (Essn, Pno))");
        statement.executeUpdate("INSERT INTO department VALUES (1, 'Research'), (2, 'Headquarters'), "
                + "(3, 'Director''s Office')");
        statement.executeUpdate("INSERT INTO PROJECT VALUES (1, 'ProductX'), (2, 'ProductY'), "
                + "(3, 'Owner''s Report')");
        statement.executeUpdate("INSERT INTO EMPLOYEE VALUES "
                + "('000000001', 'Alice', 'A', 'Adams', 1), "
                + "('000000002', 'Bob', NULL, 'Brown', 1), "
                + "('000000003', 'Cara', '', 'Clark', 2), "
                + "('000000004', 'Dan', NULL, 'Davis', 2), "
                + "('000000005', 'Eve', NULL, 'Evans', 3), "
                + "('000000006', 'Fay', NULL, 'Foster', NULL), "
                + "('000000007', 'Gus', NULL, 'Green', 3), "
                + "('000000008', 'Alice', 'A', 'Adams', 2)");
        statement.executeUpdate("INSERT INTO WORKS_ON VALUES ('000000001', 1), ('000000001', 2), "
                + "('000000002', 2), ('000000003', 1), ('000000003', 3), ('000000005', 3), "
                + "('000000007', 1), ('000000007', 2), ('000000008', 2)");
    }
}
