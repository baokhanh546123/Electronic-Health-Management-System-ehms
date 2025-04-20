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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MedicalHistoryTable extends JPanel {
    DefaultTableModel tableModel;
    JTable historyTable;
    Doctor doctor;
    JComboBox<String> filterPatientCombo;
    Map<String, Integer> patientMap;
    Consumer<Object[]> selectionAction;

    public MedicalHistoryTable(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>(); // Initialize patientMap
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Danh sách lịch sử y tế",
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
        updatePatientFilter(patientMap); // Initial population
        filterPatientCombo.addActionListener(e -> loadMedicalHistory());
        filterPanel.add(filterPatientCombo);
        add(filterPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Tên bệnh nhân", "Tình trạng", "Ngày chẩn đoán", "Điều trị"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(tableModel);
        historyTable.setRowHeight(30);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 14));
        historyTable.setShowGrid(true);
        historyTable.setGridColor(new Color(200, 200, 200));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setAutoCreateRowSorter(true);

        // Set preferred column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(200);

        // Alternating row colors
        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                }
                return c;
            }
        });

        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = historyTable.getSelectedRow();
                if (selectedRow != -1 && selectionAction != null) {
                    String diagnosisDateStr = (String) tableModel.getValueAt(selectedRow, 3);
                    LocalDate diagnosisDate = diagnosisDateStr != null ? LocalDate.parse(diagnosisDateStr) : null;
                    Object[] rowData = new Object[]{
                            tableModel.getValueAt(selectedRow, 1), // patientName
                            tableModel.getValueAt(selectedRow, 2), // condition
                            diagnosisDate,                         // diagnosisDate as LocalDate
                            tableModel.getValueAt(selectedRow, 4)  // treatment
                    };
                    selectionAction.accept(rowData);
                }
            }
        });

        loadMedicalHistory();
        JScrollPane tableScrollPane = new JScrollPane(historyTable);
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

    public void loadMedicalHistory() {
        tableModel.setRowCount(0);
        String patientFilter = (String) filterPatientCombo.getSelectedItem();
        Integer patientId = patientFilter != null && !patientFilter.equals("Tất cả") ? patientMap.get(patientFilter) : null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT mh.*, p.full_name " +
                         "FROM medical_history mh " +
                         "JOIN patients p ON mh.patient_id = p.patient_id " +
                         "WHERE p.doctor_id = ?" + (patientId != null ? " AND mh.patient_id = ?" : "");
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            if (patientId != null) {
                stmt.setInt(2, patientId);
            }
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                LocalDate diagnosisDate = rs.getDate("diagnosis_date") != null ?
                        rs.getDate("diagnosis_date").toLocalDate() : null;
                tableModel.addRow(new Object[]{
                        rs.getInt("history_id"),
                        rs.getString("full_name"),
                        rs.getString("condition"),
                        diagnosisDate != null ? diagnosisDate.toString() : null,
                        rs.getString("treatment")
                });
                count++;
            }
            MedicalHistoryPanel.logger.debug("Loaded {} medical history records into table.", count);
        } catch (SQLException e) {
            MedicalHistoryPanel.logger.error("Error loading medical history: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách lịch sử y tế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setSelectionAction(Consumer<Object[]> action) {
        this.selectionAction = action;
    }

    public void clearSelection() {
        historyTable.clearSelection();
    }

    public int getSelectedHistoryId() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow != -1) {
            return (int) tableModel.getValueAt(selectedRow, 0);
        }
        return -1;
    }
}