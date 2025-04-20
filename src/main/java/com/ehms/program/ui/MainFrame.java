package com.ehms.program.ui;

import com.ehms.program.model.*;
import com.ehms.program.util.*;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame(Doctor doctor) {
        setTitle("Electronic Health Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        PatientPanel patientPanel = new PatientPanel(doctor);
        PrescriptionPanel prescriptionsPanel = new PrescriptionPanel(doctor);
        MedicalHistoryPanel medicalHistoryPanel =  new MedicalHistoryPanel(doctor);
        AllergiesPanel allergiesPanel = new AllergiesPanel(doctor);
        LabResultsPanel labresultsPanel = new LabResultsPanel(doctor);
        
        // Register the PrescriptionsPanel as a listener for patient changes
        patientPanel.addPatientChangeListener(prescriptionsPanel);
        patientPanel.addPatientChangeListener(labresultsPanel);
        patientPanel.addPatientChangeListener(allergiesPanel);
        patientPanel.addPatientChangeListener(medicalHistoryPanel);
        
        tabbedPane.addTab("Bệnh nhân", patientPanel);
        tabbedPane.addTab("Dị ứng",allergiesPanel);
        tabbedPane.addTab("Kết quả xét nghiệm",labresultsPanel);
        tabbedPane.addTab("Đơn thuốc", prescriptionsPanel);
        tabbedPane.addTab("Lịch sử y tế", medicalHistoryPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
    }
}