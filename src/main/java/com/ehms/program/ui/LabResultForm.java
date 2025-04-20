package com.ehms.program.ui;

import com.toedter.calendar.JDateChooser;
import com.ehms.program.util.*;
import com.ehms.program.model.Doctor;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LabResultForm extends JPanel implements PatientChangeListener  {
    JComboBox<String> patientCombo, testNameCombo;
    JTextField resultField;
    JDateChooser testDateChooser;
    JLabel unitLabel;
    Map<String, Integer> patientMap;
    Map<String, String> testToUnitMap;
    Consumer<LabResultData> addAction, updateAction;
    Runnable deleteAction;
    Doctor doctor;

    public static class LabResultData {
        public String patientName;
        public String testName;
        public String resultValue;
        public Date testDate;

        public LabResultData(String patientName, String testName, String resultValue, Date testDate) {
            this.patientName = patientName;
            this.testName = testName;
            this.resultValue = resultValue;
            this.testDate = testDate;
        }
    }

    public LabResultForm(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>();
        this.testToUnitMap = new HashMap<>();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Thông tin kết quả xét nghiệm",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        setBackground(new Color(245, 245, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Patient Selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel patientLabel = new JLabel("Bệnh nhân:");
        patientLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        patientLabel.setToolTipText("Chọn bệnh nhân để nhập kết quả xét nghiệm");
        add(patientLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        patientCombo = new JComboBox<>();
        patientCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        loadPatientsIntoComboBox();
        add(patientCombo, gbc);

        // Test Name (JComboBox)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel testNameLabel = new JLabel("Tên xét nghiệm:");
        testNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        testNameLabel.setToolTipText("Chọn xét nghiệm từ danh sách");
        add(testNameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        testNameCombo = new JComboBox<>();
        testNameCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        loadTestNamesIntoComboBox();
        testNameCombo.addActionListener(e -> updateUnitLabel());
        add(testNameCombo, gbc);

        // Unit Label
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 5, 10, 10);
        unitLabel = new JLabel("");
        unitLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        unitLabel.setForeground(new Color(34, 139, 34));
        add(unitLabel, gbc);

        // Reset insets
        gbc.insets = new Insets(10, 10, 10, 10);

        // Result Value
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel resultLabel = new JLabel("Kết quả:");
        resultLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        resultLabel.setToolTipText("Nhập kết quả xét nghiệm (ví dụ: 120)");
        add(resultLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        resultField = new JTextField(15);
        resultField.setFont(new Font("Arial", Font.PLAIN, 14));
        add(resultField, gbc);

        // Test Date
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel testDateLabel = new JLabel("Ngày xét nghiệm:");
        testDateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        testDateLabel.setToolTipText("Chọn ngày thực hiện xét nghiệm");
        add(testDateLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        testDateChooser = new JDateChooser();
        testDateChooser.setDateFormatString("yyyy-MM-dd");
        testDateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        add(testDateChooser, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); // Increased spacing
        buttonPanel.setBackground(new Color(245, 245, 245));

        JButton addButton = new JButton("Thêm");
        addButton.setBackground(new Color(50, 205, 50));
        addButton.setForeground(Color.WHITE);
        /*
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 165, 40), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        addButton.setToolTipText("Thêm kết quả xét nghiệm mới");
        addButton.addActionListener(e -> {
            if (addAction != null) {
                addAction.accept(getFormData());
            }
        });
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Sửa");
        updateButton.setBackground(new Color(255, 165, 0));
        updateButton.setForeground(Color.WHITE);
        /*
        updateButton.setFont(new Font("Arial", Font.BOLD, 14));
        updateButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 135, 0), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        updateButton.setToolTipText("Cập nhật kết quả xét nghiệm đã chọn");
        updateButton.addActionListener(e -> {
            if (updateAction != null) {
                updateAction.accept(getFormData());
            }
        });
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Xóa");
        deleteButton.setBackground(new Color(255, 69, 0));
        deleteButton.setForeground(Color.WHITE);
        //deleteButton.setFont(new Font("Arial", Font.BOLD, 14));
        /*deleteButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 55, 0), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        deleteButton.setToolTipText("Xóa kết quả xét nghiệm đã chọn");
        deleteButton.addActionListener(e -> {
            if (deleteAction != null) {
                deleteAction.run();
            }
        });
        buttonPanel.add(deleteButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        // Set initial unit label
        updateUnitLabel();
    }

    public void loadPatientsIntoComboBox() {
        patientCombo.removeAllItems();
        patientMap.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT patient_id, full_name FROM patients WHERE doctor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String fullName = rs.getString("full_name");
                patientCombo.addItem(fullName);
                patientMap.put(fullName, patientId);
            }
        } catch (SQLException e) {
            LabResultsPanel.logger.error("Error loading patients: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách bệnh nhân!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadTestNamesIntoComboBox() {
        testNameCombo.removeAllItems();
        testToUnitMap.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT DISTINCT ON (test_name) test_name, result_value " +
                        "FROM lab_results " +
                        "ORDER BY test_name, test_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String testName = rs.getString("test_name");
                String resultValue = rs.getString("result_value");
                String unit = extractUnit(resultValue);
                testNameCombo.addItem(testName);
                testToUnitMap.put(testName, unit);
            }
        } catch (SQLException e) {
            LabResultsPanel.logger.error("Error loading test names: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách xét nghiệm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    String extractUnit(String resultValue) {
        if (resultValue == null || resultValue.trim().isEmpty()) {
            return "";
        }

        int lastSpaceIndex = resultValue.lastIndexOf(" ");
        if (lastSpaceIndex != -1 && lastSpaceIndex < resultValue.length() - 1) {
            return resultValue.substring(lastSpaceIndex + 1).trim();
        }

        int length = resultValue.length();
        if (length >= 4) {
            String potentialUnit = resultValue.substring(length - 4).trim();
            if (potentialUnit.equals("mmHg") || potentialUnit.equals("g/dL") || 
                potentialUnit.equals("mg/L") || potentialUnit.equals("K/µL") || 
                potentialUnit.equals("IU/L")) {
                return potentialUnit;
            }
        }
        if (length >= 2) {
            String potentialUnit = resultValue.substring(length - 2).trim();
            if (potentialUnit.equals("%")) {
                return potentialUnit;
            }
        }
        return "";
    }

    void updateUnitLabel() {
        String selectedTest = (String) testNameCombo.getSelectedItem();
        String unit = testToUnitMap.getOrDefault(selectedTest, "");
        unitLabel.setText(unit.isEmpty() ? "" : "(" + unit + ")");
    }

    public void setAddAction(Consumer<LabResultData> action) {
        this.addAction = action;
    }

    public void setUpdateAction(Consumer<LabResultData> action) {
        this.updateAction = action;
    }

    public void setDeleteAction(Runnable action) {
        this.deleteAction = action;
    }

    public void clearForm() {
        patientCombo.setSelectedIndex(-1);
        testNameCombo.setSelectedIndex(-1);
        resultField.setText("");
        testDateChooser.setDate(null);
        updateUnitLabel();
    }

    public void populateForm(String patientName, String testName, String resultValue, Date testDate) {
        patientCombo.setSelectedItem(patientName);
        testNameCombo.setSelectedItem(testName);
        resultField.setText(resultValue);
        testDateChooser.setDate(testDate);
        updateUnitLabel();
    }

    public LabResultData getFormData() {
        String resultValue = resultField.getText().trim();
        String selectedTest = (String) testNameCombo.getSelectedItem();
        String unit = testToUnitMap.getOrDefault(selectedTest, "");
        if (!unit.isEmpty() && !resultValue.endsWith(unit)) {
            resultValue = resultValue + " " + unit;
        }
        return new LabResultData(
                (String) patientCombo.getSelectedItem(),
                selectedTest,
                resultValue,
                testDateChooser.getDate()
        );
    }

    public Map<String, Integer> getPatientMap() {
        return patientMap;
    }

    @Override
    public void onPatientAdded() {
        loadPatientsIntoComboBox();
        loadTestNamesIntoComboBox();
    }

    @Override
    public void onPatientUpdated() {
        loadPatientsIntoComboBox();
        loadTestNamesIntoComboBox();
    }

    @Override
    public void onPatientDeleted() {
        loadPatientsIntoComboBox();
        loadTestNamesIntoComboBox();
    }
}