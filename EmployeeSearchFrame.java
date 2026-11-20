/**
 * Starter framework author: Lon Smith, Ph.D.
 * Group members: Swabhiman Paudel, Yukta Karki, Kierra Wines
 * Course: CSCI 4055 Database Project
 * Description: Swing window that loads departments and projects from a COMPANY-schema
 *    database and searches for employees in, or not in, the selected departments/projects.
 *    Layout started from the WindowBuilder template; the Settings button is our addition.
 */

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class EmployeeSearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtDatabase;
	private JList<String> lstDepartment;
	private DefaultListModel<String> department = new DefaultListModel<String>();
	private JList<String> lstProject;
	private DefaultListModel<String> project = new DefaultListModel<String>();
	private JTextArea textAreaEmployee;
	private JCheckBox chckbxNotDept;
	private JCheckBox chckbxNotProject;
	private Connection searchConnection;
	private Properties connectionSettings;

	/** Use the connection opened by Fill for later searches. */
	public void setSearchConnection(Connection connection) {
		searchConnection = connection;
	}

	private void clearResults() {
		textAreaEmployee.setText("");
		lstDepartment.clearSelection();
		lstProject.clearSelection();
		chckbxNotDept.setSelected(false);
		chckbxNotProject.setSelected(false);
	}

	private void closeDatabase() throws SQLException {
		Connection previous = searchConnection;
		setSearchConnection(null);
		if (previous != null) {
			previous.close();
		}
	}

	@Override
	public void dispose() {
		try {
			closeDatabase();
		} catch (SQLException error) {
			System.err.println("Could not close the database connection.");
		} finally {
			connectionSettings = null;
			super.dispose();
		}
	}

	private List<String> loadNames(Connection connection, String sql, String column)
			throws SQLException {
		List<String> names = new ArrayList<String>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setQueryTimeout(10);
			try (ResultSet results = statement.executeQuery()) {
				while (results.next()) {
					names.add(results.getString(column));
				}
			}
		}
		return names;
	}

	private Properties currentSettings() throws SQLException {
		if (connectionSettings == null) {
			connectionSettings = DatabaseConnection.getSettings();
		}
		return connectionSettings;
	}

	private Properties askConnectionSettings(String message) throws SQLException {
		Properties previous = currentSettings();
		JTextField host = new JTextField(previous.getProperty("DB_HOST"), 16);
		JTextField port = new JTextField(previous.getProperty("DB_PORT"), 6);
		JTextField user = new JTextField(previous.getProperty("DB_USER"), 16);
		JPasswordField password = new JPasswordField(previous.getProperty("DB_PASSWORD"), 16);
		JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));
		String[] labels = {"Host", "Port", "Username", "Password"};
		JTextField[] inputs = {host, port, user, password};
		for (int i = 0; i < labels.length; i++) {
			JLabel label = new JLabel(labels[i]);
			label.setLabelFor(inputs[i]);
			fields.add(label);
			fields.add(inputs[i]);
		}
		JLabel explanation = new JLabel(message);
		JPanel panel = new JPanel(new BorderLayout(0, 12));
		panel.add(explanation, BorderLayout.NORTH);
		panel.add(fields, BorderLayout.CENTER);
		while (true) {
			EventQueue.invokeLater(() -> password.requestFocusInWindow());
			int choice = JOptionPane.showConfirmDialog(this, panel, "Database Connection",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION) {
				password.setText("");
				return null;
			}
			try {
				int number = Integer.parseInt(port.getText().trim());
				if (number < 1 || number > 65535) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException error) {
				explanation.setText("Enter a port number between 1 and 65535.");
				continue;
			}
			if (host.getText().trim().isEmpty() || user.getText().trim().isEmpty()) {
				explanation.setText("Enter the database host and username.");
				continue;
			}
			Properties updated = new Properties();
			updated.putAll(previous);
			updated.setProperty("DB_HOST", host.getText().trim());
			updated.setProperty("DB_PORT", port.getText().trim());
			updated.setProperty("DB_USER", user.getText().trim());
			char[] enteredPassword = password.getPassword();
			updated.setProperty("DB_PASSWORD", new String(enteredPassword));
			java.util.Arrays.fill(enteredPassword, '\0');
			password.setText("");
			return updated;
		}
	}

	private void configureConnection() {
		try {
			Properties updated = askConnectionSettings("Enter your XAMPP server, port, and login.");
			if (updated == null) {
				return;
			}
			closeDatabase();
			clearResults();
			department.clear();
			project.clear();
			connectionSettings = updated;
		} catch (SQLException error) {
			JOptionPane.showMessageDialog(this,
					"Could not change the database connection:\n" + error.getMessage(),
					"Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean needsConnectionSettings(SQLException error) {
		String state = error.getSQLState();
		return (state != null && (state.startsWith("08") || state.equals("28000")))
				|| error.getErrorCode() == 1045 || error.getErrorCode() == 1044;
	}

	private void fillDatabase() {
		clearResults();
		department.clear();
		project.clear();
		try {
			closeDatabase();
			String databaseName = txtDatabase.getText().trim();
			if (databaseName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please enter a database name.",
						"Missing Database", JOptionPane.ERROR_MESSAGE);
				return;
			}
			while (true) {
				Connection pending = null;
				try {
					pending = DatabaseConnection.getConnection(databaseName, currentSettings());
					Map<String, String> tables = DatabaseConnection.companyTables(pending);
					List<String> departments = loadNames(pending,
							"SELECT Dname FROM " + tables.get("DEPARTMENT") + " ORDER BY Dnumber", "Dname");
					List<String> projects = loadNames(pending,
							"SELECT Pname FROM " + tables.get("PROJECT") + " ORDER BY Pnumber", "Pname");
					for (String name : departments) {
						department.addElement(name);
					}
					for (String name : projects) {
						project.addElement(name);
					}
					setSearchConnection(pending);
					pending = null; // Search reuses this connection.
					JOptionPane.showMessageDialog(this, "Departments and projects loaded successfully!");
					return;
				} catch (SQLException error) {
					if (!needsConnectionSettings(error)) {
						throw error;
					}
					Properties updated = askConnectionSettings(
							"Database could not be opened. Check your XAMPP port and login.");
					if (updated == null) {
						return;
					}
					connectionSettings = updated;
				} finally {
					if (pending != null) {
						try {
							pending.close();
						} catch (SQLException error) {
							System.err.println("Could not close the failed database connection.");
						}
					}
				}
			}
		} catch (SQLException error) {
			JOptionPane.showMessageDialog(this,
					"Database could not be opened:\n" + error.getMessage(),
					"Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void searchEmployees(boolean notDepartments, boolean notProjects) {
		textAreaEmployee.setText("");
		try {
			if (searchConnection == null || searchConnection.isClosed()) {
				JOptionPane.showMessageDialog(this,
						"Open a database with Fill before searching.",
						"Database Required", JOptionPane.WARNING_MESSAGE);
				return;
			}
			List<String> employees = EmployeeSearch.search(searchConnection,
					lstDepartment.getSelectedValuesList(), notDepartments,
					lstProject.getSelectedValuesList(), notProjects);
			textAreaEmployee.setText(employees.isEmpty()
					? "No employees found." : String.join("\n", employees));
			textAreaEmployee.setCaretPosition(0);
		} catch (SQLException error) {
			JOptionPane.showMessageDialog(this,
					"Employee search failed:\n" + error.getMessage(),
					"Search Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmployeeSearchFrame frame = new EmployeeSearchFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EmployeeSearchFrame() {
		setTitle("Employee Search");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 580, 450);
		setResizable(false); // Absolute WindowBuilder layout does not stretch.
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Database:");
		lblNewLabel.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblNewLabel.setBounds(25, 23, 70, 14);
		contentPane.add(lblNewLabel);
		
		txtDatabase = new JTextField();
		txtDatabase.setBounds(95, 20, 225, 22);
		contentPane.add(txtDatabase);
		txtDatabase.setColumns(10);
		lblNewLabel.setLabelFor(txtDatabase);

		JButton btnSettings = new JButton("Settings");
		btnSettings.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnSettings.setBounds(330, 19, 95, 24);
		btnSettings.addActionListener(e -> configureConnection());
		contentPane.add(btnSettings);
		
		JButton btnDBFill = new JButton("Fill");
		btnDBFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fillDatabase();
			}
		});
		
		btnDBFill.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnDBFill.setBounds(435, 19, 95, 24);
		contentPane.add(btnDBFill);
		
		JLabel lblDepartment = new JLabel("Department");
		lblDepartment.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblDepartment.setBounds(50, 60, 120, 14);
		contentPane.add(lblDepartment);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblProject.setBounds(310, 60, 120, 14);
		contentPane.add(lblProject);
		
		lstProject = new JList<String>(new DefaultListModel<String>());
		lstProject.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lstProject.setModel(project);
		lblProject.setLabelFor(lstProject);

		//Select multiple projects
		lstProject.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		//Make Project scrollable
		JScrollPane scrollProject = new JScrollPane(lstProject);
		scrollProject.setBounds(300, 78, 230, 90);
		contentPane.add(scrollProject);
		
		chckbxNotDept = new JCheckBox("Not");
		chckbxNotDept.getAccessibleContext().setAccessibleName("Not in selected departments");
		chckbxNotDept.setBounds(120, 175, 70, 23);
		contentPane.add(chckbxNotDept);
		
		chckbxNotProject = new JCheckBox("Not");
		chckbxNotProject.getAccessibleContext().setAccessibleName("Not on selected projects");
		chckbxNotProject.setBounds(380, 175, 70, 23);
		contentPane.add(chckbxNotProject);
		
		lstDepartment = new JList<String>(new DefaultListModel<String>());
		lblDepartment.setLabelFor(lstDepartment);
		lstDepartment.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lstDepartment.setModel(department);
		
		//Select multiple departments
		lstDepartment.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		//Make Department scrollable
		JScrollPane scrollDepartment = new JScrollPane(lstDepartment);
		scrollDepartment.setBounds(40, 78, 230, 90);
		contentPane.add(scrollDepartment);

		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblEmployee.setBounds(50, 210, 120, 14);
		contentPane.add(lblEmployee);
		
		JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchEmployees(chckbxNotDept.isSelected(), chckbxNotProject.isSelected());
			}
		});
		btnSearch.setBounds(130, 372, 100, 26);
		contentPane.add(btnSearch);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearResults();
			}
		});
		btnClear.setBounds(340, 372, 100, 26);
		contentPane.add(btnClear);
		
		textAreaEmployee = new JTextArea();
		textAreaEmployee.setEditable(false);
		lblEmployee.setLabelFor(textAreaEmployee);

		//Make Employee scrollable
		JScrollPane scrollEmployee = new JScrollPane(textAreaEmployee);
		scrollEmployee.setBounds(40, 228, 490, 132);
		contentPane.add(scrollEmployee);
	}
}
