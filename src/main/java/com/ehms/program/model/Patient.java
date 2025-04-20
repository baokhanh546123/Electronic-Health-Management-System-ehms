package com.ehms.program.model;

import java.util.Date;

public class Patient {
	int patientId , age , doctorId ;
	String fullName , gender , department , symptoms , diagnosis; 
	Date dateOfBirth;
	
    public Patient(int patientId, String fullName, String gender, Date dateOfBirth, 
    		int age , String symptoms , String diagnosis, String department, int doctorId) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.department = department ; 
        this.doctorId = doctorId;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public int getAge() {
        return age;
    }
    
    public String getSymptoms() {
    	return symptoms;
    }
    
    public String getDiagnosis() {
    	return diagnosis;
    }

    public String getDeparment() {
        return department;
    }

    public int getDoctorId() {
        return doctorId;
    }
}
