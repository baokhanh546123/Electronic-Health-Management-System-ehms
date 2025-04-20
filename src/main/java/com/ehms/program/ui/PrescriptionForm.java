package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PrescriptionForm extends JPanel {
    static final Logger logger = LoggerFactory.getLogger(PrescriptionForm.class);
     Doctor doctor;
     JComboBox<String> patientCombo, medicineCombo, usageInstructionsCombo, dosageCombo;
     JTextField quantityField;
     Map<String, Integer> patientMap;
     Map<String, Integer> medicineMap;
     JButton addButton, updateButton, deleteButton, clearButton;
     Consumer<PrescriptionData> addAction, updateAction;
     Runnable deleteAction, clearAction;

    public static class PrescriptionData {
        public String patientName, medicineName, usageInstructions, dosage;
        public int quantity; 

        public PrescriptionData(String patientName, String medicineName, String usageInstructions, String dosage, int quantity) {
            this.patientName = patientName;
            this.medicineName = medicineName;
            this.usageInstructions = usageInstructions;
            this.dosage = dosage;
            this.quantity = quantity;
        }
    }

    public PrescriptionForm(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>();
        this.medicineMap = new HashMap<>();
        setLayout(new GridBagLayout());
        //setBorder(BorderFactory.createTitledBorder("Thông tin đơn thuốc"));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 1),
                "Thông tin đơn thuốc",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Patient Selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Bệnh nhân:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        patientCombo = new JComboBox<>();
        patientCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        loadPatientsIntoComboBox();
        add(patientCombo, gbc);

        // Medicine Selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Tên thuốc:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        medicineCombo = new JComboBox<>();
        medicineCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        medicineCombo.setEditable(true);
        loadMedicinesIntoComboBox();
        add(medicineCombo, gbc);

        // Usage Instructions
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Hướng dẫn sử dụng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        usageInstructionsCombo = new JComboBox<>();
        usageInstructionsCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        usageInstructionsCombo.setEditable(true);
        loadUsageInstructions();
        add(usageInstructionsCombo, gbc);

        // Dosage
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Liều dùng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        dosageCombo = new JComboBox<>();
        dosageCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        dosageCombo.setEditable(true);
        loadDosages();
        add(dosageCombo, gbc);

        // Quantity
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Số lượng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        quantityField = new JTextField(15);
        add(quantityField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        addButton = new JButton("Thêm");
        addButton.setBackground(new Color(50, 205, 50));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> {
            if (addAction != null) {
                addAction.accept(getPrescriptionData());
            }
        });
        buttonPanel.add(addButton);

        updateButton = new JButton("Sửa");
        updateButton.setBackground(new Color(255, 165, 0));
        updateButton.setForeground(Color.WHITE);
        updateButton.addActionListener(e -> {
            if (updateAction != null) {
                updateAction.accept(getPrescriptionData());
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

    public void loadPatientsIntoComboBox() {
        patientCombo.removeAllItems();
        patientMap.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT patient_id, full_name FROM patients WHERE doctor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctor.getDoctorId());
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String fullName = rs.getString("full_name");
                patientCombo.addItem(fullName);
                patientMap.put(fullName, patientId);
                count++;
            }
            logger.debug("Loaded {} patients into patientCombo.", count);
        } catch (SQLException e) {
            logger.error("Error loading patients: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadMedicinesIntoComboBox() {
        medicineCombo.removeAllItems();
        medicineMap.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT medicine_id, medicine_name FROM medicines ORDER BY medicine_name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                int medicineId = rs.getInt("medicine_id");
                String medicineName = rs.getString("medicine_name");
                medicineCombo.addItem(medicineName);
                medicineMap.put(medicineName, medicineId);
                count++;
            }
            logger.debug("Loaded {} medicines into medicineCombo.", count);
        } catch (SQLException e) {
            logger.error("Error loading medicines: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách thuốc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadUsageInstructions() {
        usageInstructionsCombo.removeAllItems();
        usageInstructionsCombo.addItem("Uống sau ăn");
        usageInstructionsCombo.addItem("Uống trước ăn");
        usageInstructionsCombo.addItem("Uống khi đói");
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT usage_instructions FROM prescriptions GROUP BY usage_instructions ORDER BY usage_instructions";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String usageInstruction = rs.getString("usage_instructions");
                if (usageInstruction != null && !usageInstruction.trim().isEmpty()) {
                    if (usageInstructionsCombo.getItemCount() == 0 ||
                        !usageInstruction.equals(usageInstructionsCombo.getItemAt(usageInstructionsCombo.getItemCount() - 1))) {
                        usageInstructionsCombo.addItem(usageInstruction);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading usage instructions: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách hướng dẫn sử dụng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadDosages() {
        dosageCombo.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT dosage FROM prescriptions GROUP BY dosage ORDER BY dosage";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                String dosage = rs.getString("dosage");
                if (dosage != null && !dosage.trim().isEmpty()) {
                    dosageCombo.addItem(dosage);
                    count++;
                }
            }
            logger.debug("Loaded {} dosages into dosageCombo.", count);
        } catch (SQLException e) {
            logger.error("Error loading dosages: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách liều dùng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Map<String, Integer> getPatientMap() {
        return patientMap;
    }

    public Map<String, Integer> getMedicineMap() {
        return medicineMap;
    }

    public PrescriptionData getPrescriptionData() {
        String patientName = (String) patientCombo.getSelectedItem();
        String medicineName = (String) medicineCombo.getSelectedItem();
        String usageInstructions = (String) usageInstructionsCombo.getSelectedItem();
        String dosage = (String) dosageCombo.getSelectedItem();
        String quantityText = quantityField.getText().trim();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                throw new NumberFormatException("Số lượng phải là số nguyên dương!");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số lượng không hợp lệ: " + e.getMessage());
        }
        return new PrescriptionData(patientName, medicineName, usageInstructions, dosage, quantity);
    }

    public void populateForm(String patientName, String medicineName, String usageInstructions, String dosage, int quantity) {
        patientCombo.setSelectedItem(patientName);
        medicineCombo.setSelectedItem(medicineName);
        usageInstructionsCombo.setSelectedItem(usageInstructions);
        dosageCombo.setSelectedItem(dosage);
        quantityField.setText(String.valueOf(quantity)); // Convert int to String for display
    }

    public void clearForm() {
        patientCombo.setSelectedIndex(-1);
        medicineCombo.setSelectedIndex(-1);
        usageInstructionsCombo.setSelectedIndex(-1);
        dosageCombo.setSelectedIndex(-1);
        quantityField.setText("");
    }

    public void setAddAction(Consumer<PrescriptionData> action) {
        this.addAction = action;
    }

    public void setUpdateAction(Consumer<PrescriptionData> action) {
        this.updateAction = action;
    }

    public void setDeleteAction(Runnable action) {
        this.deleteAction = action;
    }

    public void setClearAction(Runnable action) {
        this.clearAction = action;
    }
}