# Employee search branch

`EmployeeSearch.java` contains the JDBC queries. The Search button passes the
selected department/project names and both Not flags to that class. Results show
each matching employee once, even when several project assignments match.

## Search rules to confirm with Dr. Smith

These are working assumptions; the assignment does not settle them:

- Multiple departments or projects match **any** selected item in that list.
- When both lists have selections, their filters combine with **AND**.
- No selections returns all employees. A Not checkbox on an empty filter has no
  effect.

Department Not includes employees outside all selected departments, including
employees without a department. Project Not excludes anyone working on a selected
project, even if they also work on another project; employees without projects
satisfy Project Not. Queries use `EXISTS` / `NOT EXISTS` and prepared parameters.

## Connection handoff for the Fill teammate

The starter Fill handler still inserts sample strings. Until the database-fill
branch supplies a real connection, Search will display "Open a database with Fill
before searching." This branch does not implement Fill or Clear.

When implementing Fill in `EmployeeSearchFrame.java`:

1. Call `setSearchConnection(null)` before replacing the old database connection.
2. Open a JDBC connection to the database named in the Database field.
3. Replace the two list models with the exact `Dname` and `Pname` strings from
   that connection. Do not prefix the strings with numbers or other formatting.
4. After both lists load successfully, call `setSearchConnection(connection)`
   using that open connection. On failure, keep the search connection null.

Fill owns connection cleanup. Search closes only its statements and result sets.
Queries use the connection's active database and have a ten-second timeout.

The columns below were verified from the local `COMPANY` database. The database
name is not hardcoded in Search; another database with the same schema works.
Table capitalization matters on installations with case-sensitive table names.

| Table | Columns used by Search |
| --- | --- |
| `department` | `Dnumber`, `Dname` |
| `EMPLOYEE` | `Ssn`, `Fname`, `Minit`, `Lname`, `Dno` |
| `PROJECT` | `Pnumber`, `Pname` |
| `WORKS_ON` | `Essn`, `Pno` |

## Validation

Both Java source files compile with Java 8 or later. Local integration tests check
21 cases against a separate MySQL instance with synthetic records. Final GUI
testing remains necessary after integrating the Fill and UI branches. Include a
MySQL JDBC driver on the runtime classpath when adding the database connection.

## GUI tools

The window comes from the professor's Java Swing template, originally built with
Eclipse WindowBuilder. This branch edits Java source directly and uses Swing's
existing controls; it does not introduce another GUI builder.
