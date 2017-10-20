package net.ddns.swooosh.campusliveserver.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.EOFException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    static final File APPLICATION_FOLDER = new File(System.getProperty("user.home") + "/AppData/Local/Swooosh/CampusLive");
    static final File FILES_FOLDER = new File(APPLICATION_FOLDER.getAbsolutePath() + "/Files");
    static final File LECTURER_IMAGES = new File(APPLICATION_FOLDER.getAbsolutePath() + "/ClassLecturer");
    static final File CONTACT_IMAGES = new File(APPLICATION_FOLDER.getAbsolutePath() + "/Contact");
    static final File DATABASE_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/CampusLiveDB.db");
    static final File LOG_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/CampusLiveLogFile.txt");
    static final int BUFFER_SIZE = 4194304;
    public static ObservableList<ConnectionHandler> connectionsList = FXCollections.observableArrayList();
    public static final int PORT = 25760;
    public static final int MAX_CONNECTIONS = 500;
    public static final String DROPBOX_LOGIN = "username";
    public static final String DROPBOX_PASSWORD = "password";
    public DatabaseHandler dh = new DatabaseHandler();

    public Server() {
        if (!FILES_FOLDER.exists()) {
            FILES_FOLDER.mkdirs();
            dh.log("Server> Local Files Folder Created");
        }
        if (!LECTURER_IMAGES.exists()) {
            LECTURER_IMAGES.mkdirs();
            dh.log("Server> Local Lecturer Images Folders Created");
        }
        if (!CONTACT_IMAGES.exists()) {
            CONTACT_IMAGES.mkdirs();
            dh.log("Server> Local Contact Images Folder Created");
        }
        new ClientListener().start();
    }

    public class ClientListener extends Thread {
        @Override
        public void run() {
            try {
                dh.log("Server> Trying to set up client on port " + PORT);
                System.setProperty("javax.net.ssl.keyStore", APPLICATION_FOLDER.getAbsolutePath() + "/campuslive.store");
                System.setProperty("javax.net.ssl.keyStorePassword", "campuslivepassword1");
                dh.log("Server> Set up client on port " + PORT);
                ServerSocket ss = SSLServerSocketFactory.getDefault().createServerSocket(PORT);
                while (true) {
                    while (connectionsList.size() <= MAX_CONNECTIONS) {
                        dh.log("Server> Waiting for new connection");
                        Socket s = ss.accept();
                        s.setKeepAlive(true);
                        dh.log("Server> Connection established on " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
                        new LoginManager(s).start();
                    }
                }
            } catch (Exception ex) {
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
                    try {
                        while ((input = objectInputStream.readUTF()) == null) ;
                        System.out.println("test: " + input); //TODO
                        if (input.startsWith("saf:")) {
                            dh.log("Server> Authorising Student : " + input.substring(4).split(":")[0]);
                            if (authoriseStudent(input.substring(4).split(":")[0], input.substring(4).split(":")[1])) {
                                dh.log("Server> Authorised Student : " + input.substring(4).split(":")[0]);
                                objectOutputStream.writeObject("saf:y");
                                objectOutputStream.flush();
                                StudentConnectionHandler studentConnectionHandler = new StudentConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(4).split(":")[0], connectionsList, dh);
                                Thread t = new Thread(studentConnectionHandler);
                                t.start();
                                connectionsList.add(studentConnectionHandler);
                                break StopClass;
                            } else {
                                dh.log("Server> Authorising Student : " + input.substring(3).split(":")[0] + " Failed");
                                objectOutputStream.writeObject("saf:n");
                                objectOutputStream.flush();
                            }
                        } else if (input.startsWith("san:")) {
                            dh.log("Server> Authorising Student Off-Campus: " + input.substring(3).split(":")[0]);
                            if (authoriseStudent(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                                dh.log("Server> Authorised Student Off-Campus: " + input.substring(3).split(":")[0]);
                                objectOutputStream.writeObject("san:y");
                                objectOutputStream.flush();

                                //DropBox Details

                            /*StudentConnectionHandler studentConnectionHandler = new StudentConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList);
                            Thread t = new Thread(studentConnectionHandler);
                            t.start();
                            connectionsList.add(studentConnectionHandler);*/
                                break StopClass;
                            } else {
                                dh.log("Server> Authorising Student : " + input.substring(3).split(":")[0] + " Failed");
                                objectOutputStream.writeObject("san:n");
                                objectOutputStream.flush();
                            }
                        } else if (input.startsWith("la:")) {
                            dh.log("Server> Authorising Lecturer : " + input.substring(3).split(":")[0]);
                            if (authoriseLecturer(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                                dh.log("Server> Authorised Lecturer : " + input.substring(3).split(":")[0]);
                                objectOutputStream.writeObject("la:y");
                                objectOutputStream.flush();
                                LecturerConnectionHandler lecturerConnectionHandler = new LecturerConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList, dh);
                                Thread t = new Thread(lecturerConnectionHandler);
                                t.start();
                                connectionsList.add(lecturerConnectionHandler);
                                break StopClass;
                            } else {
                                dh.log("Server> Authorising Lecturer : " + input.substring(3).split(":")[0] + " Failed");
                                objectOutputStream.writeObject("la:n");
                                objectOutputStream.flush();
                            }
                        } else if (input.startsWith("aa:")) {
                            System.out.println("wtf?"); //TODO
                            dh.log("Server> Authorising Admin : " + input.substring(3).split(":")[0]);
                            if (authoriseAdmin(input.substring(3).split(":")[0], input.substring(3).split(":")[1])) {
                                dh.log("Server> Authorised Admin : " + input.substring(3).split(":")[0]);
                                objectOutputStream.writeObject("aa:y");
                                objectOutputStream.flush();
                                AdminConnectionHandler adminConnectionHandler = new AdminConnectionHandler(s, objectInputStream, objectOutputStream, input.substring(3).split(":")[0], connectionsList, dh);
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
                    } catch (SocketException e) {
                        dh.log("Server> User Disconnected");
                        this.stop();
                        //connectionsList.remove(this);//TODO
                    } catch (EOFException e) {
                        //System.out.println("");
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
