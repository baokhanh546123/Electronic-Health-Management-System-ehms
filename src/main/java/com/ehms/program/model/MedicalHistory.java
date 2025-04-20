package com.ehms.program.model;


public class MedicalHistory {
    int historyId , patientId;
    String condition , diagnosisDate , treatment;


    public MedicalHistory(int historyId, int patientId, String condition, String diagnosisDate, String treatment) {
        this.historyId = historyId;
        this.patientId = patientId;
        this.condition = condition;
        this.diagnosisDate = diagnosisDate;
        this.treatment = treatment;
    }

    public int getHistoryId() {
        return historyId;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getCondition() {
        return condition;
    }

    public String getDiagnosisDate() {
        return diagnosisDate;
    }

    public String getTreatment() {
        return treatment;
    }
}