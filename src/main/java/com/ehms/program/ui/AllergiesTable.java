package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

public class AllergiesTable extends JPanel {
    private DefaultTableModel tableModel;
    private JTable allergiesTable;
    private JComboBox<String> filterPatientCombo;
    private Map<String, Integer> patientMap;
    private Doctor doctor;
    private Consumer<Object[]> selectionAction;

    public AllergiesTable(Doctor doctor, Map<String, Integer> patientMap) {
        this.doctor = doctor;
        this.patientMap = patientMap;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Danh sách dị ứng",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Lọc theo bệnh nhân:"));
        filterPatientCombo = new JComboBox<>();
        filterPatientCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        updatePatientFilter(patientMap);
        filterPatientCombo.addActionListener(e -> loadAllergies());
        filterPanel.add(filterPatientCombo);
        add(filterPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Bệnh nhân", "Tác nhân dị ứng", "Phản ứng", "Mức độ nghiêm trọng"}; // Updated column name
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        allergiesTable = new JTable(tableModel);
        allergiesTable.setRowHeight(30);
        allergiesTable.setFont(new Font("Arial", Font.PLAIN, 14));
        allergiesTable.setShowGrid(true);
        allergiesTable.setGridColor(new Color(200, 200, 200));
        allergiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        allergiesTable.setAutoCreateRowSorter(true);

        // Set preferred column widths
        allergiesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        allergiesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        allergiesTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        allergiesTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        allergiesTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Alternating row colors
        allergiesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                }
                return c;
            }
        });

        allergiesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = allergiesTable.getSelectedRow();
                if (selectedRow != -1 && selectionAction != null) {
                    Object[] rowData = new Object[]{
                            tableModel.getValueAt(selectedRow, 1), // patientName
                            tableModel.getValueAt(selectedRow, 2), // allergen
                            tableModel.getValueAt(selectedRow, 3), // reaction
                            tableModel.getValueAt(selectedRow, 4)  // severity
                    };
                    selectionAction.accept(rowData);
                }
            }
        });

        loadAllergies();
        JScrollPane tableScrollPane = new JScrollPane(allergiesTable);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    public void loadAllergies() {
        tableModel.setRowCount(0);
        String patientFilter = (String) filterPatientCombo.getSelectedItem();
        Integer patientId = patientFilter != null && !patientFilter.equals("Tất cả") ? patientMap.get(patientFilter) : null;

        // If the selected patient no longer exists in patientMap, reset the filter
        if (patientFilter != null && !patientFilter.equals("Tất cả") && !patientMap.containsKey(patientFilter)) {
            AllergiesPanel.logger.debug("Selected patient '{}' no longer exists, resetting filter.", patientFilter);
            filterPatientCombo.setSelectedItem("Tất cả");
            patientId = null;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT a.allergy_id, a.patient_id, pt.full_name, a.allergen, a.reaction, a.severity " +
                         "FROM allergies a " +
                         "JOIN patients pt ON a.patient_id = pt.patient_id " +
                         "WHERE pt.doctor_id = ?" + (patientId != null ? " AND a.patient_id = ?" : "");
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            if (patientId != null) {
                stmt.setInt(2, patientId);
            }
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("allergy_id"),
                        rs.getString("full_name"),
                        rs.getString("allergen"),
                        rs.getString("reaction"),
                        rs.getString("severity")
                });
                count++;
            }
            AllergiesPanel.logger.debug("Loaded {} allergy records into table.", count);
        } catch (SQLException e) {
            AllergiesPanel.logger.error("Error loading allergies: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách dị ứng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updatePatientFilter(Map<String, Integer> newPatientMap) {
        this.patientMap = newPatientMap;
        filterPatientCombo.removeAllItems();
        filterPatientCombo.addItem("Tất cả");
        int count = 0;
        for (String patientName : patientMap.keySet()) {
            filterPatientCombo.addItem(patientName);
            count++;
        }
        AllergiesPanel.logger.debug("Updated filterPatientCombo with {} patients.", count);
    }

    public void setSelectionAction(Consumer<Object[]> action) {
        this.selectionAction = action;
    }

    public void clearSelection() {
        allergiesTable.clearSelection();
    }

    public int getSelectedAllergyId() {
        int selectedRow = allergiesTable.getSelectedRow();
        if (selectedRow != -1) {
            return (int) tableModel.getValueAt(selectedRow, 0);
        }
        return -1;
    }
}