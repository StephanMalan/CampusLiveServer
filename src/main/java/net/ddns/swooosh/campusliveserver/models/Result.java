package net.ddns.swooosh.campusliveserver.models;

import java.io.Serializable;

public class Result implements Serializable {

    private String resultName;
    private Double result;
    private Double resultMax;
    private Double resultsWeight;

    public Result(String resultName, Double result, Double resultMax, Double resultsWeight) {
        this.resultName = resultName;
        this.result = result;
        this.resultMax = resultMax;
        this.resultsWeight = resultsWeight;
    }

    public String getResultName() {
        return resultName;
    }

    public Double getResult() {
        return result;
    }

    public Double getResultMax() {
        return resultMax;
    }

    public Double getResultsWeight() {
        return resultsWeight;
    }
}
