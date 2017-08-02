package models;

import java.io.Serializable;
import java.util.List;

public class ClassResultAttendance implements Serializable {

    private StudentClass studentClass;
    private List<Result> results;
    private List<Attendance> attendance;

    public ClassResultAttendance(StudentClass studentClass, List<Result> results, List<Attendance> attendance) {
        this.studentClass = studentClass;
        this.results = results;
        this.attendance = attendance;
    }

    public StudentClass getStudentClass() {
        return studentClass;
    }

    public List<Result> getResults() {
        return results;
    }

    public String toString() {
        return studentClass.getModuleName();
    }
}
