package models.admin;

public class AdminSearch {

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
}
