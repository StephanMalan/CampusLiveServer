package models.all;

import java.io.Serializable;

public class ResultTemplate implements Serializable{

    private int id;
    private int classID;
    private int resultMax;
    private int dpWeight;
    private int finalWeight;
    private String resultName;

    public ResultTemplate(int id, int classID, int resultMax, int dpWeight, int finalWeight, String resultName) {
        this.id = id;
        this.classID = classID;
        this.resultMax = resultMax;
        this.dpWeight = dpWeight;
        this.finalWeight = finalWeight;
        this.resultName = resultName;
    }

    public int getId() {
        return id;
    }

    public int getClassID() {
        return classID;
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

    public String getResultName() {
        return resultName;
    }
}
