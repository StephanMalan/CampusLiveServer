package net.ddns.swooosh.campusliveserver.main;

import java.io.Serializable;

public class OnlineDownload implements Serializable {

    private int classID;
    private String fileName;
    private String dropboxURL;

    public OnlineDownload(int classID, String fileName, String dropboxURL) {
        this.classID = classID;
        this.fileName = fileName;
        this.dropboxURL = dropboxURL;
    }

    public int getClassID() {
        return classID;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDropboxURL() {
        return dropboxURL;
    }
}
