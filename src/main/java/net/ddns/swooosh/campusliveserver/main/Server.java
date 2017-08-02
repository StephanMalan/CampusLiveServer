package net.ddns.swooosh.campusliveserver.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static final File APPLICATION_FOLDER = new File(System.getProperty("user.home") + "/AppData/Local/Swooosh/CampusLive");
    static final File FILES_FOLDER = new File(APPLICATION_FOLDER.getAbsolutePath() + "/Files");
    static final File DATABASE_FILE = new File(APPLICATION_FOLDER.getAbsolutePath()+ "/CampusLiveDB.db");
    static final File LOG_FILE = new File(APPLICATION_FOLDER.getAbsolutePath()+ "/CampusLiveLogFile.txt");
    static final int BUFFER_SIZE = 4194304;
    private ObservableList<ConnectionHandler> connectionsList = FXCollections.observableArrayList();
    public static final int PORT = 25760;
    public static final int MAX_CONNECTIONS = 500;
    public static final String DROPBOX_LOGIN = "username";
    public static final String DROPBOX_PASSWORD = "password";
    public DatabaseHandler dh = new DatabaseHandler();

    public Server() {
        if (!FILES_FOLDER.exists()) {
            FILES_FOLDER.mkdirs();
            dh.log("Server> Local Folders Created");
            System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Local Folders Created");
        }
        new ClientListener().start();
    }

    public class ClientListener extends Thread {
        @Override
        public void run() {
            try {
                dh.log("Server> Trying to set up server on port " + PORT);
                System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Trying to set up server on port " + PORT);
                System.setProperty("javax.net.ssl.keyStore", "src/main/resources/campuslive.store");
                System.setProperty("javax.net.ssl.keyStorePassword", "campuslivepassword1");
                dh.log("Server> Set up server on port " + PORT);
                System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Set up server on port " + PORT);
                ServerSocket ss = SSLServerSocketFactory.getDefault().createServerSocket(PORT);
                while (true) {
                    while (connectionsList.size() <= MAX_CONNECTIONS) {
                        System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Waiting for new connection");
                        Socket s = ss.accept();
                        s.setKeepAlive(true);
                        dh.log("Server> Connection established on " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
                        System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Connection established on " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
                        new LoginManager(s).start();
                    }
                }
            } catch (IOException ex) {
                dh.log("Server> ClientListener> " + ex);
                ex.printStackTrace();
            }
        }
    }

    public class LoginManager extends Thread {

        private Socket s;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        public LoginManager(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {
            try {
                objectInputStream = new ObjectInputStream(s.getInputStream());
                objectOutputStream = new ObjectOutputStream(s.getOutputStream());
                StopClass:
                while (true) {
                    String input;
                    while ((input = objectInputStream.readUTF()) == null) ;
                    if (input.startsWith("sa:")) {
                        dh.log("Server> Authorising Student : " + input.substring(3).split(":")[0]);
                        if (authoriseStudent(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                            dh.log("Server> Authorisied Student : " + input.substring(3).split(":")[0]);
                            objectOutputStream.writeObject("sa:y");
                            objectOutputStream.flush();
                            StudentConnectionHandler studentConnectionHandler = new StudentConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList);
                            Thread t = new Thread(studentConnectionHandler);
                            t.start();
                            connectionsList.add(studentConnectionHandler);
                            break StopClass;
                        } else {
                            dh.log("Server> Authorising Student : " + input.substring(3).split(":")[0] + " Failed");
                            objectOutputStream.writeObject("sa:n");
                            objectOutputStream.flush();
                        }
                    } else if (input.startsWith("sao:")) {
                        dh.log("Server> Authorising Student Off-Campus: " + input.substring(3).split(":")[0]);
                        if (authoriseStudent(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                            dh.log("Server> Authorisied Student Off-Campus: " + input.substring(3).split(":")[0]);
                            objectOutputStream.writeObject("sao:y");
                            objectOutputStream.flush();
                            
                            /*StudentConnectionHandler studentConnectionHandler = new StudentConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList);
                            Thread t = new Thread(studentConnectionHandler);
                            t.start();*
                            connectionsList.add(studentConnectionHandler);*/
                            break StopClass;
                        } else {
                            dh.log("Server> Authorising Student : " + input.substring(3).split(":")[0] + " Failed");
                            objectOutputStream.writeObject("sao:n");
                            objectOutputStream.flush();
                        }
                    }else if(input.startsWith("la:")) {
                        dh.log("Server> Authorising Lecturer : " + input.substring(3).split(":")[0]);
                        if (authoriseLecturer(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                            dh.log("Server> Authorised Lecturer : " + input.substring(3).split(":")[0]);
                            objectOutputStream.writeObject("la:y");
                            objectOutputStream.flush();
                            LecturerConnectionHandler lecturerConnectionHandler = new LecturerConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList);
                            Thread t = new Thread(lecturerConnectionHandler);
                            t.start();
                            connectionsList.add(lecturerConnectionHandler);
                            break StopClass;
                        } else {
                            dh.log("Server> Authorising Lecturer : " + input.substring(3).split(":")[0] + " Failed");
                            objectOutputStream.writeObject("la:n");
                            objectOutputStream.flush();
                        }
                    }  else if(input.startsWith("aa:")) {
                        dh.log("Server> Authorising Admin : " + input.substring(3).split(":")[0]);
                        if (authoriseAdmin(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                            dh.log("Server> Authorised Admin : " + input.substring(3).split(":")[0]);
                            objectOutputStream.writeObject("aa:y");
                            objectOutputStream.flush();
                            AdminConnectionHandler adminConnectionHandler = new AdminConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList);
                            Thread t = new Thread(adminConnectionHandler);
                            t.start();
                            connectionsList.add(adminConnectionHandler);
                            break StopClass;
                        } else {
                            dh.log("Server> Authorising Admin : " + input.substring(3).split(":")[0] + " Failed");
                            objectOutputStream.writeObject("aa:n");
                            objectOutputStream.flush();
                        }
                    }

                }
            } catch (Exception ex) {
                dh.log("Server> LoginManager> " + ex);
                ex.printStackTrace();
            }
        }
    }

    private Boolean authoriseStudent(String studentNumber, String password) {
        return dh.authoriseStudent(studentNumber, password);
    }

    private Boolean authoriseLecturer(String lecturerID, String password) {
        return dh.authoriseLecturer(lecturerID, password);
    }

    private Boolean authoriseAdmin(String username, String password) {
        return dh.authoriseAdmin(username, password);
    }

    public static void main(String[] args) {
        new Server();
    }

}
