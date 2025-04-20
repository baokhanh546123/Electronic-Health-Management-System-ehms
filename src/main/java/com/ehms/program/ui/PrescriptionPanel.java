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
import java.sql.Statement;

public class PrescriptionPanel extends JPanel implements PatientChangeListener {
    static final Logger logger = LoggerFactory.getLogger(PrescriptionPanel.class);
     Doctor doctor;
     PrescriptionForm form;
     PrescriptionTable table;

    public PrescriptionPanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());

        // Create the form
        form = new PrescriptionForm(doctor);

        // Create the table
        table = new PrescriptionTable(doctor);

        // Set up actions for the form
        form.setAddAction(this::addPrescription);
        form.setUpdateAction(this::updatePrescription);
        form.setDeleteAction(this::deletePrescription);

        // Set up table selection action to populate the form
        table.setSelectionAction(rowData -> {
            try {
                String patientName = (String) rowData[0];
                String medicineName = (String) rowData[1];
                String usageInstructions = (String) rowData[2];
                String dosage = (String) rowData[3];
                int quantity = (int) rowData[4]; // Now an int
                form.populateForm(patientName, medicineName, usageInstructions, dosage, quantity);
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
                mainSplitPane.setDividerLocation((int) (totalWidth * 0.3125));
            }
        });

        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void loadData() {
        logger.info("Starting loadData in PrescriptionPanel...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.debug("Loading patients into form...");
                form.loadPatientsIntoComboBox();
                logger.debug("Patients loaded into form.");

                logger.debug("Loading medicines into form...");
                form.loadMedicinesIntoComboBox();
                logger.debug("Medicines loaded into form.");

                logger.debug("Loading usage instructions into form...");
                form.loadUsageInstructions();
                logger.debug("Usage instructions loaded into form.");

                logger.debug("Loading dosages into form...");
                form.loadDosages();
                logger.debug("Dosages loaded into form.");

                logger.debug("Loading prescriptions into table...");
                table.loadPrescriptions();
                logger.debug("Prescriptions loaded into table.");

                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    logger.debug("Clearing form...");
                    clearForm();
                    logger.debug("Form cleared.");
                    logger.info("Completed loadData in PrescriptionPanel.");
                } catch (Exception e) {
                    logger.error("Error during loadData: {}", e.getMessage(), e);
                    JOptionPane.showMessageDialog(PrescriptionPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

     void addPrescription(PrescriptionForm.PrescriptionData data) {
        try {
            // This will throw IllegalArgumentException if quantity is invalid
            data = form.getPrescriptionData();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (data.patientName == null || data.medicineName == null || data.medicineName.isEmpty() ||
            data.usageInstructions == null || data.dosage == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer medicineId = form.getMedicineMap().get(data.medicineName);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (medicineId == null) {
                String sql = "SELECT medicine_id FROM medicines WHERE medicine_name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, data.medicineName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    medicineId = rs.getInt("medicine_id");
                    form.getMedicineMap().put(data.medicineName, medicineId);
                } else {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Thuốc '" + data.medicineName + "' không tồn tại. Bạn có muốn thêm thuốc mới không?",
                            "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        String medicineDosage = JOptionPane.showInputDialog(this,
                                "Nhập liều lượng cho thuốc '" + data.medicineName + "':",
                                "Nhập liều lượng", JOptionPane.PLAIN_MESSAGE);
                        if (medicineDosage == null || medicineDosage.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(this, "Liều lượng không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String sqlInsert = "INSERT INTO medicines (medicine_name, dosage) VALUES (?, ?)";
                        PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                        stmtInsert.setString(1, data.medicineName);
                        stmtInsert.setString(2, medicineDosage);
                        stmtInsert.executeUpdate();
                        ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            medicineId = generatedKeys.getInt(1);
                            logger.info("Đã thêm thuốc mới với ID: {}", medicineId);
                            form.getMedicineMap().put(data.medicineName, medicineId);
                        } else {
                            throw new SQLException("Thêm thuốc mới thất bại, không lấy được ID!");
                        }
                    } else {
                        return;
                    }
                }
            }

            String sqlPrescription = "INSERT INTO prescriptions (patient_id, medicine_id, usage_instructions, dosage, quantity) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmtPrescription = conn.prepareStatement(sqlPrescription, Statement.RETURN_GENERATED_KEYS);
            stmtPrescription.setInt(1, patientId);
            stmtPrescription.setInt(2, medicineId);
            stmtPrescription.setString(3, data.usageInstructions);
            stmtPrescription.setString(4, data.dosage);
            stmtPrescription.setInt(5, data.quantity); // Use setInt for quantity
            int affectedRows = stmtPrescription.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Thêm đơn thuốc thất bại!");
            }

            ResultSet generatedKeys = stmtPrescription.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newPrescriptionId = generatedKeys.getInt(1);
                logger.info("Đã thêm đơn thuốc với ID: {}", newPrescriptionId);
            }

            table.loadPrescriptions();
            form.loadUsageInstructions();
            form.loadMedicinesIntoComboBox();
            form.loadDosages();
            clearForm();
            JOptionPane.showMessageDialog(this, "Thêm đơn thuốc thành công!");
        } catch (SQLException e) {
            logger.error("Error adding prescription: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi thêm đơn thuốc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void updatePrescription(PrescriptionForm.PrescriptionData data) {
        int prescriptionId = table.getSelectedPrescriptionId();
        if (prescriptionId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đơn thuốc để sửa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // This will throw IllegalArgumentException if quantity is invalid
            data = form.getPrescriptionData();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (data.patientName == null || data.medicineName == null || data.medicineName.isEmpty() ||
            data.usageInstructions == null || data.dosage == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer medicineId = form.getMedicineMap().get(data.medicineName);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (medicineId == null) {
                String sql = "SELECT medicine_id FROM medicines WHERE medicine_name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, data.medicineName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    medicineId = rs.getInt("medicine_id");
                    form.getMedicineMap().put(data.medicineName, medicineId);
                } else {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Thuốc '" + data.medicineName + "' không tồn tại. Bạn có muốn thêm thuốc mới không?",
                            "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        String medicineDosage = JOptionPane.showInputDialog(this,
                                "Nhập liều lượng cho thuốc '" + data.medicineName + "':",
                                "Nhập liều lượng", JOptionPane.PLAIN_MESSAGE);
                        if (medicineDosage == null || medicineDosage.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(this, "Liều lượng không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String sqlInsert = "INSERT INTO medicines (medicine_name, dosage) VALUES (?, ?)";
                        PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                        stmtInsert.setString(1, data.medicineName);
                        stmtInsert.setString(2, medicineDosage);
                        stmtInsert.executeUpdate();
                        ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            medicineId = generatedKeys.getInt(1);
                            logger.info("Đã thêm thuốc mới với ID: {}", medicineId);
                            form.getMedicineMap().put(data.medicineName, medicineId);
                        } else {
                            throw new SQLException("Thêm thuốc mới thất bại, không lấy được ID!");
                        }
                    } else {
                        return;
                    }
                }
            }

            String sqlUpdate = "UPDATE prescriptions SET patient_id = ?, medicine_id = ?, usage_instructions = ?, dosage = ?, quantity = ? WHERE prescription_id = ?";
            PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate);
            stmtUpdate.setInt(1, patientId);
            stmtUpdate.setInt(2, medicineId);
            stmtUpdate.setString(3, data.usageInstructions);
            stmtUpdate.setString(4, data.dosage);
            stmtUpdate.setInt(5, data.quantity); // Use setInt for quantity
            stmtUpdate.setInt(6, prescriptionId);
            stmtUpdate.executeUpdate();

            table.loadPrescriptions();
            form.loadUsageInstructions();
            form.loadMedicinesIntoComboBox();
            form.loadDosages();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cập nhật đơn thuốc thành công!");
        } catch (SQLException e) {
            logger.error("Error updating prescription: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật đơn thuốc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void deletePrescription() {
        int prescriptionId = table.getSelectedPrescriptionId();
        if (prescriptionId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đơn thuốc để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa đơn thuốc này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM prescriptions WHERE prescription_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, prescriptionId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Xóa đơn thuốc thất bại!");
            }

            logger.info("Đã xóa đơn thuốc với ID: {}", prescriptionId);
            table.loadPrescriptions();
            form.loadUsageInstructions();
            form.loadMedicinesIntoComboBox();
            form.loadDosages();
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa đơn thuốc thành công!");
        } catch (SQLException e) {
            logger.error("Error deleting prescription: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi xóa đơn thuốc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

     void clearForm() {
        form.clearForm();
        table.clearSelection();
    }
}