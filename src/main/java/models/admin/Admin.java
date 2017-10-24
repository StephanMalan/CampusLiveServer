package models.admin;

import java.io.Serializable;

public class Admin implements Serializable{

    private String adminName;
    private String adminEmail;

    public Admin(String adminName, String adminEmail) {
        this.adminName = adminName;
        this.adminEmail = adminEmail;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }
}
