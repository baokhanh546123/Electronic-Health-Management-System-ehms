package com.ehms.program.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set FlatLaf look-and-feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to set look-and-feel: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Start the application (e.g., show login window)
        SwingUtilities.invokeLater(() -> {
            // Replace with your actual login window or main frame
            new LoginFrame().setVisible(true);
        });
    }
}