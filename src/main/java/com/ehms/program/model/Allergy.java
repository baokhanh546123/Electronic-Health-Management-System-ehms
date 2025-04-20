package com.ehms.program.model;


public class Allergy {
    int allergyId , patientId;
    String allergen , reaction , severity;

    public Allergy(int allergyId, int patientId, String allergen, String reaction, String severity) {
        this.allergyId = allergyId;
        this.patientId = patientId;
        this.allergen = allergen;
        this.reaction = reaction;
        this.severity = severity;
    }

    public int getAllergyId() {
        return allergyId;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getAllergen() {
        return allergen;
    }

    public String getReaction() {
        return reaction;
    }

    public String getSeverity() {
        return severity;
    }
}