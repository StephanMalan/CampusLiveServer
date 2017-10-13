package models.admin;

public class AdminLog {

    private byte[] logFile;

    public AdminLog(byte[] logFile) {
        this.logFile = logFile;
    }

    public byte[] getLogFile() {
        return logFile;
    }
}
