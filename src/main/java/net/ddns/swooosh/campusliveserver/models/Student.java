package net.ddns.swooosh.campusliveserver.models;

import javafx.collections.ObservableList;

import java.io.Serializable;

public class Student implements Serializable {

    private String studentNumber;
    private String campus;
    private String qualification;
    private String FirstName;
    private String lastName;
    private String email;
    private ObservableList<ClassAndResult> classAndResults;

    public Student(String studentNumber, String campus, String qualification, String firstName, String lastName, String email, ObservableList<ClassAndResult> classAndResults) {
        this.studentNumber = studentNumber;
        this.campus = campus;
        this.qualification = qualification;
        FirstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.classAndResults = classAndResults;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getCampus() {
        return campus;
    }

    public String getQualification() {
        return qualification;
    }

    public String getFirstName() {
        return FirstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public ObservableList<ClassAndResult> getClassAndResults() {
        return classAndResults;
    }
}
