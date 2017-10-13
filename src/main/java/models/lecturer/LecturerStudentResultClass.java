package models.lecturer;

import models.all.Result;

import java.util.List;

public class LecturerStudentResultClass {

    private int classID;
    private List<Result> results;

    public LecturerStudentResultClass(int classID, List<Result> results) {
        this.classID = classID;
        this.results = results;
    }

    public int getClassID() {
        return classID;
    }

    public List<Result> getResults() {
        return results;
    }
}
