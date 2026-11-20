import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Exercise the actual Fill/Search/Clear buttons using only disposable test databases. */
public final class IntegratedFrameTest {
    private static int checks;

    private static class TrackedFrame extends EmployeeSearchFrame {
        private static final long serialVersionUID = 1L;
        private Connection active;

        @Override
        public void setSearchConnection(Connection connection) {
            super.setSearchConnection(connection);
            active = connection;
        }
    }

    public static int run(Connection fixture, String database) throws Exception {
        String other = database + "_other";
        String incomplete = database + "_incomplete";
        try (Statement statement = fixture.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + other);
            statement.executeUpdate("CREATE DATABASE " + incomplete);
        }
        try {
            try (Statement statement = fixture.createStatement()) {
                statement.executeUpdate("CREATE TABLE " + other + ".DePaRtMeNt LIKE " + database + ".department");
                for (String table : Arrays.asList("EMPLOYEE", "PROJECT", "WORKS_ON")) {
                    statement.executeUpdate("CREATE TABLE " + other + "." + table + " LIKE " + database + "." + table);
                }
                statement.executeUpdate("INSERT INTO " + other + ".DePaRtMeNt VALUES (1, 'Other department')");
                statement.executeUpdate("INSERT INTO " + other + ".PROJECT VALUES (1, 'Other project')");
                statement.executeUpdate("INSERT INTO " + other + ".EMPLOYEE VALUES "
                        + "('000000099', 'Ian', NULL, 'Irwin', 1)");
                statement.executeUpdate("CREATE TABLE " + incomplete + ".DEPARTMENT "
                        + "(Dnumber INT, Dname VARCHAR(60))");
            }
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        exerciseWindow(database, other, incomplete);
                    } catch (SQLException error) {
                        throw new RuntimeException(error);
                    }
                }
            });
            return checks;
        } finally {
            try (Statement statement = fixture.createStatement()) {
                statement.executeUpdate("DROP DATABASE " + other);
                statement.executeUpdate("DROP DATABASE " + incomplete);
            }
        }
    }

    private static void exerciseWindow(String database, String other, String incomplete)
            throws SQLException {
        TrackedFrame frame = new TrackedFrame();
        List<String> messages = new ArrayList<String>();
        Timer dismissDialogs = new Timer(40, event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof Dialog && window.getOwner() == frame && window.isShowing()) {
                    for (Component component : components((Container) window)) {
                        if (component instanceof JOptionPane) {
                            messages.add(String.valueOf(((JOptionPane) component).getMessage()));
                        }
                    }
                    window.dispose();
                }
            }
        });
        dismissDialogs.start();
        try {
            List<Component> controls = components(frame.getContentPane());
            JTextField name = (JTextField) labelled(controls, "Database:");
            JList<?> departments = (JList<?>) labelled(controls, "Department");
            JList<?> projects = (JList<?>) labelled(controls, "Project");
            JTextArea results = (JTextArea) labelled(controls, "Employee");
            JCheckBox notDepartment = checkbox(controls, "Not in selected departments");
            JCheckBox notProject = checkbox(controls, "Not on selected projects");
            AbstractButton fill = button(controls, "Fill");
            AbstractButton search = button(controls, "Search");
            AbstractButton clear = button(controls, "Clear");
            int scrolls = 0;
            for (Component component : controls) {
                if (component instanceof JScrollPane) {
                    scrolls++;
                }
            }
            expect(scrolls == 3, "All three areas must have scroll panes.");
            expect(!results.isEditable(), "Employee results must be read-only.");
            expect(departments.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                    && projects.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
                    "Both lists must support multiple selections.");
            search.doClick();
            expect(last(messages).contains("Open a database"), "Search before Fill needs a popup.");

            name.setText(database);
            fill.doClick();
            expect(frame.active != null && !frame.active.isClosed(), "Fill must retain an open connection.");
            expect(departments.getModel().getSize() == 3 && projects.getModel().getSize() == 3,
                    "Fill must load real department and project rows.");
            Connection first = frame.active;
            search.doClick();
            expect(results.getLineCount() == 8, "No selections must return all employees.");
            select(departments, "Research");
            select(projects, "ProductX");
            search.doClick();
            expect(results.getText().equals("Alice A Adams"), "GUI must combine department AND project.");
            select(departments, "Research", "Headquarters");
            select(projects, "ProductX", "ProductY");
            search.doClick();
            expect(results.getLineCount() == 4, "GUI must pass every selected department and project.");
            departments.clearSelection();
            select(projects, "ProductX");
            notProject.setSelected(true);
            search.doClick();
            expect(results.getText().equals("Alice A Adams\nBob Brown\nDan Davis\nEve Evans\nFay Foster"),
                    "Project Not must exclude employees with mixed project assignments.");
            select(departments, "Research");
            notDepartment.setSelected(true);
            search.doClick();
            expect(results.getText().equals("Alice A Adams\nDan Davis\nEve Evans\nFay Foster"),
                    "Both GUI Not flags must reach Search.");
            clear.doClick();
            expect(results.getText().isEmpty() && departments.isSelectionEmpty() && projects.isSelectionEmpty()
                    && !notDepartment.isSelected() && !notProject.isSelected(), "Clear must reset the whole search.");
            expect(departments.getModel().getSize() == 3 && projects.getModel().getSize() == 3,
                    "Clear must keep the loaded choices.");
            fill.doClick();
            expect(first.isClosed() && frame.active != first, "Repeated Fill must replace and close the old connection.");
            expect(departments.getModel().getSize() == 3 && projects.getModel().getSize() == 3,
                    "Repeated Fill must not append duplicate choices.");

            name.setText(database + "_missing");
            search.doClick();
            expect(results.getLineCount() == 8, "Editing the name alone must keep the currently loaded database.");
            Connection beforeFailure = frame.active;
            fill.doClick();
            expect(last(messages).contains("Database could not be opened"), "An invalid database needs an error popup.");
            expect(beforeFailure.isClosed() && frame.active == null && results.getText().isEmpty()
                    && departments.getModel().getSize() == 0 && projects.getModel().getSize() == 0,
                    "Failed Fill must discard old results, lists, and connection.");
            search.doClick();
            expect(last(messages).contains("Open a database"), "Search must not reuse a failed Fill connection.");
            name.setText(" ");
            fill.doClick();
            expect(last(messages).contains("Please enter a database name"), "Blank names need a popup.");
            name.setText("bad?options");
            fill.doClick();
            expect(last(messages).contains("Database could not be opened"), "Malformed names must be rejected.");
            name.setText(incomplete);
            fill.doClick();
            expect(last(messages).contains("missing") && frame.active == null
                    && departments.getModel().getSize() == 0 && projects.getModel().getSize() == 0,
                    "An incomplete COMPANY schema must not leave partial data.");

            name.setText(other);
            fill.doClick();
            expect(frame.active != null && departments.getModel().getElementAt(0).equals("Other department"),
                    "Fill must accept another database name and mixed table capitalization.");
            search.doClick();
            expect(results.getText().equals("Ian Irwin"), "Search must use the newly loaded database.");
            select(departments, "Other department");
            notDepartment.setSelected(true);
            search.doClick();
            expect(results.getText().equals("No employees found."), "Empty search results need a clear message.");
            Connection finalConnection = frame.active;
            frame.dispose();
            expect(finalConnection.isClosed(), "Closing the window must close its database connection.");
        } finally {
            dismissDialogs.stop();
            frame.dispose();
        }
    }

    private static void expect(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static String last(List<String> messages) {
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1);
    }

    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<Component>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container) {
                result.addAll(components((Container) child));
            }
        }
        return result;
    }

    private static Component labelled(List<Component> controls, String label) {
        for (Component control : controls) {
            if (control instanceof JLabel && label.equals(((JLabel) control).getText())) {
                return ((JLabel) control).getLabelFor();
            }
        }
        throw new AssertionError("Label missing: " + label);
    }

    private static JCheckBox checkbox(List<Component> controls, String accessibleName) {
        for (Component control : controls) {
            if (control instanceof JCheckBox && accessibleName.equals(
                    ((JCheckBox) control).getAccessibleContext().getAccessibleName())) {
                return (JCheckBox) control;
            }
        }
        throw new AssertionError("Checkbox missing: " + accessibleName);
    }

    private static AbstractButton button(List<Component> controls, String text) {
        for (Component control : controls) {
            if (control instanceof AbstractButton && text.equals(((AbstractButton) control).getText())) {
                return (AbstractButton) control;
            }
        }
        throw new AssertionError("Button missing: " + text);
    }

    private static void select(JList<?> list, String... names) {
        list.clearSelection();
        for (String name : names) {
            boolean found = false;
            for (int i = 0; i < list.getModel().getSize(); i++) {
                if (name.equals(list.getModel().getElementAt(i))) {
                    list.addSelectionInterval(i, i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionError("Missing list choice: " + name);
            }
        }
    }
}
