package models;

import java.io.Serializable;

public class Notice implements Serializable{

    private String heading;
    private String description;
    private String tag;
    private String expiryDate;

    public Notice(String heading, String description, String tag, String expiryDate) {
        this.heading = heading;
        this.description = description;
        this.tag = tag;
        this.expiryDate = expiryDate;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public String getExpiryDate(){
        return expiryDate;
    }
}
