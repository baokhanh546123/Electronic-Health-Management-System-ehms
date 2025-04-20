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

public class AllergiesForm extends JPanel {
    static final Logger logger = LoggerFactory.getLogger(AllergiesForm.class);
     Doctor doctor;
     JComboBox<String> patientCombo, severityCombo;
     Map<String, Integer> patientMap;
     JTextField reactionField, allergenField;
     JButton clearButton, updateButton, addButton, deleteButton; // Added deleteButton
     Consumer<AllergyData> updateAction, addAction;
     Runnable clearAction;
     Runnable deleteAction; // Added deleteAction callback

    public static class AllergyData {
        public String patientName, allergen, reaction, severity;

        public AllergyData(String patientName, String allergen, String reaction, String severity) {
            this.patientName = patientName;
            this.allergen = allergen;
            this.reaction = reaction;
            this.severity = severity;
        }
    }

    public AllergiesForm(Doctor doctor) {
        this.doctor = doctor;
        this.patientMap = new HashMap<>();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Thông tin dị ứng",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Patient Combo
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Bệnh nhân:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        patientCombo = new JComboBox<>();
        patientCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        add(patientCombo, gbc);

        // Allergen
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Tác nhân dị ứng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        allergenField = new JTextField(15);
        add(allergenField, gbc);

        // Reaction
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Phản ứng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        reactionField = new JTextField(15);
        add(reactionField, gbc);

        // Severity
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Mức độ nghiêm trọng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        severityCombo = new JComboBox<>();
        severityCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        severityCombo.addItem("Nặng");
        severityCombo.addItem("Trung bình");
        severityCombo.addItem("Nhẹ");
        add(severityCombo, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        addButton = new JButton("Thêm");
        addButton.setBackground(new Color(50, 205, 50));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> {
            if (addAction != null) {
                addAction.accept(getAllergyData());
            }
        });
        buttonPanel.add(addButton);

        updateButton = new JButton("Sửa");
        updateButton.setBackground(new Color(255, 165, 0));
        updateButton.setForeground(Color.WHITE);
        updateButton.addActionListener(e -> {
            if (updateAction != null) {
                updateAction.accept(getAllergyData());
            }
        });
        buttonPanel.add(updateButton);

        deleteButton = new JButton("Xóa"); // Added Delete button
        deleteButton.setBackground(new Color(255, 69, 0)); // Crimson color for delete
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> {
            if (deleteAction != null) {
                deleteAction.run();
            }
        });
        buttonPanel.add(deleteButton);


        add(buttonPanel, gbc);

        loadPatientsIntoComboBox();
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
            if (count == 0) {
                logger.warn("No patients found for doctor_id: {}", doctor.getDoctorId());
            }
        } catch (SQLException e) {
            logger.error("Error loading patients: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Map<String, Integer> getPatientMap() {
        return patientMap;
    }

    public AllergyData getAllergyData() {
        String patientName = (String) patientCombo.getSelectedItem();
        String allergen = allergenField.getText().trim();
        String reaction = reactionField.getText().trim();
        String severity = (String) severityCombo.getSelectedItem();
        return new AllergyData(patientName, allergen, reaction, severity);
    }

    public void populateForm(String patientName, String allergen, String reaction, String severity) {
        patientCombo.setSelectedItem(patientName);
        allergenField.setText(allergen);
        reactionField.setText(reaction);
        severityCombo.setSelectedItem(severity);
    }

    public void clearForm() {
        patientCombo.setSelectedIndex(-1);
        allergenField.setText("");
        reactionField.setText("");
        severityCombo.setSelectedIndex(-1);
    }

    public void setAddAction(Consumer<AllergyData> action) {
        this.addAction = action;
    }

    public void setUpdateAction(Consumer<AllergyData> action) {
        this.updateAction = action;
    }

    public void setClearAction(Runnable action) {
        this.clearAction = action;
    }

    public void setDeleteAction(Runnable action) { // Added setter for delete action
        this.deleteAction = action;
    }
}