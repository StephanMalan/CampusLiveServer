package models.admin;

import models.all.ClassTime;

import java.util.List;

public class AdminClass {

    private int classID;
    private String moduleNumber;
    private String moduleName;
    private String lecturerNumber;
    private List<ClassTime> classTime;

    public AdminClass(int classID, String moduleNumber, String moduleName, String lecturerNumber, List<ClassTime> classTime) {
        this.classID = classID;
        this.moduleNumber = moduleNumber;
        this.moduleName = moduleName;
        this.lecturerNumber = lecturerNumber;
        this.classTime = classTime;
    }

    public int getClassID() {
        return classID;
    }

    public String getModuleNumber() {
        return moduleNumber;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getLecturerNumber() {
        return lecturerNumber;
    }

    public List<ClassTime> getClassTime() {
        return classTime;
    }
}
