package models.all;

import java.util.List;

public class Lecturer {

    private String firstName;
    private String lastName;
    private String lecturerNumber;
    private String email;
    private String contactNumber;
    private List<LecturerClass> classes;

    public Lecturer(String firstName, String lastName, String lecturerNumber, String email, String contactNumber, List<LecturerClass> classes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.lecturerNumber = lecturerNumber;
        this.email = email;
        this.contactNumber = contactNumber;
        this.classes = classes;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLecturerNumber() {
        return lecturerNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public List<LecturerClass> getClasses() {
        return classes;
    }
}
