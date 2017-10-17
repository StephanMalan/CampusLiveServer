package models.all;

import java.io.Serializable;

public class Result implements Serializable {

    private int resultTemplateID;
    private String studentNumber;
    private String resultName;
    private int result;
    private int resultMax;
    private int dpWeight;
    private int finalWeight;

    public Result(int resultTemplateID, String studentNumber, String resultName, int result, int resultMax, int dpWeight, int finalWeight) {
        this.resultTemplateID = resultTemplateID;
        this.studentNumber = studentNumber;
        this.resultName = resultName;
        this.result = result;
        this.resultMax = resultMax;
        this.dpWeight = dpWeight;
        this.finalWeight = finalWeight;
    }

    public int getResultTemplateID() {
        return resultTemplateID;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getResultName() {
        return resultName;
    }

    public int getResult() {
        return result;
    }

    public int getResultMax() {
        return resultMax;
    }

    public int getDpWeight() {
        return dpWeight;
    }

    public int getFinalWeight() {
        return finalWeight;
    }
}
