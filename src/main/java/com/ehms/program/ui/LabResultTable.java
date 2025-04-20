package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

public class LabResultTable extends JPanel {
     DefaultTableModel tableModel;
     JTable labResultsTable;
     JComboBox<String> filterPatientCombo;
     Map<String, Integer> patientMap;
     Doctor doctor;
     SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
     Consumer<Object[]> selectionAction;

    public LabResultTable(Doctor doctor, Map<String, Integer> patientMap) {
        this.doctor = doctor;
        this.patientMap = patientMap;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Danh sách kết quả xét nghiệm",
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
        filterPatientCombo.addActionListener(e -> loadLabResults());
        filterPanel.add(filterPatientCombo);
        add(filterPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Bệnh nhân", "Tên xét nghiệm", "Kết quả", "Ngày xét nghiệm", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        labResultsTable = new JTable(tableModel);
        labResultsTable.setRowHeight(30);
        labResultsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        labResultsTable.setShowGrid(true);
        labResultsTable.setGridColor(new Color(200, 200, 200));
        labResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        labResultsTable.setAutoCreateRowSorter(true); // Enable sorting

        // Set preferred column widths
        labResultsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        labResultsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        labResultsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        labResultsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        labResultsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        labResultsTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        // Alternating row colors and status coloring
        labResultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                    if (column == 5) { // Status column
                        String status = value != null ? value.toString() : "";
                        if (status.equals("Bình thường")) {
                            c.setForeground(new Color(34, 139, 34)); // Green
                        } else if (status.equals("Bất thường")) {
                            c.setForeground(new Color(178, 34, 34)); // Red
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        labResultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = labResultsTable.getSelectedRow();
                if (selectedRow != -1 && selectionAction != null) {
                    Object[] rowData = new Object[]{
                            tableModel.getValueAt(selectedRow, 1),
                            tableModel.getValueAt(selectedRow, 2),
                            tableModel.getValueAt(selectedRow, 3),
                            tableModel.getValueAt(selectedRow, 4)
                    };
                    selectionAction.accept(rowData);
                }
            }
        });

        loadLabResults();
        JScrollPane tableScrollPane = new JScrollPane(labResultsTable);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    public void loadLabResults() {
        tableModel.setRowCount(0);
        String patientFilter = (String) filterPatientCombo.getSelectedItem();
        Integer patientId = patientFilter != null && !patientFilter.equals("Tất cả") ? patientMap.get(patientFilter) : null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT lr.result_id, lr.patient_id, pt.full_name, lr.test_name, lr.result_value, lr.test_date, pt.gender " +
                         "FROM lab_results lr " +
                         "JOIN patients pt ON lr.patient_id = pt.patient_id " +
                         "WHERE pt.doctor_id = ?" + (patientId != null ? " AND lr.patient_id = ?" : "") +
                         " ORDER BY lr.test_name DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            if (patientId != null) {
                stmt.setInt(2, patientId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String resultValue = rs.getString("result_value");
                String displayValue = extractValue(resultValue); // Extract value without unit for display
                String testName = rs.getString("test_name");
                String gender = rs.getString("gender");
                String status = determineStatus(testName, resultValue, gender);
                tableModel.addRow(new Object[]{
                        rs.getInt("result_id"),
                        rs.getString("full_name"),
                        testName,
                        displayValue, // Show only the value part
                        dateFormat.format(rs.getDate("test_date")),
                        status
                });
            }
        } catch (SQLException e) {
            LabResultsPanel.logger.error("Error loading lab results: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách kết quả xét nghiệm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     String extractValue(String resultValue) {
        if (resultValue == null || resultValue.trim().isEmpty()) {
            return "";
        }

        // Split on the last space to separate value and unit
        int lastSpaceIndex = resultValue.lastIndexOf(" ");
        if (lastSpaceIndex != -1 && lastSpaceIndex > 0) {
            return resultValue.substring(0, lastSpaceIndex).trim();
        }

        // If no space, try to remove known units
        if (resultValue.endsWith("mmHg")) return resultValue.substring(0, resultValue.length() - 4).trim();
        if (resultValue.endsWith("g/dL")) return resultValue.substring(0, resultValue.length() - 4).trim();
        if (resultValue.endsWith("mg/L")) return resultValue.substring(0, resultValue.length() - 4).trim();
        if (resultValue.endsWith("K/µL")) return resultValue.substring(0, resultValue.length() - 4).trim();
        if (resultValue.endsWith("IU/L")) return resultValue.substring(0, resultValue.length() - 4).trim();
        if (resultValue.endsWith("mIU/L")) return resultValue.substring(0, resultValue.length() - 5).trim();
        if (resultValue.endsWith("G/L")) return resultValue.substring(0, resultValue.length() - 3).trim();
        if (resultValue.endsWith("%")) return resultValue.substring(0, resultValue.length() - 1).trim();

        return resultValue; // Return as-is if no unit can be determined
    }

     double parseResultValue(String resultValue) {
        String valueStr = extractValue(resultValue);
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            LabResultsPanel.logger.error("Error parsing result value: {}", resultValue, e);
            return Double.NaN; // Return NaN if parsing fails
        }
    }

     String determineStatus(String testName, String result, String gender) {
        if (result == null || result.trim().isEmpty()) {
            return "Không xác định";
        }

        // Parse the numeric value from the result
        double value = parseResultValue(result);
        if (Double.isNaN(value)) {
            // Check for textual results like "bình thường" or "bất thường"
            if (result.toLowerCase().contains("bình thường")) {
                return "Bình thường";
            } else if (result.toLowerCase().contains("bất thường")) {
                return "Bất thường";
            }
            return "Không xác định";
        }

        // TSH: 0.4 to 5 mIU/L, below is bad
        if (testName.toLowerCase().contains("tsh")) {
            if (result.endsWith("mIU/L")) {
                if (value >= 0.4 && value <= 5.0) {
                    return "Bình thường";
                } else if (value < 0.4) {
                    return "Bất thường (Thấp - Có thể suy giáp)";
                } else {
                    return "Bất thường (Cao - Có thể cường giáp)";
                }
            }
        }

        // LDL-Cholesterol: <130 mg/dL, higher is bad
        if (testName.toLowerCase().contains("ldl-cholesterol")) {
            if (result.endsWith("mg/dL")) {
                if (value < 130) {
                    return "Bình thường";
                } else {
                    return "Bất thường (Cao - Nguy cơ tim mạch)";
                }
            }
        }

        // Creatinine: Male 0.6-1.2 mg/dL, Female 0.5-1.1 mg/dL
        if (testName.toLowerCase().contains("creatinine")) {
            if (result.endsWith("mg/dL")) {
                if (gender != null && gender.equalsIgnoreCase("Nam")) {
                    if (value >= 0.6 && value <= 1.2) {
                        return "Bình thường";
                    } else if (value < 0.6) {
                        return "Bất thường (Thấp)";
                    } else {
                        return "Bất thường (Cao - Có thể suy thận)";
                    }
                } else if (gender != null && gender.equalsIgnoreCase("Nữ")) {
                    if (value >= 0.5 && value <= 1.1) {
                        return "Bình thường";
                    } else if (value < 0.5) {
                        return "Bất thường (Thấp)";
                    } else {
                        return "Bất thường (Cao - Có thể suy thận)";
                    }
                }
            }
        }

        // CRP: <5 mg/L, higher indicates inflammation
        if (testName.toLowerCase().contains("crp")) {
            if (result.endsWith("mg/L")) {
                if (value < 5) {
                    return "Bình thường";
                } else {
                    return "Bất thường (Cao - Có thể viêm nhiễm)";
                }
            }
        }

        // Glucose: Random <140 mg/dL, Fasting <100 mg/dL, Postprandial <140 mg/dL
        if (testName.toLowerCase().contains("đường huyết")) {
            if (result.endsWith("mg/dL")) {
                if (testName.toLowerCase().contains("lúc đói")) {
                    if (value < 100) {
                        return "Bình thường";
                    } else {
                        return "Bất thường (Cao - Có thể đái tháo đường)";
                    }
                } else if (testName.toLowerCase().contains("sau bữa ăn")) {
                    if (value < 140) {
                        return "Bình thường";
                    } else {
                        return "Bất thường (Cao - Có thể đái tháo đường)";
                    }
                } else { // Random glucose
                    if (value < 140) {
                        return "Bình thường";
                    } else {
                        return "Bất thường (Cao - Có thể đái tháo đường)";
                    }
                }
            }
        }

        // HbA1c: <4.0% low, <5.7% normal, 5.7-6.4% prediabetes, >=6.5% diabetes
        if (testName.toLowerCase().contains("hba1c")) {
            if (result.endsWith("%")) {
                if (value < 4.0) {
                    return "Bất thường (Thấp - Nguy cơ hạ đường huyết)";
                } else if (value < 5.7) {
                    return "Bình thường";
                } else if (value <= 6.4) {
                    return "Bất thường (Tiền đái tháo đường)";
                } else {
                    return "Bất thường (Cao - Có thể đái tháo đường)";
                }
            }
        }

        // PLT: 150-400 G/L, <150 bleeding risk, >450 clotting risk
        if (testName.toLowerCase().contains("plt")) {
            if (result.endsWith("G/L")) {
                if (value >= 150 && value <= 400) {
                    return "Bình thường";
                } else if (value < 150) {
                    return "Bất thường (Thấp - Nguy cơ rối loạn đông máu)";
                } else {
                    return "Bất thường (Cao - Nguy cơ cục máu đông)";
                }
            }
        }

        // WBC: <4 G/L low, >4 G/L high (normal range not specified, assuming 4-10 G/L as typical)
        if (testName.toLowerCase().contains("wbc")) {
            if (result.endsWith("G/L")) {
                if (value >= 4 && value <= 10) { // Assuming 4-10 G/L as normal range
                    return "Bình thường";
                } else if (value < 4) {
                    return "Bất thường (Thấp - Giảm bạch cầu)";
                } else {
                    return "Bất thường (Cao - Tăng bạch cầu)";
                }
            }
        }

        return "Không xác định";
    }

    public void updatePatientFilter(Map<String, Integer> newPatientMap) {
        this.patientMap = newPatientMap;
        filterPatientCombo.removeAllItems();
        filterPatientCombo.addItem("Tất cả");
        for (String patientName : patientMap.keySet()) {
            filterPatientCombo.addItem(patientName);
        }
    }

    public void setSelectionAction(Consumer<Object[]> action) {
        this.selectionAction = action;
    }

    public void clearSelection() {
        labResultsTable.clearSelection();
    }

    public int getSelectedLabResultId() {
        int selectedRow = labResultsTable.getSelectedRow();
        if (selectedRow != -1) {
            return (int) tableModel.getValueAt(selectedRow, 0);
        }
        return -1;
    }
}