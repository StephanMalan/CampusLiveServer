package models;

import java.io.Serializable;

public class NoticeBoard implements Serializable{

    private String heading;
    private String description;

    public NoticeBoard(String heading, String description) {
        this.heading = heading;
        this.description = description;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }
}
