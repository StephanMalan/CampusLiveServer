package models;

import java.io.Serializable;
import java.util.List;

public class ClassAndResult implements Serializable {

    private StudentClass studentClass;
    private List<Result> results;

    public ClassAndResult(StudentClass studentClass, List<Result> results) {
        this.studentClass = studentClass;
        this.results = results;
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
