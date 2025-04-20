package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class PatientTable extends JPanel {
    private DefaultTableModel tableModel;
    private JTable patientTable;
    private final Doctor doctor;
    private Consumer<Object[]> selectionAction;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JButton refreshButton;

    public PatientTable(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Danh sách bệnh nhân",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));

        // Top Panel: Search and Refresh
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Search Field
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setToolTipText("Tìm kiếm theo tên, triệu chứng hoặc chẩn đoán");
        topPanel.add(searchField);

        // Refresh Button
        refreshButton = new JButton("Làm mới");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 12));
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadPatients());
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        // Table Setup
        String[] columns = {"ID", "Họ tên", "Giới tính", "Ngày sinh", "Tuổi", "Khoa", "Triệu chứng", "Chẩn đoán"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rowSorter = new TableRowSorter<>(tableModel);
        patientTable = new JTable(tableModel);
        patientTable.setRowSorter(rowSorter);
        patientTable.setRowHeight(30);
        patientTable.setFont(new Font("Arial", Font.PLAIN, 14));
        patientTable.setShowGrid(true);
        patientTable.setGridColor(new Color(200, 200, 200));
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientTable.setAutoCreateRowSorter(true);
        patientTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Column Widths
        setColumnWidths();

        // Alternating row colors
        patientTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        // Search Functionality
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTable(); }

            private void filterTable() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        // Context Menu
        JPopupMenu contextMenu = createContextMenu();

        // Mouse Listener for Context Menu
        patientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = patientTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < patientTable.getRowCount()) {
                        patientTable.setRowSelectionInterval(row, row);
                        contextMenu.show(patientTable, e.getX(), e.getY());
                    }
                }
            }
        });

        patientTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionAction != null) {
                int selectedRow = patientTable.getSelectedRow();
                if (selectedRow != -1) {
                    triggerSelectionAction(selectedRow);
                }
            }
        });

        loadPatients();
        JScrollPane tableScrollPane = new JScrollPane(patientTable);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    // Set Column Widths
    private void setColumnWidths() {
        patientTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        patientTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        patientTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        patientTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        patientTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        patientTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        patientTable.getColumnModel().getColumn(6).setPreferredWidth(200);
        patientTable.getColumnModel().getColumn(7).setPreferredWidth(200);
    }

    // Context Menu
    private JPopupMenu createContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem viewItem = new JMenuItem("Xem chi tiết");
        viewItem.addActionListener(e -> {
            int selectedRow = patientTable.getSelectedRow();
            if (selectedRow != -1) {
                JOptionPane.showMessageDialog(this, "Thông tin bệnh nhân: " + tableModel.getValueAt(selectedRow, 1));
            }
        });
        contextMenu.add(viewItem);

        JMenuItem editItem = new JMenuItem("Sửa");
        editItem.addActionListener(e -> triggerSelectionAction(patientTable.getSelectedRow()));
        contextMenu.add(editItem);

        JMenuItem deleteItem = new JMenuItem("Xóa");
        deleteItem.addActionListener(e -> {
            int selectedRow = patientTable.getSelectedRow();
            if (selectedRow != -1) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa bệnh nhân này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    PatientPanel panel = (PatientPanel) SwingUtilities.getAncestorOfClass(PatientPanel.class, this);
                    if (panel != null) panel.deletePatient();
                }
            }
        });
        contextMenu.add(deleteItem);

        return contextMenu;
    }

    // Trigger Selection Action
    private void triggerSelectionAction(int selectedRow) {
        if (selectedRow != -1 && selectionAction != null) {
            String dobText = (String) tableModel.getValueAt(selectedRow, 3);
            LocalDate dateOfBirth = dobText != null && !dobText.isEmpty() ? LocalDate.parse(dobText, dateFormatter) : null;
            Object[] rowData = new Object[]{
                    tableModel.getValueAt(selectedRow, 1),
                    tableModel.getValueAt(selectedRow, 2),
                    dateOfBirth,
                    Integer.parseInt(tableModel.getValueAt(selectedRow, 4).toString()),
                    tableModel.getValueAt(selectedRow, 6),
                    tableModel.getValueAt(selectedRow, 7)
            };
            selectionAction.accept(rowData);
        }
    }

    public void loadPatients() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM patients WHERE doctor_id = ? ORDER BY patient_id DESC LIMIT 100";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                java.sql.Date dob = rs.getDate("date_of_birth");
                String dobString = dob != null ? dateFormatter.format(dob.toLocalDate()) : "";
                tableModel.addRow(new Object[]{
                        rs.getInt("patient_id"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        dobString,
                        rs.getInt("age"),
                        rs.getString("department"),
                        rs.getString("symptoms"),
                        rs.getString("diagnosis")
                });
                count++;
            }
            PatientPanel.logger.debug("Loaded {} patient records into table.", count);
        } catch (SQLException e) {
            PatientPanel.logger.error("Error loading patients: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setSelectionAction(Consumer<Object[]> action) {
        this.selectionAction = action;
    }

    public void clearSelection() {
        patientTable.clearSelection();
    }

    public int getSelectedPatientId() {
        int selectedRow = patientTable.getSelectedRow();
        return selectedRow != -1 ? (int) tableModel.getValueAt(selectedRow, 0) : -1;
    }
}