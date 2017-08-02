package models;

import java.io.Serializable;
import java.util.Date;

public class ImportantDate implements Serializable {

    private Date date;
    private String description;

    public ImportantDate(Date date, String description) {
        this.date = date;
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

}
