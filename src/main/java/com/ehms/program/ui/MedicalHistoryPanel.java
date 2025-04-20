package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Date;

public class MedicalHistoryPanel extends JPanel implements PatientChangeListener {
    static final Logger logger = LoggerFactory.getLogger(MedicalHistoryPanel.class);
     Doctor doctor;
     MedicalHistoryForm form;
     MedicalHistoryTable table;

    public MedicalHistoryPanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());

        // Create the form
        form = new MedicalHistoryForm(doctor);
        form.setPreferredSize(new Dimension(400, form.getPreferredSize().height));

        // Create the table
        table = new MedicalHistoryTable(doctor);

        // Set up actions for the form
        form.setAddAction(this::addMedicalHistory);
        form.setUpdateAction(this::updateMedicalHistory);
        form.setDeleteAction(this::deleteMedicalHistory);
        form.setClearAction(this::clearForm);

        // Set up table selection action to populate the form
        table.setSelectionAction(rowData -> {
            try {
                String patientName = (String) rowData[0];
                String condition = (String) rowData[1];
                LocalDate diagnosisDate = (LocalDate) rowData[2]; // Now LocalDate
                String treatment = (String) rowData[3];
                form.populateForm(patientName, condition, diagnosisDate, treatment);
            } catch (Exception e) {
                logger.error("Error populating form: {}", e.getMessage(), e);
            }
        });

        // Main Split Pane (Left: Form, Right: Table)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3125);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setLeftComponent(form);
        mainSplitPane.setRightComponent(table);

        // Set initial divider location
        SwingUtilities.invokeLater(() -> {
            int totalWidth = mainSplitPane.getWidth();
            if (totalWidth > 0) {
                mainSplitPane.setDividerLocation((int) (totalWidth * 0.3125	));
            }
        });

        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void loadData() {
        logger.info("Starting loadData in MedicalHistoryPanel...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.debug("Loading patient names into form...");
                form.loadPatientNames();
                logger.debug("Patient names loaded into form.");

                logger.debug("Loading treatments into form...");
                form.loadTreatments();
                logger.debug("Treatments loaded into form.");

                logger.debug("Loading medical history into table...");
                table.loadMedicalHistory();
                logger.debug("Medical history loaded into table.");

                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    logger.debug("Clearing form...");
                    clearForm();
                    logger.debug("Form cleared.");
                    logger.info("Completed loadData in MedicalHistoryPanel.");
                } catch (Exception e) {
                    logger.error("Error during loadData: {}", e.getMessage(), e);
                    JOptionPane.showMessageDialog(MedicalHistoryPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    @Override
    public void onPatientAdded() {
        logger.info("Patient added event received, reloading data...");
        loadData();
    }

    @Override
    public void onPatientUpdated() {
        logger.info("Patient updated event received, reloading data...");
        loadData();
    }

    @Override
    public void onPatientDeleted() {
        logger.info("Patient deleted event received, reloading data...");
        loadData();
    }

     void addMedicalHistory(MedicalHistoryForm.MedicalHistoryData data) {
        if (data.patientName == null || data.condition.isEmpty() || data.diagnosisDate == null || data.treatment == null || data.treatment.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO medical_history (patient_id, condition, diagnosis_date, treatment) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.condition);
            stmt.setDate(3, Date.valueOf(data.diagnosisDate)); // Convert LocalDate to java.sql.Date
            stmt.setString(4, data.treatment);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Thêm lịch sử y tế thất bại!");
            }

            logger.info("Đã thêm lịch sử y tế cho bệnh nhân: {}", data.patientName);
            table.loadMedicalHistory();
            form.loadTreatments();
            clearForm();
            JOptionPane.showMessageDialog(this, "Thêm lịch sử y tế thành công!");
        } catch (SQLException e) {
            logger.error("Error adding medical history: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi thêm lịch sử y tế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void updateMedicalHistory(MedicalHistoryForm.MedicalHistoryData data) {
        int historyId = table.getSelectedHistoryId();
        if (historyId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch sử y tế để sửa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (data.patientName == null || data.condition.isEmpty() || data.diagnosisDate == null || data.treatment == null || data.treatment.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_history SET patient_id = ?, condition = ?, diagnosis_date = ?, treatment = ? WHERE history_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.condition);
            stmt.setDate(3, Date.valueOf(data.diagnosisDate)); // Convert LocalDate to java.sql.Date
            stmt.setString(4, data.treatment);
            stmt.setInt(5, historyId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Cập nhật lịch sử y tế thất bại!");
            }

            logger.info("Đã cập nhật lịch sử y tế với ID: {}", historyId);
            table.loadMedicalHistory();
            form.loadTreatments();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cập nhật lịch sử y tế thành công!");
        } catch (SQLException e) {
            logger.error("Error updating medical history: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật lịch sử y tế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void deleteMedicalHistory() {
        int historyId = table.getSelectedHistoryId();
        if (historyId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch sử y tế để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa lịch sử y tế này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM medical_history WHERE history_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, historyId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Xóa lịch sử y tế thất bại!");
            }

            logger.info("Đã xóa lịch sử y tế với ID: {}", historyId);
            table.loadMedicalHistory();
            form.loadTreatments();
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa lịch sử y tế thành công!");
        } catch (SQLException e) {
            logger.error("Error deleting medical history: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi xóa lịch sử y tế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void clearForm() {
        form.clearForm();
        table.clearSelection();
    }
}