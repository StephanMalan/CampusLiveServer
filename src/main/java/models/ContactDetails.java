package models;

import java.io.Serializable;

public class ContactDetails implements Serializable {

    private String name;
    private String position;
    private String department;
    private String contactNumber;
    private String email;

    public ContactDetails(String name, String position, String department, String contactNumber, String email) {
        this.name = name;
        this.position = position;
        this.department = department;
        this.contactNumber = contactNumber;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getDepartment() {
        return department;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }
}
