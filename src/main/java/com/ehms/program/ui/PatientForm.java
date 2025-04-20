package com.ehms.program.ui;

import com.ehms.program.model.DiseasePredictor;
import com.ehms.program.model.Doctor;
import com.ehms.program.model.Patient;
import com.ehms.program.util.DatabaseConnection;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Consumer;

public class PatientForm extends JPanel {
     static final Logger logger = LoggerFactory.getLogger(PatientForm.class);
     final Doctor doctor;
     final DiseasePredictor predictor;
     JTextField fullNameField, ageField, symptomsField;
     DatePicker dobField;
     JTextArea diagnosisField;
     JComboBox<String> genderCombo;
     JLabel predictionResultLabel;
     JButton addButton, updateButton, deleteButton;
     Consumer<Patient> addAction, updateAction;
     Runnable deleteAction;

    public PatientForm(Doctor doctor) {
        this.doctor = doctor;
        this.predictor = new DiseasePredictor(doctor.getDepartment());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2, true),
                "Thông tin bệnh nhân",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 16),
                new Color(70, 130, 180)
        ));
        setBackground(new Color(245, 245, 245));

        // Split Pane (Top: Form, Bottom: Prediction)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);

        // Top: Form with Fields and Buttons
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 1),
                "Thông tin bệnh nhân",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        formPanel.setBackground(new Color(255, 255, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Full Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel fullNameLabel = new JLabel("Họ tên:");
        fullNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(fullNameLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        fullNameField = new JTextField(15);
        fullNameField.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(fullNameField, gbc);

        // Gender
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        JLabel genderLabel = new JLabel("Giới tính:");
        genderLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(genderLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        genderCombo = new JComboBox<>(new String[]{"Nam", "Nữ"});
        genderCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(genderCombo, gbc);

        // Date of Birth
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JLabel dobLabel = new JLabel("Ngày sinh:");
        dobLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(dobLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        dateSettings.setAllowEmptyDates(false);
        dobField = new DatePicker(dateSettings);
        dobField.setFont(new Font("Arial", Font.PLAIN, 14));
        dobField.addDateChangeListener(event -> calculateAge());
        formPanel.add(dobField, gbc);

        // Age
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        JLabel ageLabel = new JLabel("Tuổi:");
        ageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(ageLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        ageField = new JTextField(15);
        ageField.setFont(new Font("Arial", Font.PLAIN, 14));
        ageField.setEditable(false);
        formPanel.add(ageField, gbc);

        // Symptoms
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        JLabel symptomsLabel = new JLabel("Triệu chứng:");
        symptomsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(symptomsLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        symptomsField = new JTextField(15);
        symptomsField.setFont(new Font("Arial", Font.PLAIN, 14));
        symptomsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                logger.debug("Symptoms updated: {}", symptomsField.getText());
                suggestDiagnosis();
                predictDisease();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                logger.debug("Symptoms removed: {}", symptomsField.getText());
                suggestDiagnosis();
                predictDisease();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                logger.debug("Symptoms changed: {}", symptomsField.getText());
                suggestDiagnosis();
                predictDisease();
            }
        });
        formPanel.add(symptomsField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10)); // Increased spacing
        buttonPanel.setBackground(new Color(255, 255, 255));

        addButton = new JButton("Thêm");
        addButton.setBackground(new Color(50, 205, 50));
        addButton.setForeground(Color.WHITE);
        /*
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 165, 40), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        addButton.setToolTipText("Thêm bệnh nhân mới");
        addButton.addActionListener(e -> {
            if (addAction != null) addAction.accept(getPatientData());
        });
        buttonPanel.add(addButton);

        updateButton = new JButton("Sửa");
        updateButton.setBackground(new Color(255, 165, 0));
        updateButton.setForeground(Color.WHITE);
        /*
        updateButton.setFont(new Font("Arial", Font.BOLD, 14));
        updateButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 135, 0), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        updateButton.setToolTipText("Cập nhật thông tin bệnh nhân");
        updateButton.addActionListener(e -> {
            if (updateAction != null) updateAction.accept(getPatientData());
        });
        buttonPanel.add(updateButton);

        deleteButton = new JButton("Xóa");
        deleteButton.setBackground(new Color(255, 69, 0));
        deleteButton.setForeground(Color.WHITE);
        //deleteButton.setFont(new Font("Arial", Font.BOLD, 14));
        /*deleteButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 55, 0), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));*/
        deleteButton.setToolTipText("Xóa bệnh nhân đã chọn");
        deleteButton.addActionListener(e -> {
            if (deleteAction != null) deleteAction.run();
        });
        buttonPanel.add(deleteButton);

        formPanel.add(buttonPanel, gbc);
        splitPane.setTopComponent(formPanel);

        // Bottom: Diagnosis Prediction
        JPanel predictionPanel = new JPanel(new GridBagLayout());
        predictionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 1, true),
                "Dự đoán bệnh",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
        ));
        predictionPanel.setBackground(new Color(230, 240, 255));
        predictionPanel.setMinimumSize(new Dimension(300, 150));
        GridBagConstraints predGbc = new GridBagConstraints();
        predGbc.insets = new Insets(10, 10, 10, 10);
        predGbc.fill = GridBagConstraints.HORIZONTAL;

        // Diagnosis Field
        predGbc.gridx = 0; predGbc.gridy = 0; predGbc.anchor = GridBagConstraints.EAST;
        JLabel diagnosisLabel = new JLabel("Chẩn đoán:");
        diagnosisLabel.setFont(new Font("Arial", Font.BOLD, 14));
        predictionPanel.add(diagnosisLabel, predGbc);
        predGbc.gridx = 1; predGbc.anchor = GridBagConstraints.WEST;
        diagnosisField = new JTextArea(5, 15);
        diagnosisField.setLineWrap(true);
        diagnosisField.setWrapStyleWord(true);
        diagnosisField.setFont(new Font("Arial", Font.PLAIN, 14));
        diagnosisField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JScrollPane diagnosisScrollPane = new JScrollPane(diagnosisField);
        predictionPanel.add(diagnosisScrollPane, predGbc);

        // Prediction Result
        predGbc.gridx = 0; predGbc.gridy = 1; predGbc.anchor = GridBagConstraints.EAST;
        JLabel predictionLabel = new JLabel("Kết quả dự đoán:");
        predictionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        predictionPanel.add(predictionLabel, predGbc);
        predGbc.gridx = 1; predGbc.anchor = GridBagConstraints.WEST;
        predictionResultLabel = new JLabel("Chưa có kết quả");
        predictionResultLabel.setFont(new Font("Arial", Font.BOLD, 14));
        predictionResultLabel.setForeground(new Color(0, 128, 0));
        predictionResultLabel.setOpaque(true);
        predictionResultLabel.setBackground(new Color(200, 255, 200));
        predictionResultLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        predictionPanel.add(predictionResultLabel, predGbc);

        splitPane.setBottomComponent(predictionPanel);
        add(splitPane, BorderLayout.CENTER);

        // Set initial divider location
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(350));
    }

     void calculateAge() {
        LocalDate dob = dobField.getDate();
        if (dob == null) {
            ageField.setText("");
            return;
        }
        LocalDate now = LocalDate.now();
        Period period = Period.between(dob, now);
        int age = period.getYears();
        ageField.setText(String.valueOf(age));
    }

     void suggestDiagnosis() {
        String symptoms = symptomsField.getText().trim();
        SwingUtilities.invokeLater(() -> {
            if (symptoms.isEmpty()) {
                diagnosisField.setText("");
                return;
            }

            String diagnosis = predictor.predictDisease(symptoms);
            logger.debug("Predictor diagnosis: {}", diagnosis);
            if ("Không xác định được bệnh".equals(diagnosis)) {
                try (var conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT symptoms, diagnosis FROM patients WHERE department ILIKE ? " +
                                     "AND symptoms IS NOT NULL AND diagnosis IS NOT NULL GROUP BY symptoms, diagnosis")) {
                    stmt.setString(1, "%" + doctor.getDepartment() + "%");
                    ResultSet rs = stmt.executeQuery();
                    boolean found = false;
                    while (rs.next()) {
                        String dbSymptoms = rs.getString("symptoms");
                        if (dbSymptoms != null && dbSymptoms.toLowerCase().contains(symptoms.toLowerCase())) {
                            diagnosis = rs.getString("diagnosis");
                            diagnosisField.setText(diagnosis);
                            found = true;
                            logger.debug("Database diagnosis found: {}", diagnosis);
                            break;
                        }
                    }
                    if (!found) {
                        diagnosisField.setText("Cần thêm thông tin");
                        logger.debug("No matching diagnosis in database");
                    }
                } catch (SQLException e) {
                    logger.error("Database error in suggestDiagnosis: {}", e.getMessage(), e);
                    diagnosisField.setText("Lỗi truy vấn dữ liệu!");
                }
            } else {
                diagnosisField.setText(diagnosis);
            }
            diagnosisField.revalidate();
            diagnosisField.repaint();
        });
    }

     void predictDisease() {
        String symptoms = symptomsField.getText().trim();
        SwingUtilities.invokeLater(() -> {
            if (symptoms.isEmpty()) {
                predictionResultLabel.setText("Vui lòng nhập triệu chứng!");
                predictionResultLabel.setForeground(new Color(255, 0, 0));
                predictionResultLabel.setBackground(new Color(255, 200, 200));
                return;
            }

            String disease = predictor.predictDisease(symptoms);
            logger.debug("Predicted disease: {}", disease);
            predictionResultLabel.setText(disease);
            predictionResultLabel.setForeground("Cần thêm thông tin".equals(disease) ? new Color(255, 165, 0) : new Color(0, 128, 0));
            predictionResultLabel.setBackground("Cần thêm thông tin".equals(disease) ? new Color(255, 230, 180) : new Color(200, 255, 200));
            predictionResultLabel.revalidate();
            predictionResultLabel.repaint();
        });
    }

    public Patient getPatientData() {
        String fullName = fullNameField.getText().trim();
        String gender = (String) genderCombo.getSelectedItem();
        LocalDate dateOfBirth = dobField.getDate();
        String ageStr = ageField.getText().trim();
        String symptoms = symptomsField.getText().trim();
        String diagnosis = diagnosisField.getText().trim();

        int age;
        try {
            age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tuổi phải là số!");
        }

        Date dob = dateOfBirth != null ? Date.from(dateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        return new Patient(0, fullName, gender, dob, age, symptoms, diagnosis, doctor.getDepartment(), doctor.getDoctorId());
    }

    public void populateForm(String fullName, String gender, Date dateOfBirth, int age, String symptoms, String diagnosis) {
        fullNameField.setText(fullName);
        genderCombo.setSelectedItem(gender);
        LocalDate dob = dateOfBirth != null ? dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        dobField.setDate(dob);
        ageField.setText(String.valueOf(age));
        symptomsField.setText(symptoms);
        diagnosisField.setText(diagnosis);
        predictDisease();
    }

    public void clearForm() {
        fullNameField.setText("");
        genderCombo.setSelectedIndex(0);
        dobField.setDate(null);
        ageField.setText("");
        symptomsField.setText("");
        diagnosisField.setText("");
        predictionResultLabel.setText("Chưa có kết quả");
        predictionResultLabel.setForeground(new Color(0, 128, 0));
        predictionResultLabel.setBackground(new Color(200, 255, 200));
    }

    public void setAddAction(Consumer<Patient> action) {
        this.addAction = action;
    }

    public void setUpdateAction(Consumer<Patient> action) {
        this.updateAction = action;
    }

    public void setDeleteAction(Runnable action) {
        this.deleteAction = action;
    }
}