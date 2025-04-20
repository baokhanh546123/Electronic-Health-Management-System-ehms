package com.ehms.program.model;

import java.util.Date ; 

public class Doctor {
	int doctorId , age , hierarchy, salary , prize ;
	String fullName , gender , qualification , position , department ;
	Date dateOfBirth , employmentStartDate , employmentEndDate ,lastHierarchyIncreaseDate;
	
	
	public Doctor(int doctorId , String fullName ) {
		this.doctorId = doctorId ;
		this.fullName = fullName;
	}
	
	public Doctor(int doctorId , String fullName , String deparment) {
		this.doctorId = doctorId ;
		this.fullName = fullName;
		this.department = deparment;
	}
	
	public Doctor(int doctorId, String fullName, String gender, Date dateOfBirth, int age,
            String qualification, String position, String department, int hierarchy,
            int salary, int prize, Date employmentStartDate, Date employmentEndDate,
            Date lastHierarchyIncreaseDate) {
			  this.doctorId = doctorId;
			  this.fullName = fullName;
			  this.gender = gender;
			  this.dateOfBirth = dateOfBirth;
			  this.age = age;
			  this.qualification = qualification;
			  this.position = position;
			  this.department = department;
			  this.hierarchy = hierarchy;
			  this.salary = salary;
			  this.prize = prize;
			  this.employmentStartDate = employmentStartDate;
			  this.employmentEndDate = employmentEndDate;
			  this.lastHierarchyIncreaseDate = lastHierarchyIncreaseDate;
}
	public int getDoctorId() {
        return doctorId;
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

    public String getQualification() {
        return qualification;
    }

    public String getPosition() {
        return position;
    }

    public String getDepartment() {
        return department;
    }

    public int getHierarchy() {
        return hierarchy;
    }

    public int getSalary() {
        return salary;
    }

    public int getPrize() {
        return prize;
    }

    public Date getEmploymentStartDate() {
        return employmentStartDate;
    }

    public Date getEmploymentEndDate() {
        return employmentEndDate;
    }

    public Date getLastHierarchyIncreaseDate() {
        return lastHierarchyIncreaseDate;
    }
	
}