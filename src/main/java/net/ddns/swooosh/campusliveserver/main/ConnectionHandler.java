package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionHandler {

    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    Socket socket;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    ObservableList<ConnectionHandler> connectionsList;
    DatabaseHandler dh;

    public ConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, ObservableList<ConnectionHandler> connectionsList, DatabaseHandler dh) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.connectionsList = connectionsList;
        this.dh = dh;
    }

    public void terminateConnection() {
        try {
            running.set(false);
            socket.close();
            connectionsList.remove(this);
            System.out.println("Num connections: " + connectionsList.size());
        } catch (Exception ex) {
            dh.log("Server> terminateConnection> " + ex);
            ex.printStackTrace();
        }
    }

    public Object getReply() {
        try {
            Object input = null;
            while (running.get() && (input = objectInputStream.readUTF()) == null);
            return input;
        } catch (Exception ex) {
            terminateConnection();
            dh.log("Server> sendData> " + ex);
            ex.printStackTrace();
        }
        return null;
    }

}
