package models.all;

import java.io.Serializable;
import java.util.List;

public class Student implements Serializable {

    private String studentNumber;
    private String qualification;
    private String firstName;
    private String lastName;
    private String email;
    private String contactNumber;
    private List<ClassResultAttendance> classResultAttendances;


    public Student(String studentNumber, String qualification, String firstName, String lastName, String email, String contactNumber, List<ClassResultAttendance> classResultAttendances) {
        this.studentNumber = studentNumber;
        this.qualification = qualification;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.classResultAttendances = classResultAttendances;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getQualification() {
        return qualification;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public List<ClassResultAttendance> getClassResultAttendances() {
        return classResultAttendances;
    }

}
