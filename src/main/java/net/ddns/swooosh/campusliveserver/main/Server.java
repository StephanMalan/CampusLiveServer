package net.ddns.swooosh.campusliveserver.main;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
//import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {

    private static final File APPLICATION_FOLDER = new File(System.getProperty("user.home") + "/AppData/Local/Swooosh/CampusLive");
    private static final File LOCAL_CACHE_FOLDER = new File(APPLICATION_FOLDER.getPath() + "/Cache");
    static final File DATABASE_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/Database/CampusLiveDB.db");
    private final List<ConnectionHandler> connectionsList = new ArrayList();
    private final int MAX_CONNECTIONS = 100;
    private final int PORT = 25760;
    private final int PORT2 = 25761;
    private Socket s;
    private final ClientListner cl = new ClientListner();
    private final DatabaseHandler dh = new DatabaseHandler();

    public Server() {
        if (!APPLICATION_FOLDER.exists()) {
            APPLICATION_FOLDER.mkdirs();
            System.out.println("Server> Application folder created");
        }
        if (!DATABASE_FILE.exists()) {
            //create DB
        } else {
            //connect DB
        }
        if (!LOCAL_CACHE_FOLDER.exists()) {
            LOCAL_CACHE_FOLDER.mkdirs();
            System.out.println("Server> Local cache folder created");
        }
        cl.start();
    }

    public class ClientListner extends Thread {

        public ClientListner() {
        }

        @Override
        public void run() {
            try {
                System.out.println("Server> Setting up server on PORT " + PORT);
                System.setProperty("javax.net.ssl.keyStore", "campuslive.store");
                System.setProperty("javax.net.ssl.keyStorePassword", "campuslivepassword1");
                System.out.println("Server> Server successfully created on PORT  " + PORT);
                ServerSocket ss = ((SSLServerSocketFactory) SSLServerSocketFactory.getDefault()).createServerSocket(PORT);
                ServerSocket rss = ((SSLServerSocketFactory) SSLServerSocketFactory.getDefault()).createServerSocket(PORT2);
                while (true) {
                    while (connectionsList.size() < MAX_CONNECTIONS) {
                        System.out.println("Server> Waiting for connection " + (connectionsList.size() + 1));
                        s = ss.accept();
                        s.setKeepAlive(true);
                        connectionsList.add(new ConnectionHandler(s, rss, connectionsList.size() + 1));
                        connectionsList.get(connectionsList.size() - 1).start();
                        System.out.println("Server> Connection established with connection " + connectionsList.size() + " on " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
                    }
                }
            } catch (IOException ex) {
                System.out.println("Server> " + ex);
            }
        }
    }

    public class ConnectionHandler extends Thread {

        private Socket s;
        private int connectionNum;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private Boolean run = true;
        private RefreshHandler rh;


        public ConnectionHandler(Socket s, ServerSocket rss, int connectionNum) {
            this.s = s;
            this.connectionNum = connectionNum;

            try {
                oos = new ObjectOutputStream(s.getOutputStream());
                ois = new ObjectInputStream(s.getInputStream());
                rh = new RefreshHandler(rss.accept(), this);
                rh.start();
            } catch (IOException ex) {
                System.out.println("Server> Connection " + connectionNum + " ERROR> " + ex);
            }
        }


            public class RefreshHandler extends Thread {

                private Socket rs;
                private ConnectionHandler ch;
                private ObjectInputStream rois;
                private ObjectOutputStream roos;

                public RefreshHandler(Socket rs, ConnectionHandler ch) {
                    this.rs = rs;
                    this.ch = ch;
                    try {
                        roos = new ObjectOutputStream(rs.getOutputStream());
                        rois = new ObjectInputStream(rs.getInputStream());
                    } catch (IOException ex) {
                        System.out.println("Server> Connection " + connectionNum + "> ERROR > " + ex);
                    }
                }

                @Override
                public void run() {
                    while (run) {
                        String command = readRefreshString();
                        System.out.println("Server> Connection " + connectionNum + "> Refresh>" + command);
                        if (command.startsWith("ref")) {

                        }
                    }
                }

                private String readRefreshString() {
                    try {
                        String input;
                        while ((input = rois.readUTF()) == null);
                        return input.trim();
                    } catch (Exception ex) {
                        if (ex.toString().contains("java.net.SocketException: Connection reset") || ex.toString().equals("java.io.EOFException")) {
                            closeConnection();
                        }
                        System.out.println("Server> readRefreshString> " + ex);
                    }
                    return null;
                }

                private void closeConnection() {
                    try {
                        System.out.println("Server> Connection " + connectionNum + "> Closing connection");
                        s.close();
                        rs.close();
                        oos.close();
                        roos.close();
                        ois.close();
                        rois.close();
                        run = false;
                        connectionsList.remove(ch);
                        updateConnectionList();
                        ch.stop();
                        this.stop();
                    } catch (Exception ex) {
                        System.out.println("Server> Connection " + connectionNum + "> ERROR>" + ex);
                    }
                }


            }

            @Override
            public void run () {
                while (run) {
                    try {
                        String command = readString();
                        System.out.println("Server> Connection " + connectionNum + "> Command> " + command);
                        if (command != null) {
                            if (command.startsWith("auth:")) {

                            } else {
                                System.out.println("Server> Connection " + connectionNum + "> Unknown command from client: '" + command + "'");
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Server> " + connectionNum + "> ERROR>" + ex);
                        connectionsList.remove(this);
                        updateConnectionList();
                        this.stop();
                    }
                }
            }

        private String readString() {
            try {
                String input;
                while ((input = ois.readUTF()) == null) ;
                return input.trim();
            } catch (Exception ex) {
                System.out.println("Server> Could not get reply from client.");
                connectionsList.remove(this);
                updateConnectionList();
                this.stop();
            }
            return null;
        }

        private void sendString(String string) {
            try {
                oos.writeUTF(string);
                oos.flush();
            } catch (IOException ex) {
                System.out.println("Server> Connection " + connectionNum + "> " + ex);
            }
        }

        private void sendObject(Object object) {
            try {
                oos.writeObject(object);
                oos.flush();
            } catch (IOException ex) {
                System.out.println("Server> Connection " + connectionNum + "> " + ex);
            }
        }

        private void updateConnectionNum(int connectionNum) {
            this.connectionNum = connectionNum;
        }

        public int getConnectionNum() {
            return connectionNum;
        }

        public Socket getSocket() {
            return s;
        }

        private void sendFile(String fileToSend) {
            try {
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " is downloading File: " + fileToSend);
                File myFile = new File(LOCAL_CACHE_FOLDER + "/" + fileToSend);
                //byte[] content = Files.readAllBytes(myFile.toPath());
                //oos.writeObject(content);
                oos.flush();
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " successfully downloaded File: " + fileToSend);
            } catch (Exception ex) {
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " failed to download File: " + fileToSend);
            }
        }

        private void recieveFile(String classID, String fileName) {
            try {
                sendString("send");
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " is uploading File: " + fileName);
                byte[] in;
                while ((in = (byte[]) ois.readObject()) == null) ;
                new File(LOCAL_CACHE_FOLDER + "/" + classID).mkdirs();
                File newFile = new File(LOCAL_CACHE_FOLDER + "/" + classID + "/" + fileName);
               // Files.write(newFile.toPath(), in);
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " successfully uploaded File: " + fileName);
            } catch (ClassNotFoundException ex) {
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " failed to upload File: " + fileName);
            } catch (IOException ex) {
                System.out.println("Server> Connection " + connectionNum + "> Connection " + connectionNum + " failed to upload File: " + fileName);
            }
        }

    }

    public void updateConnectionList() {
        int count = 1;
        for (ConnectionHandler ch : connectionsList) {
            ch.updateConnectionNum(count);
            count++;
        }
        System.out.println("Server> Waiting for connection " + (connectionsList.size() + 1));
    }

    public static void main(String[] args) {
        new Server();
    }

}
