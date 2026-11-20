/**
 * Author: Lon Smith, Ph.D.
 * Description: This is the framework for the database program. Additional requirements and functionality
 *    are to be built by you and your group.
 */

import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	/**
	 * Fill calls this after successfully opening the database and loading both
	 * lists. Pass null before replacing or closing that connection. Fill owns it.
	 */
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

	private void fillDatabase() {
		clearResults();
		department.clear();
		project.clear();
		Connection pending = null;
		try {
			closeDatabase();
			String databaseName = txtDatabase.getText().trim();
			if (databaseName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please enter a database name.",
						"Missing Database", JOptionPane.ERROR_MESSAGE);
				return;
			}
			pending = DatabaseConnection.getConnection(databaseName);
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
			pending = null; // The frame now owns this connection until refill/close.
			JOptionPane.showMessageDialog(this, "Departments and projects loaded successfully!");
		} catch (SQLException error) {
			JOptionPane.showMessageDialog(this,
					"Database could not be opened:\n" + error.getMessage(),
					"Database Error", JOptionPane.ERROR_MESSAGE);
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
		setBounds(100, 100, 450, 347);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Database:");
		lblNewLabel.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblNewLabel.setBounds(21, 23, 59, 14);
		contentPane.add(lblNewLabel);
		
		txtDatabase = new JTextField();
		txtDatabase.setBounds(90, 20, 193, 20);
		contentPane.add(txtDatabase);
		txtDatabase.setColumns(10);
		lblNewLabel.setLabelFor(txtDatabase);
		
		JButton btnDBFill = new JButton("Fill");
		/**
		 * The btnDBFill should fill the department and project JList with the 
		 * departments and projects from your entered database name.
		 */
		btnDBFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fillDatabase();
			}
		});
		
		btnDBFill.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnDBFill.setBounds(307, 19, 68, 23);
		contentPane.add(btnDBFill);
		
		JLabel lblDepartment = new JLabel("Department");
		lblDepartment.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblDepartment.setBounds(52, 63, 89, 14);
		contentPane.add(lblDepartment);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblProject.setBounds(255, 63, 47, 14);
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
		scrollProject.setBounds(225, 84, 150, 42);
		contentPane.add(scrollProject);
		
		chckbxNotDept = new JCheckBox("Not");
		chckbxNotDept.getAccessibleContext().setAccessibleName("Not in selected departments");
		chckbxNotDept.setBounds(71, 133, 59, 23);
		contentPane.add(chckbxNotDept);
		
		chckbxNotProject = new JCheckBox("Not");
		chckbxNotProject.getAccessibleContext().setAccessibleName("Not on selected projects");
		chckbxNotProject.setBounds(270, 133, 59, 23);
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
		scrollDepartment.setBounds(36, 84, 172, 40);
		contentPane.add(scrollDepartment);

		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblEmployee.setBounds(52, 179, 89, 14);
		contentPane.add(lblEmployee);
		
		JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchEmployees(chckbxNotDept.isSelected(), chckbxNotProject.isSelected());
			}
		});
		btnSearch.setBounds(80, 276, 89, 23);
		contentPane.add(btnSearch);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearResults();
			}
		});
		btnClear.setBounds(236, 276, 89, 23);
		contentPane.add(btnClear);
		
		textAreaEmployee = new JTextArea();
		textAreaEmployee.setEditable(false);
		lblEmployee.setLabelFor(textAreaEmployee);

		//Make Employee scrollable
		JScrollPane scrollEmployee = new JScrollPane(textAreaEmployee);
		scrollEmployee.setBounds(36, 197, 339, 68);
		contentPane.add(scrollEmployee);
	}
}
