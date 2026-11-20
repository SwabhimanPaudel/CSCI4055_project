import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Test the login dialog against a temporary password-protected database user. */
public class ConnectionDialogTest {
    private static class Frame extends EmployeeSearchFrame {
        private static final long serialVersionUID = 1L;
        Connection active;
        @Override
        public void setSearchConnection(Connection connection) {
            super.setSearchConnection(connection);
            active = connection;
        }
    }

    private static int checks;

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        SwingUtilities.invokeAndWait(() -> {
            try {
                exercise(mode);
            } catch (SQLException error) {
                throw new RuntimeException(error);
            }
        });
        System.out.println(mode + ": " + checks + " login checks passed.");
    }

    private static void exercise(String mode) throws SQLException {
        Frame frame = new Frame();
        int[] dialogs = {0};
        List<String> alerts = new ArrayList<String>();
        Timer answer = new Timer(40, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof Dialog) || window.getOwner() != frame || !window.isShowing()) {
                    continue;
                }
                List<Component> controls = components((Container) window);
                JOptionPane pane = null;
                for (Component component : controls) {
                    if (component instanceof JOptionPane) {
                        pane = (JOptionPane) component;
                        break;
                    }
                }
                if (pane == null) continue;
                Component host = labelled(controls, "Host");
                if (host == null) {
                    alerts.add(String.valueOf(pane.getMessage()));
                    window.dispose();
                    continue;
                }
                dialogs[0]++;
                expect(labelled(controls, "Password") instanceof JPasswordField,
                        "The password input must mask its value.");
                if (mode.equals("cancel")) {
                    pane.setValue(JOptionPane.CANCEL_OPTION);
                    continue;
                }
                ((JTextField) host).setText("127.0.0.1");
                ((JTextField) labelled(controls, "Port")).setText(
                        mode.equals("validate") && dialogs[0] == 1 ? "not-a-port" : "3307");
                ((JTextField) labelled(controls, "Username")).setText(System.getenv("CHECK_USER"));
                ((JPasswordField) labelled(controls, "Password")).setText(
                        mode.equals("retry") && dialogs[0] == 1
                        ? "deliberately-wrong-test-password" : System.getenv("CHECK_PASSWORD"));
                pane.setValue(JOptionPane.OK_OPTION);
            }
        });
        answer.start();
        try {
            List<Component> controls = components(frame.getContentPane());
            JTextField name = (JTextField) labelled(controls, "Database:");
            JList<?> departments = (JList<?>) labelled(controls, "Department");
            JTextArea employees = (JTextArea) labelled(controls, "Employee");
            AbstractButton fill = button(controls, "Fill");
            AbstractButton search = button(controls, "Search");
            name.setText(System.getenv("CHECK_DATABASE"));
            if (mode.equals("settings")) {
                button(controls, "Settings").doClick();
            }
            fill.doClick();
            if (mode.equals("cancel")) {
                expect(dialogs[0] == 1, "Cancel must stop retrying.");
                expect(frame.active == null && departments.getModel().getSize() == 0,
                        "Cancel must leave no active database or stale choices.");
                search.doClick();
                expect(alerts.get(alerts.size() - 1).contains("Open a database"),
                        "Search after cancel must request Fill.");
                return;
            }
            int expected = mode.equals("retry") || mode.equals("validate") ? 2 : 1;
            expect(dialogs[0] == expected, "Expected the connection dialog to recover from " + mode);
            expect(frame.active != null && !frame.active.isClosed(), "Fill must recover and retain its connection.");
            expect(departments.getModel().getSize() == 1, "Fill must load the test account's data.");
            search.doClick();
            expect(employees.getText().equals("Example Employee"), "Search must work after login.");
            Connection previous = frame.active;
            fill.doClick();
            expect(previous.isClosed(), "Refill must still close the previous connection.");
            expect(dialogs[0] == expected, "The accepted login must last for the current window.");
            button(controls, "Settings").doClick();
            expect(previous.isClosed() && frame.active == null && departments.getModel().getSize() == 0,
                    "Changing settings must clear the previous database state.");
            fill.doClick();
            expect(frame.active != null && !frame.active.isClosed(), "Manual Settings must apply to Fill.");
            Connection finalConnection = frame.active;
            frame.dispose();
            expect(finalConnection.isClosed(), "Closing the window must release the logged-in connection.");
        } finally {
            answer.stop();
            frame.dispose();
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<Component>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container) result.addAll(components((Container) child));
        }
        return result;
    }

    private static Component labelled(List<Component> controls, String label) {
        for (Component component : controls) {
            if (component instanceof JLabel && label.equals(((JLabel) component).getText())) {
                return ((JLabel) component).getLabelFor();
            }
        }
        return null;
    }

    private static AbstractButton button(List<Component> controls, String text) {
        for (Component component : controls) {
            if (component instanceof AbstractButton && text.equals(((AbstractButton) component).getText())) {
                return (AbstractButton) component;
            }
        }
        throw new AssertionError("Missing button: " + text);
    }
}
