package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.*;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;

public class StudentConnectionHandler extends ConnectionHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String studentNumber;
    private ObjectProperty<Student> student = new SimpleObjectProperty<>();
    private ObservableList<ConnectionHandler> connectionsList;
    private ObservableList<NoticeBoard> noticeBoards = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateStudent = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNoticeBoard = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    private DatabaseHandler dh = new DatabaseHandler();

    public StudentConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String studentNumber, ObservableList<ConnectionHandler> connectionsList) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.studentNumber = studentNumber;
        this.connectionsList = connectionsList;
    }

    public void run() {
        updateStudent.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateStudent();
                updateStudent.set(false);
            }
        });
        updateNoticeBoard.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNoticeBoard();
                updateStudent.set(false);
            }
        });
        student.addListener(e -> {
            outputQueue.add(0, student.get());
        });
        noticeBoards.addListener((InvalidationListener) e -> {
            if (!noticeBoards.isEmpty()) {
                outputQueue.add(0, Arrays.asList(noticeBoards.toArray()));
            }
        });
        updateStudent();
        updateNoticeBoard();
        new InputProcessor().start();
        new OutputProcessor().start();
    }

    private class InputProcessor extends Thread {
        public void run() {
            while (running.get()) {
                String input;
                if ((input = getReply()) != null) {
                    if (input.startsWith("lo:")) {
                        if(isLecturerOnline(input.substring(3))){
                            outputQueue.add(0, "lo:y");
                        }else{
                            outputQueue.add(0, "lo:n");
                        }
                    } else if (input.startsWith("cp:")) {
                        changePassword(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("sm:")) {
                        sendMessage(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("fp:")) {
                        forgotPassword(input.substring(3));
                    } else if (input.startsWith("gf:")) {
                        getFile(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else {
                        System.out.println("Unknown command: " + input);
                    }
                }
            }
        }
    }

    private class OutputProcessor extends Thread {
        public void run() {
            while (running.get()) {
                try {
                    if (!outputQueue.isEmpty()) {
                        Object out = outputQueue.get(0);
                        sendData(out);
                        outputQueue.remove(out);
                    }
                    Thread.sleep(20);
                } catch (Exception ex) {
                    System.out.println("Server> OutputProcessor> " + ex);
                }
            }
        }
    }

    public void sendData(Object data) {
        try {
            synchronized (objectOutputStream) {
                objectOutputStream.writeObject(data);
                objectOutputStream.flush();
                objectOutputStream.reset();
            }
            System.out.println("Sent data: " + data);
        } catch (Exception ex) {
            terminateConnection();
            System.out.println("Server> sendData> " + ex);
        }
    }

    public String getReply() {
        try {
            String input;
            System.out.println("Waiting for reply...");
            synchronized (objectInputStream) {
                while ((input = objectInputStream.readUTF()) == null);
            }
            return  input;
        } catch (Exception ex) {
            terminateConnection();
            System.out.println("Server> getReply> " + ex);
        }
        return null;
    }

    private boolean isLecturerOnline(String lecturerNumber) {
        for (ConnectionHandler ch: connectionsList) {
            if(ch instanceof LecturerConnectionHandler){
                if(((LecturerConnectionHandler) ch).getLecturerNumber().matches(lecturerNumber)){
                    return true;
                }
            }
        }
        return false;
    }

    private void changePassword(String prevPassword, String newPassword) {
        String sPassword = dh.getStudentPassword(studentNumber);
        if (prevPassword.matches(sPassword)) {
            dh.changePasswordStudent(studentNumber, newPassword);
            outputQueue.add(0, "cp:y");
        }else{
            outputQueue.add(0, "cp:n");
        }
    }

    private void sendMessage(String message, String lecturerNumber) {
        if(isLecturerOnline(lecturerNumber)){
            for (ConnectionHandler ch: connectionsList) {
                if(ch instanceof LecturerConnectionHandler){
                    if(((LecturerConnectionHandler) ch).getLecturerNumber().matches(lecturerNumber)){
                        ((LecturerConnectionHandler) ch).addMessage(message, studentNumber);
                    }
                }
            }
        }
    }

    private void forgotPassword(String email) {
        if(dh.emailPassword(email)) {
            outputQueue.add(0, "fp:y");
        }else{
            outputQueue.add(0, "fp:n");
        }
    }

    private void getFile(String classID, String fileName) {
        File file = new File(Server.FILES_FOLDER.getAbsolutePath() + "/" + classID + "/" + fileName);
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            int size = 0;
            while (size < fileBytes.length) {
                System.out.println(Math.min(Server.BUFFER_SIZE, fileBytes.length - size));
                outputQueue.add(new FilePart(Arrays.copyOfRange(fileBytes, size, size + Math.min(Server.BUFFER_SIZE, fileBytes.length - size)), Integer.parseInt(classID), fileName));
                size +=  Math.min(Server.BUFFER_SIZE, fileBytes.length - size);
                System.out.println("Total size: " + size);
            }
        } catch (Exception ex) {
            System.out.println("Server> getFile> " + ex);
        }
    }

    private void updateStudent() {
        student.setValue(dh.getStudent(studentNumber));
    }

    private void updateNoticeBoard() {
        noticeBoards.addAll(dh.getNoticeBoards());
    }

    private void terminateConnection() {
        try {
            running.set(false);
            socket.close();
            connectionsList.remove(this);
        } catch (Exception ex) {
            System.out.println("Server> terminateConnection> " + ex);
        }
    }

}
