package models;

import java.io.Serializable;
import java.util.Date;

public class Attendance implements Serializable {

    private Date date;
    private String attendance;

    public Attendance(Date date, String attendance) {
        this.date = date;
        this.attendance = attendance;
    }

    public Date getDate() {
        return date;
    }

    public String getAttendance() {
        return attendance;
    }
}
