package models.lecturer;

import models.all.Attendance;

import java.util.List;

public class LecturerStudentAttendanceClass {

    private int classID;
    private List<Attendance> attendance;

    public LecturerStudentAttendanceClass(int classID, List<Attendance> attendance) {
        this.classID = classID;
        this.attendance = attendance;
    }

    public int getClassID() {
        return classID;
    }

    public List<Attendance> getAttendance() {
        return attendance;
    }
}
