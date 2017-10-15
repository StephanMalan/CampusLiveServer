package models.all;

import models.student.ClassLecturer;

import java.io.Serializable;
import java.util.List;

public class StudentClass implements Serializable {

    private int classID;
    private String moduleName;
    private String moduleNumber;
    private ClassLecturer classLecturer;
    private List<ClassTime> classTimes;
    private List<ClassFile> files;

    public StudentClass(int classID, String moduleName, String moduleNumber, ClassLecturer classLecturer, List<ClassTime> classTimes, List<ClassFile> files) {
        this.classID = classID;
        this.moduleName = moduleName;
        this.moduleNumber = moduleNumber;
        this.classLecturer = classLecturer;
        this.classTimes = classTimes;
        this.files = files;
    }

    public int getClassID() {
        return classID;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getModuleNumber() {
        return moduleNumber;
    }

    public ClassLecturer getClassLecturer() {
        return classLecturer;
    }

    public List<ClassTime> getClassTimes() {
        return classTimes;
    }

    public List<ClassFile> getFiles() {
        return files;
    }
}
