package models;

import java.io.Serializable;

public class Notification implements Serializable{

    private String heading;
    private String description;
    private String tag;

    public Notification(String heading, String description, String tag) {
        this.heading = heading;
        this.description = description;
        this.tag = tag;
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

}
