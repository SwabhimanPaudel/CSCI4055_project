# Employee Search

CSCI 4055 group project using Java Swing and the COMPANY database schema.

## Run

Install a JDK (Java 8 or later), start the database server in XAMPP, and load the
class COMPANY database. Another database name works if it uses the same schema.

From the repository folder:

**Mac/Linux**

```bash
sh run.sh
```

**Windows (Command Prompt)**

```bat
run.bat
```

The first run downloads MariaDB Connector/J 3.5.11 into `lib`, compiles the three
Java files into `build`, and opens the window. Internet access is needed for the
first driver download. The database itself is not included in the repository.

## Database settings

The defaults are `localhost`, port `3306`, user `root`, and an empty password.
If your XAMPP settings are different, copy `database.properties.example` to
`database.properties` and edit the values. The local file is ignored by Git.
Environment variables with the same names override the file.

```properties
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
```

Use the port shown in XAMPP. Our local test installation uses `3307`.

## Use

Enter the database name and click **Fill**. Select departments and/or projects,
then click **Search**. Use Command-click on Mac or Ctrl-click on Windows to select
separate items. The **Not** checkbox excludes the selected departments or projects.
For Project Not, employees working on any selected project are excluded even if
they also work on another project.

- Multiple items match any selected item in that list.
- Department and project filters combine with AND.
- With no selections, Search shows all employees.
- Clear empties the results, clears selections, and turns off both Not checkboxes.

Fill replaces the loaded lists. An invalid database name or incompatible schema
shows an error popup. All three list/result areas are scrollable.

## Tools

Java Swing with the professor's Eclipse WindowBuilder starter layout. Database
access uses JDBC and MariaDB Connector/J. Tested with JDK 17 and XAMPP 8.3.37
(MariaDB 10.11.19) on macOS.
