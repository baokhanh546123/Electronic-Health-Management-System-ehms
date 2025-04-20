package com.ehms.program.ui;

import com.ehms.program.model.Doctor;
import com.ehms.program.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LabResultsPanel extends JPanel implements PatientChangeListener {
    static final Logger logger = LoggerFactory.getLogger(LabResultsPanel.class);
    private Doctor doctor;
    private LabResultForm form;
    private LabResultTable table;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public LabResultsPanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout());

        // Create the form
        form = new LabResultForm(doctor);

        // Create the table, passing the patientMap from the form
        table = new LabResultTable(doctor, form.getPatientMap());

        // Set up actions for the form
        form.setAddAction(this::addLabResult);
        form.setUpdateAction(this::updateLabResult);
        form.setDeleteAction(this::deleteLabResult);
        

        // Set up table selection action to populate the form
        table.setSelectionAction(rowData -> {
            try {
                String patientName = (String) rowData[0];
                String testName = (String) rowData[1];
                String result = (String) rowData[2];
                String dateStr = (String) rowData[3];
                Date testDate = dateFormat.parse(dateStr);
                form.populateForm(patientName, testName, result, testDate);
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

    // Method to reload data
    public void loadData() {
        // Reload patients in the form (this updates patientCombo and patientMap)
        form.loadPatientsIntoComboBox();
        // Update the filterPatientCombo in the table with the new patientMap
        table.updatePatientFilter(form.getPatientMap());
        // Reload test names and units in the form (this updates testNameCombo and testToUnitMap)
        form.loadTestNamesIntoComboBox();
        // Reload lab results in the table
        table.loadLabResults();
        // Clear the form to avoid stale data
        clearForm();
    }

    // Implement PatientChangeListener methods
    @Override
    public void onPatientAdded() {
        loadData();
    }

    @Override
    public void onPatientUpdated() {
        loadData();
    }

    @Override
    public void onPatientDeleted() {
        loadData();
    }

    private void addLabResult(LabResultForm.LabResultData data) {
        if (data.patientName == null || data.testName.isEmpty() || data.resultValue.isEmpty() || data.testDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate test date (not in the future)
        Date today = new Date();
        if (data.testDate.after(today)) {
            JOptionPane.showMessageDialog(this, "Ngày xét nghiệm không được là ngày trong tương lai!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO lab_results (patient_id, test_name, result_value, test_date) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.testName);
            stmt.setString(3, data.resultValue);
            stmt.setDate(4, new java.sql.Date(data.testDate.getTime()));
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Thêm kết quả xét nghiệm thất bại!");
            }

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newLabResultId = generatedKeys.getInt(1);
                logger.info("Đã thêm kết quả xét nghiệm với ID: {}", newLabResultId);
            }

            table.loadLabResults();
            clearForm();
            JOptionPane.showMessageDialog(this, "Thêm kết quả xét nghiệm thành công!");
        } catch (SQLException e) {
            logger.error("Error adding lab result: {}", e.getMessage(), e);
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                JOptionPane.showMessageDialog(this, "Lỗi: ID kết quả xét nghiệm đã tồn tại. Vui lòng thử lại hoặc liên hệ quản trị viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi thêm kết quả xét nghiệm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateLabResult(LabResultForm.LabResultData data) {
        int labResultId = table.getSelectedLabResultId();
        if (labResultId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn kết quả xét nghiệm để sửa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (data.patientName == null || data.testName.isEmpty() || data.resultValue.isEmpty() || data.testDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate test date (not in the future)
        Date today = new Date();
        if (data.testDate.after(today)) {
            JOptionPane.showMessageDialog(this, "Ngày xét nghiệm không được là ngày trong tương lai!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer patientId = form.getPatientMap().get(data.patientName);
        if (patientId == null) {
            JOptionPane.showMessageDialog(this, "Bệnh nhân không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE lab_results SET patient_id = ?, test_name = ?, result_value = ?, test_date = ? WHERE result_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            stmt.setString(2, data.testName);
            stmt.setString(3, data.resultValue);
            stmt.setDate(4, new java.sql.Date(data.testDate.getTime()));
            stmt.setInt(5, labResultId);
            stmt.executeUpdate();

            table.loadLabResults();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cập nhật kết quả xét nghiệm thành công!");
        } catch (SQLException e) {
            logger.error("Error updating lab result: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật kết quả xét nghiệm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteLabResult() {
        int labResultId = table.getSelectedLabResultId();
        if (labResultId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn kết quả xét nghiệm để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa kết quả xét nghiệm này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM lab_results WHERE result_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, labResultId);
            stmt.executeUpdate();

            table.loadLabResults();
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa kết quả xét nghiệm thành công!");
        } catch (SQLException e) {
            logger.error("Error deleting lab result: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Lỗi xóa kết quả xét nghiệm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        form.clearForm();
        table.clearSelection();
    }
}