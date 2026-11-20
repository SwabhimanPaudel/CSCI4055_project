# Employee Search

CSCI 4055 group project using Java Swing and the COMPANY database schema.

**Group members:** Swabhiman Paudel, Yukta Karki, Kierra Wines

The program loads the departments and projects from a database that uses the
COMPANY schema, then searches for employees who are in, or not in, the selected
departments and/or projects.

## Project files

| File | Purpose |
|---|---|
| `EmployeeSearchFrame.java` | Swing window (main class), built from the WindowBuilder template |
| `EmployeeSearch.java` | Builds and runs the employee search query |
| `DatabaseConnection.java` | Opens the JDBC connection and checks the COMPANY tables |
| `buildCOMPANYdb.sql` | Script that creates and fills the COMPANY tables |
| `lib/mariadb-java-client-3.5.11.jar` | MariaDB Connector/J JDBC driver |
| `run.sh` / `run.bat` | Compile and run from the command line |
| `.project` / `.classpath` | Eclipse project files |

## 1. Create the database

1. Start **MySQL/MariaDB** in the XAMPP control panel.
2. Open phpMyAdmin (`http://localhost/phpmyadmin`), click **New**, and create a
   database named `COMPANY`.
3. Select `COMPANY`, open the **Import** tab, choose `buildCOMPANYdb.sql`, and click
   **Import**.

Or from a terminal (add `-P 3307` etc. if XAMPP uses a different port):

```bash
mysql -u root -p -e "CREATE DATABASE COMPANY"
mysql -u root -p COMPANY < buildCOMPANYdb.sql
```

Another database name works too, as long as it uses the same schema.

## 2. Run the program

Requires a JDK (Java 8 or later). The JDBC driver is included in `lib`.

**Eclipse:** choose *File → Import → General → Existing Projects into Workspace*,
select this folder, then run `EmployeeSearchFrame`. The `.classpath` file already
adds `lib/mariadb-java-client-3.5.11.jar` to the build path. If you set the project
up by hand instead, add that jar under *Project → Properties → Java Build Path →
Libraries → Add JARs*.

**Mac/Linux terminal:**

```bash
sh run.sh
```

**Windows Command Prompt:**

```bat
run.bat
```

The scripts compile the three Java files into `build` and open the window. If the
driver jar is missing, they download it from Maven Central first.

## 3. Database login settings

The program uses the XAMPP defaults: host `localhost`, port `3306`, user `root`,
and an empty password. If your XAMPP uses a different port or a password, click
**Settings** (or just click **Fill**: the same dialog opens automatically when the
login fails), enter your values, and click OK. They are kept until the window is
closed and are never saved to a file.

## 4. Using the program

1. Type the database name (for example `COMPANY`) and click **Fill**. The
   Department and Project lists are reloaded from that database. If the database
   cannot be opened, a popup says so.
2. Select one or more departments and/or projects. Use Ctrl-click (Windows) or
   Command-click (Mac) to select several items, or Shift-click to select a range.
3. Tick **Not** under a list to search for employees *not* in the selected
   departments or *not* working on the selected projects.
4. Click **Search**. Matching employees appear in the Employee list.
5. Click **Clear** to empty the Employee list, clear all selections, and turn off
   both Not checkboxes.

Search rules:

- Several items selected in one list match an employee in **any** of them.
- Department and Project filters are combined with **AND**.
- Project Not excludes an employee who works on any selected project, even if
  they also work on other projects.
- A list with nothing selected is not used as a filter, and its Not checkbox
  is ignored. With no selections at all, Search shows every employee.
- The Department, Project, and Employee lists all scroll.

Example with the textbook data: Department = Research, Project = ProductX, and
Project **Not** ticked returns Ramesh K Narayan and Franklin T Wong.

## Tools used

- **Eclipse IDE with WindowBuilder:** the window started from Dr. Smith's
  WindowBuilder template (absolute layout). We added the scroll panes, the Not
  checkboxes' behavior, and a **Settings** button for the database login. The
  Settings button is not in the assignment mockup: we added it so the program
  works with XAMPP installs that use a password or a port other than 3306.
- **Java Swing** for the user interface.
- **JDBC** with **MariaDB Connector/J 3.5.11** (it also works with MySQL servers).
- **XAMPP** (MariaDB) as the database server, with **phpMyAdmin** to load the data.
- **Git/GitHub** for version control.

Tested with JDK 17 and XAMPP 8.3.37 (MariaDB 10.11.19) on macOS.
