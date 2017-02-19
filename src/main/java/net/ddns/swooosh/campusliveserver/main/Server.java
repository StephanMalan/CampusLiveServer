package net.ddns.swooosh.campusliveserver.main;

import java.io.File;

public class Server {

    private static final File APPLICATION_FOLDER = new File(System.getProperty("user.home") + "/AppData/Local/Swooosh/CampusLive");
    private static final File LOCAL_CACHE_FOLDER = new File(APPLICATION_FOLDER.getPath() + "/Cache");

}
