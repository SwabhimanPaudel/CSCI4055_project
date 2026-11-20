import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Searches a COMPANY-schema database using the names displayed in the lists. */
public final class EmployeeSearch {

    private EmployeeSearch() {
    }

    /** Apply both active filters and leave the database connection open. */
    public static List<String> search(Connection connection,
            List<String> departments, boolean notDepartments,
            List<String> projects, boolean notProjects) throws SQLException {
        Objects.requireNonNull(connection, "A database connection is required.");
        Objects.requireNonNull(departments, "Department selections are required.");
        Objects.requireNonNull(projects, "Project selections are required.");

        Map<String, String> tables = DatabaseConnection.companyTables(connection);
        StringBuilder sql = new StringBuilder("SELECT e.Fname, e.Minit, e.Lname FROM ")
                .append(tables.get("EMPLOYEE")).append(" e WHERE 1 = 1");
        List<String> parameters = new ArrayList<String>();

        if (!departments.isEmpty()) {
            sql.append(notDepartments ? " AND NOT EXISTS (" : " AND EXISTS (");
            sql.append("SELECT 1 FROM ").append(tables.get("DEPARTMENT"))
                    .append(" d WHERE d.Dnumber = e.Dno")
                    .append(" AND d.Dname IN (")
                    .append(placeholders(departments.size())).append("))");
            parameters.addAll(departments);
        }

        if (!projects.isEmpty()) {
            // Project Not excludes anyone assigned to any selected project.
            sql.append(notProjects ? " AND NOT EXISTS (" : " AND EXISTS (");
            sql.append("SELECT 1 FROM ").append(tables.get("WORKS_ON")).append(" w")
                    .append(" JOIN ").append(tables.get("PROJECT"))
                    .append(" p ON p.Pnumber = w.Pno")
                    .append(" WHERE w.Essn = e.Ssn AND p.Pname IN (")
                    .append(placeholders(projects.size())).append("))");
            parameters.addAll(projects);
        }

        sql.append(" ORDER BY e.Lname, e.Fname, e.Ssn");
        List<String> employees = new ArrayList<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setQueryTimeout(10);
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    List<String> parts = new ArrayList<String>();
                    addNamePart(parts, results.getString("Fname"));
                    addNamePart(parts, results.getString("Minit"));
                    addNamePart(parts, results.getString("Lname"));
                    employees.add(String.join(" ", parts));
                }
            }
        }
        return employees;
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static void addNamePart(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value.trim());
        }
    }
}
