package models.all;

import java.io.Serializable;

public class Result implements Serializable {

    private String resultName;
    private Double result;
    private Double resultMax;
    private Double dpWeight;
    private Double finalWeight;

    public Result(String resultName, Double result, Double resultMax, Double dpWeight, Double finalWeight) {
        this.resultName = resultName;
        this.result = result;
        this.resultMax = resultMax;
        this.dpWeight = dpWeight;
        this.finalWeight = finalWeight;
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

    public Double getDpWeight() {
        return dpWeight;
    }

    public Double getFinalWeight() {
        return finalWeight;
    }
}
