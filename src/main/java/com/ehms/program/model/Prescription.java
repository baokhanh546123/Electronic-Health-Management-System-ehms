package com.ehms.program.model;

public class Prescription {
    int prescriptionId , patientId , medicineId , quantity;
    String medicineName , usage_instructions;
    

    public Prescription(int prescriptionId, int patientId, int medicineId, int quantity, 
    		String medicineName, String usage_instructions) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.medicineName = medicineName;
        this.usage_instructions = usage_instructions;
    }

    public int getPrescriptionId() {
        return prescriptionId;
    }

    public int getPatientId() {
        return patientId;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getInstructions() {
        return usage_instructions;
    }
}