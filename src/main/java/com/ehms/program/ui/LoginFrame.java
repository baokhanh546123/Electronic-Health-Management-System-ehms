package com.ehms.program.ui;

import com.ehms.program.util.DatabaseConnection;
import com.ehms.program.model.Doctor;
import com.ehms.program.ui.MainFrame;


import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
     JTextField doctorIdField;
     JPasswordField passwordField;

     public LoginFrame() {
         setTitle("EHMS - Đăng nhập");
         setSize(300, 200);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setLocationRelativeTo(null);

         JPanel panel = new JPanel(new GridBagLayout());
         panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.insets = new Insets(5, 5, 5, 5); // Padding between components

         // Doctor ID
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.anchor = GridBagConstraints.EAST;
         panel.add(new JLabel("Mã bác sĩ:"), gbc);

         gbc.gridx = 1;
         gbc.gridy = 0;
         gbc.anchor = GridBagConstraints.WEST;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         doctorIdField = new JTextField(15);
         panel.add(doctorIdField, gbc);

         // Password
         gbc.gridx = 0;
         gbc.gridy = 1;
         gbc.anchor = GridBagConstraints.EAST;
         panel.add(new JLabel("Mật khẩu:"), gbc);

         gbc.gridx = 1;
         gbc.gridy = 1;
         gbc.anchor = GridBagConstraints.WEST;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         passwordField = new JPasswordField(15);
         panel.add(passwordField, gbc);

         // Login Button (Centered)
         gbc.gridx = 0;
         gbc.gridy = 2;
         gbc.gridwidth = 2; // Span across two columns
         gbc.anchor = GridBagConstraints.CENTER;
         gbc.fill = GridBagConstraints.NONE;
         JButton loginButton = new JButton("Đăng nhập");
         loginButton.addActionListener(e -> login());
         panel.add(loginButton, gbc);

         add(panel);
     }

    void login() {
        String doctorIdStr = doctorIdField.getText();
        String password = new String(passwordField.getPassword());

        if (doctorIdStr.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int doctorId;
        try {
            doctorId = Integer.parseInt(doctorIdStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Mã bác sĩ phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT d.doctor_id, d.full_name, d.department FROM doctors d " +
                         "JOIN doctor_accounts da ON d.doctor_id = da.doctor_id " +
                         "WHERE d.doctor_id = ? AND da.password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctorId);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Doctor doctor = new Doctor(rs.getInt("doctor_id"), rs.getString("full_name"), rs.getString("department"));
                dispose();
                MainFrame mainFrame = new MainFrame(doctor);
                mainFrame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Sai mã bác sĩ hoặc mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi kết nối cơ sở dữ liệu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}