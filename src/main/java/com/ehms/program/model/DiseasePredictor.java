package com.ehms.program.model;

import com.ehms.program.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiseasePredictor {
     final Map<String, List<String>> symptomToDisease = new HashMap<>();
     final String doctorDepartment;

    public DiseasePredictor(String doctorDepartment) {
        this.doctorDepartment = doctorDepartment;
        loadSymptomToDiseaseMapping();
    }

     void loadSymptomToDiseaseMapping() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT symptoms, diagnosis FROM patients " +
                         "WHERE department ILIKE ? AND symptoms IS NOT NULL AND diagnosis IS NOT NULL";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + doctorDepartment + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String symptoms = rs.getString("symptoms");
                String diagnosis = rs.getString("diagnosis");
                if (symptoms != null && !symptoms.trim().isEmpty() && diagnosis != null && !diagnosis.trim().isEmpty()) {
                    String key = symptoms.trim().toLowerCase(); // Case-insensitive key
                    symptomToDisease.computeIfAbsent(key, k -> new ArrayList<>()).add(diagnosis.trim());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Lỗi tải dữ liệu triệu chứng và chẩn đoán từ cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public String predictDisease(String symptoms) {
        if (symptoms == null || symptoms.trim().isEmpty()) {
            return "Vui lòng nhập triệu chứng!";
        }

        String symptomKey = symptoms.trim().toLowerCase();
        List<String> diagnoses = symptomToDisease.get(symptomKey);
        if (diagnoses != null && !diagnoses.isEmpty()) {
            // Return the first diagnosis (or implement logic to handle multiple diagnoses)
            return diagnoses.get(0);
        }

        // Fallback: Query the database for patients in the doctor's department
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT symptoms, diagnosis FROM patients " +
                         "WHERE department ILIKE ? AND symptoms IS NOT NULL AND diagnosis IS NOT NULL " +
                         "GROUP BY symptoms, diagnosis";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + doctorDepartment + "%");
            ResultSet rs = stmt.executeQuery();
            Map<String, String> fallbackMapping = new HashMap<>();
            while (rs.next()) {
                String dbSymptoms = rs.getString("symptoms").trim();
                String dbDiagnosis = rs.getString("diagnosis").trim();
                String dbSymptomKey = dbSymptoms.toLowerCase();
                if (dbSymptomKey.equals(symptomKey)) {
                    return dbDiagnosis; // Exact match found
                }
                fallbackMapping.put(dbSymptomKey, dbDiagnosis);
            }

            // Check for partial matches
            for (Map.Entry<String, String> entry : fallbackMapping.entrySet()) {
                if (entry.getKey().contains(symptomKey)) {
                    return entry.getValue();
                }
            }
            return "Cần thêm thông tin";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage();
        }
    }
}