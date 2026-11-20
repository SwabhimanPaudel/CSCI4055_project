# Employee Search

CSCI 4055 group project: a Java Swing desktop application that loads departments
and projects from a database with the COMPANY schema, then searches employees.
The integrated application combines the database-fill, employee-search, and
ui-clear-scroll branches.

## Database setup

Use the database server provided by XAMPP if required for class. Start its database
module and import the instructor's COMPANY schema/data using phpMyAdmin or the
database client. A standalone MySQL server with the same schema also works.
The application opens the database name entered in the window; it is not fixed
to `COMPANY`. Each teammate can use their own local database.

The app verifies the tables and columns it needs:

| Table | Required columns |
| --- | --- |
| DEPARTMENT | Dnumber, Dname |
| EMPLOYEE | Ssn, Fname, Minit, Lname, Dno |
| PROJECT | Pnumber, Pname |
| WORKS_ON | Essn, Pno |

Table names are resolved using database metadata, including lowercase or mixed
capitalization. This repository does not contain a database dump.

## JDBC driver

From this repository's folder, download MariaDB Connector/J 3.5.11:

```bash
mkdir -p lib
curl -fL https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar \
  -o lib/mariadb-java-client-3.5.11.jar
```

This driver supports both MariaDB and MySQL through standard JDBC.
[MariaDB Connector/J documentation](https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j)
The downloaded JAR is ignored by Git.

## Connection settings

Set these environment variables in the Terminal used to launch Java:

| Variable | Default |
| --- | --- |
| DB_HOST | localhost |
| DB_PORT | 3307, matching the database-fill branch |
| DB_USER | root |
| DB_PASSWORD | empty |
| DB_SSL_MODE | driver's default; optional |

Use the port shown in your database server's configuration. For example, if
XAMPP uses port 3306, run `export DB_PORT=3306` before launching the app.

For a password-protected database, enter the password privately in macOS zsh:

```bash
read -s 'DB_PASSWORD?Database password: '
export DB_PASSWORD
printf '\n'
```

For the standalone MySQL already installed on this Mac, use `DB_PORT=3306`.
Its locally generated TLS certificate can be used with `DB_SSL_MODE=trust`:

```bash
export DB_PORT=3306
export DB_SSL_MODE=trust
```

Set your own database password using the private prompt above. Passwords are
read from the environment, not stored in Java source or included in the JDBC URL.

## Compile and run

Use a JDK, Java 8 or later. Run these commands from this repository's folder:

```bash
javac -d . DatabaseConnection.java EmployeeSearch.java EmployeeSearchFrame.java
java -cp '.:lib/*' EmployeeSearchFrame
```

On Windows, use `java -cp ".;lib/*" EmployeeSearchFrame` for the launch command.

Enter the database name and click **Fill**. Select departments and/or projects;
use Command-click on Mac or Ctrl-click on Windows for separate selections. Each
**Not** checkbox reverses its own filter. Click **Search** to display employees.
**Clear** empties employee results, clears both selections, and turns off both
Not checkboxes while keeping the loaded department/project choices.

Fill replaces both lists and retains the connection for Search. Another Fill
closes the previous connection. Failed Fill clears stale data and displays an
error popup. Closing the window closes its connection. All three lists/results
areas are scrollable; employee results are read-only.

## Search assumptions to confirm with Dr. Smith

- Multiple selections match **any** selected item within that list.
- Active department and project filters combine with **AND**.
- No selections returns all employees. Not on an empty filter has no effect.
- Project Not excludes anyone working on any selected project, even if they also
  work on another project. Employees without projects satisfy Project Not.
- Department Not includes employees outside all selected departments, including
  employees without a department.

The assignment page does not specify the first three rules, deadline, or
submission format. Different employees with the same name remain separate result
lines; matching several projects does not duplicate an employee.

## Tools and validation

GUI: Java Swing using the professor's Eclipse WindowBuilder starter layout.
Integration changes were edited directly in Java source. Database access: JDBC
with MariaDB Connector/J. The code compiles for Java 8 and was tested with JDK 17.

Local validation passed 21 query checks and 26 integrated Swing GUI checks against
a separate MySQL 9.5 instance with synthetic data and password authentication.
Coverage includes all filter combinations, multiple selections, Clear, repeated
Fill, switching database names, table capitalization, invalid/blank names,
incomplete schemas, no matches, scrolling setup, and connection cleanup.
XAMPP itself was not installed on the test Mac; verify the configured database
port and imported class data when running the final app in XAMPP.
