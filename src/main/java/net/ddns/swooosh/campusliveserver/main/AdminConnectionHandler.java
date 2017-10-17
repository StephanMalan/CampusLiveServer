package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.admin.Admin;
import models.admin.AdminClass;
import models.admin.AdminLog;
import models.all.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;

public class AdminConnectionHandler extends ConnectionHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;
    private ObservableList<ConnectionHandler> connectionsList;
    private ObservableList<Admin> admins = FXCollections.observableArrayList();
    private ObservableList<Student> students = FXCollections.observableArrayList();
    private ObservableList<Lecturer> lecturers = FXCollections.observableArrayList();
    private ObservableList<AdminClass> classes = FXCollections.observableArrayList();
    private ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateAdmins = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateStudents = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateLecturers = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateClasses = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotifications = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    private DatabaseHandler dh = new DatabaseHandler();

    public AdminConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String username, ObservableList<ConnectionHandler> connectionsList) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.username = username;
        this.connectionsList = connectionsList;
    }

    public void run() {
        updateAdmins.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateAdmins();
                updateAdmins.set(false);
            }
        });
        updateStudents.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateStudents();
                updateStudents.set(false);
            }
        });
        updateLecturers.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateLecturers();
                updateLecturers.set(false);
            }
        });
        updateClasses.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateClasses();
                updateClasses.set(false);
            }
        });
        updateContactDetails.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateContactDetails();
                updateContactDetails.set(false);
            }
        });
        updateNotices.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotices();
                updateNotices.set(false);
            }
        });
        updateNotifications.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateNotifications();
                updateNotifications.set(false);
            }
        });
        updateImportantDates.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateImportantDates();
                updateImportantDates.set(false);
            }
        });
        updateAdmins();
        updateClasses();
        updateContactDetails();
        updateImportantDates();
        updateLecturers();
        updateNotices();
        updateNotifications();
        updateStudents();
        new InputProcessor().start();
        new OutputProcessor().start();
    }

    private class InputProcessor extends Thread {
        public void run() {
            while (running.get()) {
                Object input;
                if ((input = getReply()) != null) {
                    if(input instanceof String) {
                        String text = input.toString();
                        if (text.startsWith("asd:")) {
                            outputQueue.add(dh.getStudent(text.substring(4)));
                        } else if(text.startsWith("ald:")){
                            outputQueue.add(dh.getLecturer(text.substring(4)));
                        } else if(text.startsWith("acd:")){
                            outputQueue.add(dh.getClass(Integer.parseInt(text.substring(4))));
                        } else if(text.startsWith("asd:")){
                            try {
                                outputQueue.add(new AdminLog(Files.readAllBytes(Server.LOG_FILE.toPath())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if(text.startsWith("res:")){
                            dh.updateResult(Integer.parseInt(text.split(":")[1]), text.split(":")[2], Integer.parseInt(text.split(":")[3]));
                        } else if(text.startsWith("aci:")){
                            outputQueue.add(dh.getContactDetailsDetail(text.substring(4)));
                        } else if (text.startsWith("lgt:")) {
                            terminateConnection();
                        } else if (text.startsWith("rs:")){//student
                            dh.removeStudent(text.substring(3));
                        } else if (text.startsWith("rl:")){//lecturer
                            dh.removeLecturer(text.substring(3));
                        } else if (text.startsWith("rc:")){//class
                            dh.removeClass(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rr:")){//resultTemplate
                            dh.removeResultTemplate(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rn:")){//notice
                            dh.removeNotice(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rf:")){//notification
                            dh.removeNotification(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rd:")){//contact details
                            dh.removeContactDetails(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("ri:")){//important dates
                            dh.removeImportantDate(Integer.parseInt(text.substring(3)));
                        } else {
                            dh.log("Admin " + username + "> Requested Unknown Command: " + input);
                            System.out.println("Unknown command: " + input);
                        }
                    } else if (input instanceof Student){
                        Student uStudent = (Student) input;
                        dh.updateStudent(uStudent.getStudentNumber(), uStudent.getQualification(), uStudent.getFirstName(), uStudent.getLastName(), uStudent.getEmail(), uStudent.getContactNumber(), uStudent.getClassResultAttendances());
                    } else if (input instanceof Lecturer){
                        Lecturer uLecturer = (Lecturer) input;
                        dh.updateLecturer(uLecturer.getLecturerNumber(), uLecturer.getFirstName(), uLecturer.getLastName(), uLecturer.getEmail(), uLecturer.getContactNumber());
                    } else if (input instanceof Class){
                        AdminClass uClass = (AdminClass) input;
                        dh.updateClass(uClass.getClassID(), uClass.getModuleName(), uClass.getModuleNumber(), uClass.getLecturerNumber(), uClass.getClassTime());
                    } else if (input instanceof Notification){
                        Notification uNotification = (Notification) input;
                        dh.updateNotification(uNotification.getId(), uNotification.getHeading(), uNotification.getDescription(), uNotification.getTag());
                    } else if (input instanceof Notification){
                        Notice uNotice = (Notice) input;
                        dh.updateNotice(uNotice.getId(), uNotice.getHeading(), uNotice.getDescription(), uNotice.getExpiryDate(), uNotice.getTag());
                    } else if (input instanceof Result){
                        Result uResult = (Result) input;
                        dh.updateResult(uResult.getResultTemplateID(), uResult.getStudentNumber(), uResult.getResult());
                    } else if (input instanceof ResultTemplate){//-1 id = new
                        ResultTemplate uResultTemplate = (ResultTemplate) input;
                        dh.updateResultTemplate(uResultTemplate.getId(), uResultTemplate.getClassID(), uResultTemplate.getResultMax(), uResultTemplate.getDpWeight(), uResultTemplate.getFinalWeight(), uResultTemplate.getResultName());
                    } else if(input instanceof ContactDetails){
                        ContactDetails nContactDetails = (ContactDetails) input;
                        dh.updateContactDetails(nContactDetails.getId(), nContactDetails.getName(), nContactDetails.getPosition(), nContactDetails.getDepartment(), nContactDetails.getContactNumber(), nContactDetails.getEmail());
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
                        dh.log("Admin " + username + "> OutputProcessor> Sent: " + outputQueue.get(0));
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

    public Object getReply() {
        try {
            Object input;
            System.out.println("Waiting for reply...");
            synchronized (objectInputStream) {
                while ((input = objectInputStream.readUTF()) == null);
            }
            return input;
        } catch (Exception ex) {
            terminateConnection();
            dh.log("Server> sendData> " + ex);
            ex.printStackTrace();
        }
        return null;
    }

    private void updateAdmins() {
        admins.setAll(dh.getAllAdmins());
    }

    private void updateStudents() {
        students.setAll(dh.getAllStudents());
    }

    private void updateLecturers() {
        lecturers.setAll(dh.getAllLecturers());
    }

    private void updateClasses() {
        classes.setAll(dh.getAllClasses());
    }

    private void updateContactDetails() {
        contactDetails.setAll(dh.getContactDetails());
    }

    private void updateNotices() {
        notices.setAll(dh.getAllNotices());
    }

    private void updateNotifications() {
        notifications.setAll(dh.getAllNotifications());
    }

    private void updateImportantDates() {
        importantDates.setAll(dh.getImportantDates());
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
