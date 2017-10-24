package net.ddns.swooosh.campusliveserver.main;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.admin.Admin;
import models.admin.AdminClass;
import models.admin.AdminLog;
import models.admin.AdminSearch;
import models.all.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;

public class AdminConnectionHandler extends ConnectionHandler implements Runnable {

    private String username;
    private ObservableList<Admin> admins = FXCollections.observableArrayList();
    private ObservableList<AdminSearch> studentSearches = FXCollections.observableArrayList();
    private Student student;
    private ObservableList<AdminSearch> lecturerSearches = FXCollections.observableArrayList();
    private Lecturer lecturer;
    private ObservableList<AdminSearch> classSearches = FXCollections.observableArrayList();
    private AdminClass adminClass;
    private ObservableList<AdminSearch> contactSearches = FXCollections.observableArrayList();
    private ContactDetails contactDetails;
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    private AdminLog adminLog;
    public volatile BooleanProperty updateAdmins = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateStudents = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateLecturers = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateClasses = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotifications = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();

    public AdminConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String username, ObservableList<ConnectionHandler> connectionsList, DatabaseHandler dh) {
        super(socket, objectInputStream, objectOutputStream, connectionsList, dh);
        this.username = username;
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
                            student = dh.getStudent(text.substring(4));
                            if (student != null) {
                                outputQueue.add(student);
                            }
                        } else if(text.startsWith("ald:")){
                            lecturer = dh.getLecturer(text.substring(4));
                            if (lecturer != null) {
                                outputQueue.add(lecturer);
                            }
                        } else if(text.startsWith("acd:")){
                            outputQueue.add(dh.getClass(Integer.parseInt(text.substring(4))));
                        } else if(text.startsWith("asl:")){
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
                        } else if (text.startsWith("uc:")){//unregister class
                            dh.removeStudentFromClass(text.split(":")[1], Integer.parseInt(text.split(":")[2]));
                            outputQueue.add(dh.getStudent(student.getStudentNumber()));
                        } else if (text.startsWith("rsc:")){//register class
                            dh.addStudentToClass(text.split(":")[1], Integer.parseInt(text.split(":")[2]));
                            outputQueue.add(dh.getStudent(student.getStudentNumber()));
                        } else if (text.startsWith("gac:")){//get all classes
                            outputQueue.add(dh.getAllStudentClasses());
                        } else if (text.startsWith("rap:")){//reset admin password
                            dh.resetAdminPassword(text.split(":")[1], text.split(":")[2]);
                        } else if (text.startsWith("idp:")){//is default password
                            isDefaultPassword();
                        } else if (text.startsWith("cdp:")){//reset admin password
                           changeDefaultPassword(text.substring(4));
                        } else {
                            dh.log("Admin " + username + "> Requested Unknown Command: " + input);
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
                        dh.updateResult(uResult.getResultTemplateID(), uResult.getStudentNumber(), (int) uResult.getResult());
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
                dh.log("Server> Sent data: " + data);
            }
        } catch (Exception ex) {
            terminateConnection();
            dh.log("Server> sendData> " + ex);
            ex.printStackTrace();
        }
    }

    private void isDefaultPassword() {
        if (dh.isDefaultAdminPassword(username)) {
            outputQueue.add(0, "idp:y");
        } else {
            outputQueue.add(0, "idp:n");
        }
    }

    private void changeDefaultPassword(String newPassword) {
        if (dh.changeAdminDefaultPassword(username, newPassword)) {
            outputQueue.add(0, "cdp:y");
        } else {
            outputQueue.add(0, "cdp:n");
        }
    }

    private void updateAdmins() {
        admins.setAll(dh.getAllAdmins());
        outputQueue.add(Arrays.asList(admins.toArray()));
    }

    private void updateStudents() {
        studentSearches.setAll(dh.getStudentSearch());
        outputQueue.add(Arrays.asList(studentSearches.toArray()));
    }

    private void updateLecturers() {
        lecturerSearches.setAll(dh.getLecturerSearch());
        outputQueue.add(Arrays.asList(lecturerSearches.toArray()));
    }

    private void updateClasses() {
        classSearches.setAll(dh.getClassSearch());
        outputQueue.add(Arrays.asList(classSearches.toArray()));
    }

    private void updateContactDetails() {
        contactSearches.setAll(dh.getContactDetailsSearch());
        outputQueue.add(Arrays.asList(contactSearches.toArray()));
    }

    private void updateNotices() {
        notices.setAll(dh.getAllNotices());
        outputQueue.add(Arrays.asList(notices.toArray()));
    }

    private void updateNotifications() {
        notifications.setAll(dh.getAllNotifications());
        outputQueue.add(Arrays.asList(notifications.toArray()));
    }

    private void updateImportantDates() {
        importantDates.setAll(dh.getImportantDates());
        outputQueue.add(Arrays.asList(importantDates.toArray()));
    }

}
