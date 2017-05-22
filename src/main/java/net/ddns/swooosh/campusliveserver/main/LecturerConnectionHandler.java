package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.FilePart;
import models.NoticeBoard;
import models.Lecturer;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class LecturerConnectionHandler extends ConnectionHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String lecturerNumber;
    private ObjectProperty<Lecturer> lecturer = new SimpleObjectProperty<>();
    private ObservableList<ConnectionHandler> connectionsList;
    private ObservableList<NoticeBoard> noticeBoards = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateLecturer = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNoticeBoard = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    private DatabaseHandler dh = new DatabaseHandler();

    public LecturerConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String lecturerNumber, ObservableList<ConnectionHandler> connectionsList) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.lecturerNumber = lecturerNumber;
        this.connectionsList = connectionsList;
    }

    public void run() {
        updateLecturer.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateLecturer();
                updateLecturer.set(false);
            }
        });
        updateNoticeBoard.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNoticeBoard();
                updateLecturer.set(false);
            }
        });
        lecturer.addListener(e -> {
            outputQueue.add(0, lecturer.get());
        });
        noticeBoards.addListener((InvalidationListener) e -> {
            if (!noticeBoards.isEmpty()) {
                outputQueue.add(0, Arrays.asList(noticeBoards.toArray()));
            }
        });
        updateLecturer();
        updateNoticeBoard();
        new InputProcessor().start();
        new OutputProcessor().start();
    }

    private class InputProcessor extends Thread {
        public void run() {
            while (running.get()) {
                String input;
                if ((input = getReply()) != null) {
                    if (input.startsWith("cp:")) {
                        changePassword(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("fp:")) {
                        forgotPassword(input.substring(3));
                    } else if (input.startsWith("gf:")) {
                        getFile(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("uf:")) {
                        uploadFile(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
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
                while ((input = objectInputStream.readUTF()) == null) ;
            }
            return input;
        } catch (Exception ex) {
            terminateConnection();
            System.out.println("Server> getReply> " + ex);
        }
        return null;
    }

    public void addMessage(String message, String studentName) {
        outputQueue.add(0, "sm:" + message + ":" + studentName);
    }

    private void forgotPassword(String email) {
        if (dh.emailLecturerPassword(email, lecturerNumber)) {
            outputQueue.add(0, "fp:y");
        } else {
            outputQueue.add(0, "fp:n");
        }
    }

    private void changePassword(String prevPassword, String newPassword) {
        String sPassword = dh.getLecturerPassword(lecturerNumber);
        if (prevPassword.matches(sPassword)) {
            dh.changePasswordLecturer(lecturerNumber, newPassword);
            outputQueue.add(0, "cp:y");
        } else {
            outputQueue.add(0, "cp:n");
        }
    }

    public String getLecturerNumber() {
        return lecturerNumber;
    }

    private void getFile(String classID, String fileName) {
        File file = new File(Server.FILES_FOLDER.getAbsolutePath() + "/" + classID + "/" + fileName);
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            int size = 0;
            while (size < fileBytes.length) {
                System.out.println(Math.min(Server.BUFFER_SIZE, fileBytes.length - size));
                outputQueue.add(new FilePart(Arrays.copyOfRange(fileBytes, size, size + Math.min(Server.BUFFER_SIZE, fileBytes.length - size)), Integer.parseInt(classID), fileName));
                size += Math.min(Server.BUFFER_SIZE, fileBytes.length - size);
                System.out.println("Total size: " + size);
                dh.log("");
            }
        } catch (Exception ex) {
            System.out.println("Server> getFile> " + ex);
        }
    }

    private void uploadFile(String classID, String fileName) {
        //TODO
        updateStudents(classID);
        updateLecturer.setValue(true);
    }

    private boolean updateStudents(String classID) {
        List<String> students = dh.getStudentsInClass(Integer.parseInt(classID));
        for (ConnectionHandler ch : connectionsList) {
            if (ch instanceof StudentConnectionHandler) {
                for (int i = 0; i < students.size(); i++) {
                    if (((StudentConnectionHandler) ch).getStudentNumber().matches(students.get(i))) {
                        ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                        i = students.size();
                    }
                }
            }
        }
        return true;
    }

    private void updateLecturer() {
        lecturer.setValue(dh.getLecturer(lecturerNumber));
    }

    private void updateNoticeBoard() {
        noticeBoards.addAll(dh.getNoticeBoards(lecturerNumber));
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
