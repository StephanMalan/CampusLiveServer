package net.ddns.swooosh.campusliveserver.models;

import javafx.collections.ObservableList;

import java.io.Serializable;

public class ClassAndResult implements Serializable {

    private StudentClass studentClass;
    private ObservableList<Result> results;

    public ClassAndResult(StudentClass studentClass, ObservableList<Result> results) {
        this.studentClass = studentClass;
        this.results = results;
    }

    public StudentClass getStudentClass() {
        return studentClass;
    }

    public ObservableList<Result> getResults() {
        return results;
    }
}
