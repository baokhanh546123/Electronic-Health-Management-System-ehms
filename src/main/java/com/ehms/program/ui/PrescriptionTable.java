package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PrescriptionTable extends JPanel {
    DefaultTableModel tableModel;
    JTable prescriptionsTable;
    Doctor doctor;
    JComboBox<String> filterPatientCombo;
    Map<String, Integer> patientMap;
    Consumer<Object[]> selectionAction;

    public PrescriptionTable(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>(); // Initialize patientMap to avoid NullPointerException
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Danh sách đơn thuốc",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Lọc theo bệnh nhân:"));
        filterPatientCombo = new JComboBox<>();
        filterPatientCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        updatePatientFilter(patientMap); // Populate filterPatientCombo
        filterPatientCombo.addActionListener(e -> loadPrescriptions());
        filterPanel.add(filterPatientCombo);
        add(filterPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Bệnh nhân", "Thuốc", "Hướng dẫn sử dụng", "Liều lượng", "Số lượng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        prescriptionsTable = new JTable(tableModel);
        prescriptionsTable.setRowHeight(30);
        prescriptionsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        prescriptionsTable.setShowGrid(true);
        prescriptionsTable.setGridColor(new Color(200, 200, 200));
        prescriptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        prescriptionsTable.setAutoCreateRowSorter(true);

        // Set preferred column widths
        prescriptionsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        prescriptionsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        prescriptionsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        prescriptionsTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        prescriptionsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        prescriptionsTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        // Alternating row colors
        prescriptionsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                }
                return c;
            }
        });

        prescriptionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = prescriptionsTable.getSelectedRow();
                if (selectedRow != -1 && selectionAction != null) {
                    Object[] rowData = new Object[]{
                            tableModel.getValueAt(selectedRow, 1), // patientName
                            tableModel.getValueAt(selectedRow, 2), // medicineName
                            tableModel.getValueAt(selectedRow, 3), // usageInstructions
                            tableModel.getValueAt(selectedRow, 4), // dosage
                            tableModel.getValueAt(selectedRow, 5)  // quantity (now an int)
                    };
                    selectionAction.accept(rowData);
                }
            }
        });

        loadPrescriptions();
        JScrollPane tableScrollPane = new JScrollPane(prescriptionsTable);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    public void updatePatientFilter(Map<String, Integer> newPatientMap) {
        this.patientMap = newPatientMap;
        filterPatientCombo.removeAllItems();
        filterPatientCombo.addItem("Tất cả");
        for (String patientName : patientMap.keySet()) {
            filterPatientCombo.addItem(patientName);
        }
    }

    public void loadPrescriptions() {
        tableModel.setRowCount(0);
        String patientFilter = (String) filterPatientCombo.getSelectedItem();
        Integer patientId = patientFilter != null && !patientFilter.equals("Tất cả") ? patientMap.get(patientFilter) : null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.prescription_id, p.patient_id, pt.full_name, m.medicine_name, p.usage_instructions, p.dosage, p.quantity " +
                         "FROM prescriptions p " +
                         "JOIN patients pt ON p.patient_id = pt.patient_id " +
                         "JOIN medicines m ON p.medicine_id = m.medicine_id " +
                         "WHERE pt.doctor_id = ?" + (patientId != null ? " AND p.patient_id = ?" : "");
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            if (patientId != null) {
                stmt.setInt(2, patientId);
            }
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                int quantity = rs.getInt("quantity");
                tableModel.addRow(new Object[]{
                        rs.getInt("prescription_id"),
                        rs.getString("full_name"),
                        rs.getString("medicine_name"),
                        rs.getString("usage_instructions"),
                        rs.getString("dosage"),
                        quantity
                });
                count++;
            }
            PrescriptionPanel.logger.debug("Loaded {} prescription records into table.", count);
        } catch (SQLException e) {
            PrescriptionPanel.logger.error("Error loading prescriptions: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách đơn thuốc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setSelectionAction(Consumer<Object[]> action) {
        this.selectionAction = action;
    }

    public void clearSelection() {
        prescriptionsTable.clearSelection();
    }

    public int getSelectedPrescriptionId() {
        int selectedRow = prescriptionsTable.getSelectedRow();
        if (selectedRow != -1) {
            return (int) tableModel.getValueAt(selectedRow, 0);
        }
        return -1;
    }
}