package models.admin;

import java.io.Serializable;

public class Admin implements Serializable{

    private String adminName;

    public Admin(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminName() {
        return adminName;
    }
}
