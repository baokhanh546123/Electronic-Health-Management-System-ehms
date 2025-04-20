package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MedicalHistoryForm extends JPanel {
    static final Logger logger = LoggerFactory.getLogger(MedicalHistoryForm.class);
     Doctor doctor;
     JComboBox<String> patientNameCombo;
     Map<String, Integer> patientMap;
     JTextField conditionField;
     DatePicker diagnosisDatePicker;
     JComboBox<String> treatmentCombo;
     JButton addButton, updateButton, deleteButton, clearButton;
     Consumer<MedicalHistoryData> addAction, updateAction;
     Runnable deleteAction, clearAction;

    public static class MedicalHistoryData {
        public String patientName;
        public String condition, treatment;
        public LocalDate diagnosisDate; // Changed to LocalDate

        public MedicalHistoryData(String patientName, String condition, LocalDate diagnosisDate, String treatment) {
            this.patientName = patientName;
            this.condition = condition;
            this.diagnosisDate = diagnosisDate;
            this.treatment = treatment;
        }
    }

    public MedicalHistoryForm(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>();
        setLayout(new GridBagLayout());
        //setBorder(BorderFactory.createTitledBorder("Thông tin lịch sử y tế"));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Thông tin lịch sử y tế",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Patient Name Selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Tên bệnh nhân:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        patientNameCombo = new JComboBox<>();
        patientNameCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        patientNameCombo.setPreferredSize(new Dimension(250, 30));
        loadPatientNames();
        add(patientNameCombo, gbc);

        // Condition
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        add(new JLabel("Tình trạng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        conditionField = new JTextField();
        conditionField.setPreferredSize(new Dimension(250, 30));
        add(conditionField, gbc);

        // Diagnosis Date
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        add(new JLabel("Ngày chẩn đoán:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setAllowEmptyDates(false);
        diagnosisDatePicker = new DatePicker(dateSettings);
        diagnosisDatePicker.setPreferredSize(new Dimension(250, 30));
        add(diagnosisDatePicker, gbc);

        // Treatment
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        add(new JLabel("Điều trị:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        treatmentCombo = new JComboBox<>();
        treatmentCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        treatmentCombo.setPreferredSize(new Dimension(250, 30));
        treatmentCombo.setEditable(true);
        loadTreatments();
        add(treatmentCombo, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        addButton = new JButton("Thêm");
        addButton.setBackground(new Color(50, 205, 50));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> {
            if (addAction != null) {
                addAction.accept(getMedicalHistoryData());
            }
        });
        buttonPanel.add(addButton);

        updateButton = new JButton("Sửa");
        updateButton.setBackground(new Color(255, 165, 0));
        updateButton.setForeground(Color.WHITE);
        updateButton.addActionListener(e -> {
            if (updateAction != null) {
                updateAction.accept(getMedicalHistoryData());
            }
        });
        buttonPanel.add(updateButton);

        deleteButton = new JButton("Xóa");
        deleteButton.setBackground(new Color(255, 69, 0));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> {
            if (deleteAction != null) {
                deleteAction.run();
            }
        });
        buttonPanel.add(deleteButton);

        

        add(buttonPanel, gbc);
    }

    public void loadPatientNames() {
        patientNameCombo.removeAllItems();
        patientMap.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT patient_id, full_name FROM patients WHERE doctor_id = ? ORDER BY full_name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String fullName = rs.getString("full_name");
                patientNameCombo.addItem(fullName);
                patientMap.put(fullName, patientId);
                count++;
            }
            logger.debug("Loaded {} patient names into patientNameCombo.", count);
        } catch (SQLException e) {
            logger.error("Error loading patient names: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách tên bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadTreatments() {
        treatmentCombo.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT treatment FROM medical_history GROUP BY treatment ORDER BY treatment";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                String treatment = rs.getString("treatment");
                if (treatment != null && !treatment.trim().isEmpty()) {
                    treatmentCombo.addItem(treatment);
                    count++;
                }
            }
            logger.debug("Loaded {} treatments into treatmentCombo.", count);
        } catch (SQLException e) {
            logger.error("Error loading treatments: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách điều trị: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public MedicalHistoryData getMedicalHistoryData() {
        String patientName = (String) patientNameCombo.getSelectedItem();
        String condition = conditionField.getText().trim();
        LocalDate diagnosisDate = diagnosisDatePicker.getDate();
        String treatment = (String) treatmentCombo.getSelectedItem();
        if (treatment != null) {
            treatment = treatment.trim();
        }
        return new MedicalHistoryData(patientName, condition, diagnosisDate, treatment);
    }

    public void populateForm(String patientName, String condition, LocalDate diagnosisDate, String treatment) {
        patientNameCombo.setSelectedItem(patientName);
        conditionField.setText(condition);
        diagnosisDatePicker.setDate(diagnosisDate);
        treatmentCombo.setSelectedItem(treatment);
    }

    public void clearForm() {
        patientNameCombo.setSelectedIndex(-1);
        conditionField.setText("");
        diagnosisDatePicker.setDate(null);
        treatmentCombo.setSelectedIndex(-1);
    }

    public Map<String, Integer> getPatientMap() {
        return patientMap;
    }

    public void setAddAction(Consumer<MedicalHistoryData> action) {
        this.addAction = action;
    }

    public void setUpdateAction(Consumer<MedicalHistoryData> action) {
        this.updateAction = action;
    }

    public void setDeleteAction(Runnable action) {
        this.deleteAction = action;
    }

    public void setClearAction(Runnable action) {
        this.clearAction = action;
    }
}