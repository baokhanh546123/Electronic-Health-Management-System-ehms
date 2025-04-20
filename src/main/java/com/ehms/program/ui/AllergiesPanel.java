package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AllergiesPanel extends JPanel implements PatientChangeListener {
    static final Logger logger = LoggerFactory.getLogger(AllergiesPanel.class);
     Doctor doctor;
     AllergiesForm form;
     AllergiesTable table;

    public AllergiesPanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());

        // Create the form
        form = new AllergiesForm(doctor);

        // Create the table, passing the patientMap from the form
        table = new AllergiesTable(doctor, form.getPatientMap());

        // Set up actions for the form
        form.setAddAction(this::addAllergy);
        form.setUpdateAction(this::updateAllergy);
        form.setDeleteAction(this::deleteAllergy); // Added delete action


        // Set up table selection action to populate the form
        table.setSelectionAction(rowData -> {
            try {
                String patientName = (String) rowData[0];
                String allergen = (String) rowData[1];
                String reaction = (String) rowData[2];
                String severity = (String) rowData[3];
                form.populateForm(patientName, allergen, reaction, severity);
            } catch (Exception e) {
                logger.error("Error populating form: {}", e.getMessage(), e);
            }
        });

        // Main Split Pane (Left: Form, Right: Table)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setLeftComponent(form);
        mainSplitPane.setRightComponent(table);

        // Set initial divider location
        SwingUtilities.invokeLater(() -> {
            int totalWidth = mainSplitPane.getWidth();
            if (totalWidth > 0) {
                mainSplitPane.setDividerLocation((int) (totalWidth * 0.3));
            }
        });

        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void loadData() {
        logger.info("Starting loadData in AllergiesPanel...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Reload patients in the form (this updates patientCombo and patientMap)
                logger.debug("Loading patients into form...");
                form.loadPatientsIntoComboBox();
                logger.debug("Patients loaded into form.");

                // Update the filterPatientCombo in the table with the new patientMap
                logger.debug("Updating patient filter in table...");
                table.updatePatientFilter(form.getPatientMap());
                logger.debug("Patient filter updated in table.");

                // Reload allergies in the table
                logger.debug("Loading allergies into table...");
                table.loadAllergies();
                logger.debug("Allergies loaded into table.");

                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    // Clear the form to avoid stale data
                    logger.debug("Clearing form...");
                    clearForm();
                    logger.debug("Form cleared.");
                    logger.info("Completed loadData in AllergiesPanel.");
                } catch (Exception e) {
                    logger.error("Error during loadData: {}", e.getMessage(), e);
                    JOptionPane.showMessageDialog(AllergiesPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

     void addAllergy(AllergiesForm.AllergyData data) {
        if (data.patientName == null || data.allergen.isEmpty() || data.severity == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO allergies (patient_id, allergen, reaction, severity) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.allergen);
            stmt.setString(3, data.reaction);
            stmt.setString(4, data.severity);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Thêm thông tin dị ứng thất bại!");
            }

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newAllergyId = generatedKeys.getInt(1);
                logger.info("Đã thêm thông tin dị ứng với ID: {}", newAllergyId);
            }

            table.loadAllergies();
            clearForm();
            JOptionPane.showMessageDialog(this, "Thêm thông tin dị ứng thành công!");
        } catch (SQLException e) {
            logger.error("Error adding allergy: {}", e.getMessage(), e);
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                JOptionPane.showMessageDialog(this, "Lỗi: ID dị ứng đã tồn tại. Vui lòng thử lại hoặc liên hệ quản trị viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi thêm thông tin dị ứng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

     void updateAllergy(AllergiesForm.AllergyData data) {
        int allergyId = table.getSelectedAllergyId();
        if (allergyId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thông tin dị ứng để sửa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (data.patientName == null || data.allergen.isEmpty() || data.severity == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE allergies SET patient_id = ?, allergen = ?, reaction = ?, severity = ? WHERE allergy_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.allergen);
            stmt.setString(3, data.reaction);
            stmt.setString(4, data.severity);
            stmt.setInt(5, allergyId);
            stmt.executeUpdate();

            table.loadAllergies();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cập nhật thông tin dị ứng thành công!");
        } catch (SQLException e) {
            logger.error("Error updating allergy: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật thông tin dị ứng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void deleteAllergy() { // Added delete method
        int allergyId = table.getSelectedAllergyId();
        if (allergyId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thông tin dị ứng để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa thông tin dị ứng này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM allergies WHERE allergy_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, allergyId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Xóa thông tin dị ứng thất bại!");
            }

            logger.info("Đã xóa thông tin dị ứng với ID: {}", allergyId);
            table.loadAllergies();
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa thông tin dị ứng thành công!");
        } catch (SQLException e) {
            logger.error("Error deleting allergy: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi xóa thông tin dị ứng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void clearForm() {
        form.clearForm();
        table.clearSelection();
    }
}