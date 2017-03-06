package net.ddns.swooosh.campusliveserver.models;

public class StudentFiles {

    private int fileID;
    private int classID;
    private String fileName;
    private int fileLength;

    public StudentFiles(int fileID, int classID, String fileName, int fileLength) {
        this.fileID = fileID;
        this.classID = classID;
        this.fileName = fileName;
        this.fileLength = fileLength;
    }

    public int getFileID() {
        return fileID;
    }

    public int getClassID() {
        return classID;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileLength() {
        return fileLength;
    }
}
