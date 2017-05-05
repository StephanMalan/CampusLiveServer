package models;

import java.io.Serializable;
import java.util.List;

public class StudentClass implements Serializable {

    private String moduleName;
    private String moduleNumber;
    private String lecturerFirstName;
    private String lecturerLastName;
    private String lecturerNumber;
    private String lecturerEmail;
    private List<ClassTime> classTimes;
    private List<ClassFile> files;

    public StudentClass(String moduleName, String moduleNumber, String lecturerFirstName, String lecturerLastName, String lecturerNumber, String lecturerEmail, List<ClassTime> classTimes, List<ClassFile> files) {
        this.moduleName = moduleName;
        this.moduleNumber = moduleNumber;
        this.lecturerFirstName = lecturerFirstName;
        this.lecturerLastName = lecturerLastName;
        this.lecturerNumber = lecturerNumber;
        this.lecturerEmail = lecturerEmail;
        this.classTimes = classTimes;
        this.files = files;
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

    public List<ClassTime> getClassTimes() {
        return classTimes;
    }

    public List<ClassFile> getFiles() {
        return files;
    }
}
