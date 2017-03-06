package net.ddns.swooosh.campusliveserver.models;

import javafx.collections.ObservableList;

import java.io.Serializable;

public class StudentClass implements Serializable {

    private String moduleName;
    private String moduleNumber;
    private String lecturerFirstName;
    private String lecturerLastName;
    private String lecturerNumber;
    private String lecturerEmail;
    private ObservableList<ClassTime> classTimes;

    public StudentClass(String moduleName, String moduleNumber, String lecturerFirstName, String lecturerLastName, String lecturerNumber, String lecturerEmail, ObservableList<ClassTime> classTimes) {
        this.moduleName = moduleName;
        this.moduleNumber = moduleNumber;
        this.lecturerFirstName = lecturerFirstName;
        this.lecturerLastName = lecturerLastName;
        this.lecturerNumber = lecturerNumber;
        this.lecturerEmail = lecturerEmail;
        this.classTimes = classTimes;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getModuleNumber() {
        return moduleNumber;
    }

    public String getLecturerFirstName() {
        return lecturerFirstName;
    }

    public String getLecturerLastName() {
        return lecturerLastName;
    }

    public String getLecturerNumber() {
        return lecturerNumber;
    }

    public String getLecturerEmail() {
        return lecturerEmail;
    }

    public ObservableList<ClassTime> getClassTimes() {
        return classTimes;
    }
}
