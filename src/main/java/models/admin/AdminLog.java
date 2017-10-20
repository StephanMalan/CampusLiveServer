package models.admin;

import java.io.Serializable;

public class AdminLog implements Serializable{

    private byte[] logFile;

    public AdminLog(byte[] logFile) {
        this.logFile = logFile;
    }

    public byte[] getLogFile() {
        return logFile;
    }
}
