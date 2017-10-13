package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.all.ContactDetails;
import models.all.FilePart;
import models.all.ImportantDate;
import models.all.Notice;
import models.student.ClassLecturer;
import models.all.Notification;

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
    private String lecturerID;
    private ObjectProperty<ClassLecturer> lecturer = new SimpleObjectProperty<>();
    private ObservableList<ConnectionHandler> connectionsList;
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateLecturer = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotifications = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    private DatabaseHandler dh = new DatabaseHandler();

    public LecturerConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String lecturerID, ObservableList<ConnectionHandler> connectionsList) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.lecturerID = lecturerID;
        this.connectionsList = connectionsList;
    }

    public void run() {
        updateLecturer.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateLecturer();
                updateLecturer.set(false);
            }
        });
        updateNotices.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotices();
                updateLecturer.set(false);
            }
        });
        updateNotifications.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotifications();
                updateLecturer.set(false);
            }
        });
        updateContactDetails.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateContactDetails();
                updateLecturer.set(false);
            }
        });
        updateImportantDates.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateImportantDates();
                updateLecturer.set(false);
            }
        });
        lecturer.addListener(e -> {
            outputQueue.add(0, lecturer.get());
        });
        notices.addListener((InvalidationListener) e -> {
            if (!notices.isEmpty()) {
                outputQueue.add(0, Arrays.asList(notices.toArray()));
            }
        });
        updateLecturer();
        updateNotices();
        updateNotifications();
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
                    } else if (input.startsWith("lgt:")) {
                        terminateConnection();
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
        if (dh.emailLecturerPassword(email, lecturerID)) {
            outputQueue.add(0, "fp:y");
        } else {
            outputQueue.add(0, "fp:n");
        }
    }

    private void changePassword(String prevPassword, String newPassword) {
        String sPassword = dh.getLecturerPassword(lecturerID);
        if (prevPassword.matches(sPassword)) {
            dh.changePasswordLecturer(lecturerID, newPassword);
            outputQueue.add(0, "cp:y");
        } else {
            outputQueue.add(0, "cp:n");
        }
    }

    public String getLecturerID() {
        return lecturerID;
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
        //lecturer.setValue(dh.getClassLecturer(lecturerID));
    }

    private void updateNotices() {
        notices.addAll(dh.getNotices(lecturerID, "ClassLecturer"));
    }

    private void updateNotifications() {
        notifications.addAll(dh.getNotifications(lecturerID, "ClassLecturer"));
    }

    private void updateContactDetails() {
        contactDetails.addAll(dh.getContactDetails(lecturerID));
    }

    private void updateImportantDates() {
        importantDates.addAll(dh.getImportantDates(lecturerID));
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
