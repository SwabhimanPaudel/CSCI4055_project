/**
 * Author: Lon Smith, Ph.D.
 * Description: This is the framework for the database program. Additional requirements and functionality
 *    are to be built by you and your group.
 */

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

public class EmployeeSearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtDatabase;
	private JList<String> lstDepartment;
	private DefaultListModel<String> department = new DefaultListModel<String>();
	private JList<String> lstProject;
	private DefaultListModel<String> project = new DefaultListModel<String>();
	private JTextArea textAreaEmployee;
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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		
		JButton btnDBFill = new JButton("Fill");
		/**
		 * The btnDBFill should fill the department and project JList with the 
		 * departments and projects from your entered database name.
		 */
		btnDBFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] dept = {"Headquarters", "Reorganization", "Accounting", "HR"};	
				for(int i = 0; i < dept.length; i++) {
					department.addElement(dept[i]);
				}
				String[] prj = {"ProdoctX", "ProductY", "ProductZ"};
				for(int j = 0; j < prj.length; j++) {
					project.addElement(prj[j]);
				}
				
			}
		});
		
		btnDBFill.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnDBFill.setBounds(307, 19, 68, 23);
		contentPane.add(btnDBFill);
		
		JLabel lblDepartment = new JLabel("Department");
		lblDepartment.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblDepartment.setBounds(52, 63, 89, 14);
		contentPane.add(lblDepartment);

		lstDepartment = new JList<String>(department);
		lstDepartment.setFont(new Font("Tahoma", Font.PLAIN, 12));

		lstDepartment.setSelectionMode(
    	ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	
		//Make Department scrollable 
		JScrollPane scrollDepartment = new JScrollPane(lstDepartment);
		scrollDepartment.setBounds(36, 84, 172, 40);
		contentPane.add(scrollDepartment);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblProject.setBounds(255, 63, 47, 14);
		contentPane.add(lblProject);
		
		lstProject = new JList<String>(project);
		lstProject.setFont(new Font("Tahoma", Font.PLAIN, 12));

		lstProject.setSelectionMode(
    	ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		//Make Project scrollable 
		JScrollPane scrollProject = new JScrollPane(lstProject);
		scrollProject.setBounds(225, 84, 150, 42);
		contentPane.add(scrollProject);
		
		JCheckBox chckbxNotDept = new JCheckBox("Not");
		chckbxNotDept.setBounds(71, 133, 59, 23);
		contentPane.add(chckbxNotDept);
		
		JCheckBox chckbxNotProject = new JCheckBox("Not");
		chckbxNotProject.setBounds(270, 133, 59, 23);
		contentPane.add(chckbxNotProject);
		
		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblEmployee.setBounds(52, 179, 89, 14);
		contentPane.add(lblEmployee);
		
		textAreaEmployee = new JTextArea();
		textAreaEmployee.setEditable(false);
		//Make Employee scrollable 
		JScrollPane scrollEmployee = new JScrollPane(textAreaEmployee);
		scrollEmployee.setBounds(36, 197, 339, 68);
		contentPane.add(scrollEmployee);

		JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textAreaEmployee.setText("John Smith\nFranklin Wong\nDan Heng\nAlbedo Krediprinz\nRengoku Kyojuro\nYoimiya Naganohara");
			}
		});
		btnSearch.setBounds(80, 276, 89, 23);
		contentPane.add(btnSearch);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
   		 public void actionPerformed(ActionEvent e) {
        // Clear employee results
        textAreaEmployee.setText("");
        // Clear Department selection
        lstDepartment.clearSelection();
        // Clear Project selection
        lstProject.clearSelection();
        // Turn off Not checkboxes
        chckbxNotDept.setSelected(false);
        chckbxNotProject.setSelected(false);
    	}
		});
		btnClear.setBounds(236, 276, 89, 23);
		contentPane.add(btnClear);
		
		
	}
}