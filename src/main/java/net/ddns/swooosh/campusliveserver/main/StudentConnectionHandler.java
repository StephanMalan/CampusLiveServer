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
    private String qualification;
    private ObjectProperty<Student> student = new SimpleObjectProperty<>();
    private ObservableList<ConnectionHandler> connectionsList;
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateStudent = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotifications = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    private DatabaseHandler dh = new DatabaseHandler();

    public StudentConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String studentNumber, ObservableList<ConnectionHandler> connectionsList) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.studentNumber = studentNumber;
        this.connectionsList = connectionsList;
        this.qualification = dh.getStudentQualification(studentNumber);
    }

    public void run() {
        updateStudent.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateStudent();
                updateStudent.set(false);
            }
        });
        updateNotices.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotices();
                updateStudent.set(false);
            }
        });
        updateNotifications.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotifications();
                updateStudent.set(false);
            }
        });
        updateContactDetails.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateContactDetails();
                updateStudent.set(false);
            }
        });
        updateImportantDates.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateImportantDates();
                updateStudent.set(false);
            }
        });
        student.addListener(e -> {
            outputQueue.add(0, student.get());
        });
        notices.addListener((InvalidationListener) e -> {
            if (!notices.isEmpty()) {
                outputQueue.add(0, Arrays.asList(notices.toArray()));
            }
        });
        updateStudent();
        updateNotices();
        updateNotifications();
        updateContactDetails();
        updateImportantDates();
        new InputProcessor().start();
        new OutputProcessor().start();
    }

    private class InputProcessor extends Thread {
        public void run() {
            while (running.get()) {
                String input;
                if ((input = getReply()) != null) {
                    if (input.startsWith("lo:")) {
                        dh.log("Student " + studentNumber + "> Requested Lecturer Online");
                        if(isLecturerOnline(input.substring(3))){
                            outputQueue.add(0, "lo:y");
                        }else{
                            outputQueue.add(0, "lo:n");
                        }
                    } else if (input.startsWith("cp:")) {
                        dh.log("Student " + studentNumber + "> Requested Change Password");
                        changePassword(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("sm:")) {
                        dh.log("Student " + studentNumber + "> Send Direct Message to Lecturer: " + input.substring(3).split(":")[1] + "> " + input.substring(3).split(":")[0]);
                        sendMessage(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("fp:")) {
                        dh.log("Student " + studentNumber + "> Requested Forgot Password");
                        forgotPassword(input.substring(3));
                    } else if (input.startsWith("gf:")) {
                        dh.log("Student " + studentNumber + "> Requested File: " + input.substring(3).split(":")[1] + " From class: " + input.substring(3).split(":")[0]);
                        getFile(input.substring(3).split(":")[0], input.substring(3).split(":")[1]);
                    } else if (input.startsWith("lgt:")) {
                        terminateConnection();
                    } else {
                        dh.log("Student " + studentNumber + "> Requested Unknown Command: " + input);
                        System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Unknown command: " + input);
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
                        dh.log("Student " + studentNumber + "> OutputProcessor> Sent: " + outputQueue.get(0));
                        System.out.println("net.ddns.swooosh.campusliveserver.main.Server> OutputProcessor> Sent: " + outputQueue.get(0));
                        outputQueue.remove(out);
                    }
                    Thread.sleep(20);
                } catch (Exception ex) {
                    dh.log("Server> OutputProcessor> " + ex);
                    ex.printStackTrace();
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
            dh.log("Server> sendData> " + ex);
            ex.printStackTrace();
        }
    }

    public String getReply() {
        try {
            String input;
            System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Waiting for reply...");
            synchronized (objectInputStream) {
                while ((input = objectInputStream.readUTF()) == null);
            }
            return  input;
        } catch (Exception ex) {
            terminateConnection();
            dh.log("Server> sendData> " + ex);
            ex.printStackTrace();
        }
        return null;
    }

    private boolean isLecturerOnline(String lecturerNumber) {
        for (ConnectionHandler ch: connectionsList) {
            if(ch instanceof LecturerConnectionHandler){
                if(((LecturerConnectionHandler) ch).getLecturerID().matches(lecturerNumber)){
                    return true;
                }
            }
        }
        return false;
    }

    private void changePassword(String prevPassword, String newPassword) {
        String sPassword = dh.getStudentPassword(studentNumber);
        if (prevPassword.matches(sPassword)) {
            dh.changeStudentPassword(studentNumber, newPassword);
            outputQueue.add(0, "cp:y");
        }else{
            outputQueue.add(0, "cp:n");
        }
    }

    private void sendMessage(String message, String lecturerNumber) {
        if(isLecturerOnline(lecturerNumber)){
            for (ConnectionHandler ch: connectionsList) {
                if(ch instanceof LecturerConnectionHandler){
                    if(((LecturerConnectionHandler) ch).getLecturerID().matches(lecturerNumber)){
                        ((LecturerConnectionHandler) ch).addMessage(message, studentNumber);
                    }
                }
            }
        }
    }

    private void forgotPassword(String email) {
        if(dh.emailStudentPassword(email, studentNumber)) {
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
                dh.log("Student " + studentNumber + "> Successfully Downloaded File: " + fileName + " For Class: " + classID);
                System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Successfully Downloaded File: " + fileName + " For Class: " + classID);
            }
        } catch (Exception ex) {
            dh.log("Server> getFile> " + ex);
            ex.printStackTrace();;
        }
    }

    private void updateStudent() {
        student.setValue(dh.getStudent(studentNumber));
    }

    private void updateNotices() {
        notices.addAll(dh.getNotices(studentNumber, qualification));
    }

    private void updateNotifications() {
        notifications.addAll(dh.getNotifications(studentNumber, qualification));
    }

    private void updateContactDetails() {
        contactDetails.addAll(dh.getContactDetails(studentNumber));
    }

    private void updateImportantDates() {
        importantDates.addAll(dh.getImportantDates(studentNumber));
    }

    public String getStudentNumber(){
        return studentNumber;
    }

    private void terminateConnection() {
        try {
            running.set(false);
            socket.close();
            connectionsList.remove(this);
        } catch (Exception ex) {
            dh.log("Server> terminateConnection> " + ex);
            ex.printStackTrace();
        }
    }

}
