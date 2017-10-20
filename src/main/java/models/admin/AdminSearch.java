package models.admin;

import java.io.Serializable;

public class AdminSearch implements Serializable{

    private String type; //student, lecturer, class, contact
    private String primaryText;
    private String secondaryText;

    public AdminSearch(String type, String primaryText, String secondaryText) {
        this.type = type;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
    }

    public String getType() {
        return type;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    @Override
    public String toString() {
        return primaryText + " - " + secondaryText;
    }
}
