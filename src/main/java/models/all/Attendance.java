package models.all;

import java.io.Serializable;

public class Attendance implements Serializable {

    private String attendanceDate;
    private String attendance;

    public Attendance(String attendanceDate, String attendance) {
        this.attendanceDate = attendanceDate;
        this.attendance = attendance;
    }

    public String getAttendanceDate() {
        return attendanceDate;
    }

    public String getAttendance() {
        return attendance;
    }
}
