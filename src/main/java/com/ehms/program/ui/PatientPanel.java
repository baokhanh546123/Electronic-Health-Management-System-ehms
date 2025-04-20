package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.model.Patient; // Import Patient class
import com.ehms.program.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatientPanel extends JPanel {
    static final Logger logger = LoggerFactory.getLogger(PatientPanel.class);
    Doctor doctor;
    PatientForm form;
    PatientTable table;
    final List<PatientChangeListener> listeners = new ArrayList<>();

    public PatientPanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());

        // Create the form
        form = new PatientForm(doctor);
        form.setPreferredSize(new Dimension(300, 500));

        // Create the table
        table = new PatientTable(doctor);

        // Set up actions for the form
        form.setAddAction(this::addPatient);
        form.setUpdateAction(this::updatePatient);
        form.setDeleteAction(this::deletePatient);

        // Set up table selection action to populate the form
        table.setSelectionAction(rowData -> {
            try {
                String fullName = (String) rowData[0];
                String gender = (String) rowData[1];
                LocalDate dateOfBirth = (LocalDate) rowData[2];
                int age = (int) rowData[3];
                String symptoms = (String) rowData[4];
                String diagnosis = (String) rowData[5];
                // Convert LocalDate to Date
                Date dob = dateOfBirth != null ? Date.from(dateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
                form.populateForm(fullName, gender, dob, age, symptoms, diagnosis);
            } catch (Exception e) {
                logger.error("Error populating form: {}", e.getMessage(), e);
            }
        });

        // Main Split Pane (Left: Form, Right: Table)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.49999999);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setLeftComponent(form);
        mainSplitPane.setRightComponent(table);

        // Set initial divider location
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(350);
        });

        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void loadData() {
        logger.info("Starting loadData in PatientPanel...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.debug("Loading patients into table...");
                table.loadPatients();
                logger.debug("Patients loaded into table.");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    logger.debug("Clearing form...");
                    clearForm();
                    logger.debug("Form cleared.");
                    logger.info("Completed loadData in PatientPanel.");
                } catch (Exception e) {
                    logger.error("Error during loadData: {}", e.getMessage(), e);
                    JOptionPane.showMessageDialog(PatientPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    public void addPatientChangeListener(PatientChangeListener listener) {
        listeners.add(listener);
    }

    void notifyPatientAdded() {
        for (PatientChangeListener listener : listeners) {
            listener.onPatientAdded();
        }
    }

    void notifyPatientUpdated() {
        for (PatientChangeListener listener : listeners) {
            listener.onPatientUpdated();
        }
    }

    void notifyPatientDeleted() {
        for (PatientChangeListener listener : listeners) {
            listener.onPatientDeleted();
        }
    }

    void addPatient(Patient patient) {
        if (patient.getFullName().isEmpty() || patient.getGender() == null || patient.getDateOfBirth() == null ||
                patient.getAge() == 0 || patient.getSymptoms().isEmpty() || patient.getDiagnosis().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO patients (full_name, gender, date_of_birth, age, department, doctor_id, symptoms, diagnosis) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, patient.getFullName());
            stmt.setString(2, patient.getGender());
            stmt.setDate(3, new java.sql.Date(patient.getDateOfBirth().getTime()));
            stmt.setInt(4, patient.getAge());
            stmt.setString(5, patient.getDeparment());
            stmt.setInt(6, patient.getDoctorId());
            stmt.setString(7, patient.getSymptoms());
            stmt.setString(8, patient.getDiagnosis());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Thêm bệnh nhân thất bại, không có hàng nào được thêm!");
            }

            int newPatientId;
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newPatientId = generatedKeys.getInt(1);
                    logger.info("Đã thêm bệnh nhân với ID: {}", newPatientId);
                } else {
                    throw new SQLException("Thêm bệnh nhân thất bại, không lấy được ID!");
                }
            }

            table.loadPatients();
            clearForm();
            notifyPatientAdded();
            JOptionPane.showMessageDialog(this, "Thêm bệnh nhân thành công! ID: " + newPatientId);
        } catch (SQLException e) {
            logger.error("Error adding patient: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi thêm bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    void updatePatient(Patient patient) {
        int patientId = table.getSelectedPatientId();
        if (patientId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bệnh nhân để sửa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (patient.getFullName().isEmpty() || patient.getGender() == null || patient.getDateOfBirth() == null ||
                patient.getAge() == 0 || patient.getSymptoms().isEmpty() || patient.getDiagnosis().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE patients SET full_name = ?, gender = ?, date_of_birth = ?, age = ?, department = ?, symptoms = ?, diagnosis = ? WHERE patient_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, patient.getFullName());
            stmt.setString(2, patient.getGender());
            stmt.setDate(3, new java.sql.Date(patient.getDateOfBirth().getTime()));
            stmt.setInt(4, patient.getAge());
            stmt.setString(5, patient.getDeparment());
            stmt.setString(6, patient.getSymptoms());
            stmt.setString(7, patient.getDiagnosis());
            stmt.setInt(8, patientId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Cập nhật bệnh nhân thất bại!");
            }

            logger.info("Đã cập nhật bệnh nhân với ID: {}", patientId);
            table.loadPatients();
            clearForm();
            notifyPatientUpdated();
            JOptionPane.showMessageDialog(this, "Cập nhật bệnh nhân thành công!");
        } catch (SQLException e) {
            logger.error("Error updating patient: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    void deletePatient() {
        int patientId = table.getSelectedPatientId();
        if (patientId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bệnh nhân để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa bệnh nhân này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM patients WHERE patient_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Xóa bệnh nhân thất bại!");
            }

            logger.info("Đã xóa bệnh nhân với ID: {}", patientId);
            table.loadPatients();
            clearForm();
            notifyPatientDeleted();
            JOptionPane.showMessageDialog(this, "Xóa bệnh nhân thành công!");
        } catch (SQLException e) {
            logger.error("Error deleting patient: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi xóa bệnh nhân: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    void clearForm() {
        form.clearForm();
        table.clearSelection();
    }
}