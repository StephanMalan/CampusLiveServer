package models;

import java.io.Serializable;
import java.util.List;

public class Lecturer implements Serializable{

    private String lecturerID;
    private String campus;
    private String firstName;
    private String lastName;
    private String email;
    private List<LecturerClass> classes;

    public Lecturer(String lecturerID, String campus, String firstName, String lastName, String email, List<LecturerClass> classes) {
        this.lecturerID = lecturerID;
        this.campus = campus;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.classes = classes;
    }

    public String getLecturerID() {
        return lecturerID;
    }

    public String getCampus() {
        return campus;
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

    public List<LecturerClass> getClassAndResults() {
        return classes;
    }
}
