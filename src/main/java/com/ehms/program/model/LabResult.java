package com.ehms.program.model;

public class LabResult {
    int resultId , patientId;
    String testName , resultValue , testDate ;
   
    public LabResult(int resultId, int patientId, String testName, String resultValue, String testDate) {
        this.resultId = resultId;
        this.patientId = patientId;
        this.testName = testName;
        this.resultValue = resultValue;
        this.testDate = testDate;
    }

    public int getResultId() {
        return resultId;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getTestName() {
        return testName;
    }

    public String getResultValue() {
        return resultValue;
    }

    public String getTestDate() {
        return testDate;
    }
}