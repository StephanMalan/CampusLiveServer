package models.all;

import java.io.Serializable;
import java.util.Date;

public class Attendance implements Serializable {

    private String aDate;
    private String attendance;

    public Attendance(String aDate, String attendance) {
        this.aDate = aDate;
        this.attendance = attendance;
    }

    public String getDate() {
        return aDate;
    }

    public String getAttendance() {
        return attendance;
    }
}
