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
import java.util.List;

public class AdminConnectionHandler extends ConnectionHandler implements Runnable {

    private String username;
    private ObservableList<Admin> admins = FXCollections.observableArrayList();
    private ObservableList<AdminSearch> studentSearches = FXCollections.observableArrayList();
    private Student student;
    private ObservableList<AdminSearch> lecturerSearches = FXCollections.observableArrayList();
    private Lecturer lecturer;
    private ObservableList<AdminSearch> classSearches = FXCollections.observableArrayList();
    private StudentClass studentClass;
    private ObservableList<AdminSearch> contactSearches = FXCollections.observableArrayList();
    private ContactDetails contactDetails;
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
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
                    if (input instanceof String) {
                        String text = input.toString();
                        if (text.startsWith("asd:")) { //request student
                            student = dh.getStudent(text.substring(4));
                            if (student != null) {
                                outputQueue.add(student);
                            }
                        } else if (text.startsWith("ald:")) { //request lecturer
                            lecturer = dh.getLecturer(text.substring(4));
                            if (lecturer != null) {
                                outputQueue.add(lecturer);
                            }
                        } else if (text.startsWith("acd:")) { //request class
                            studentClass = dh.getClass(Integer.parseInt(text.substring(4)));
                            if (studentClass != null) {
                                outputQueue.add(studentClass);
                            }
                        } else if (text.startsWith("asl:")) { //request log
                            try {
                                outputQueue.add(new AdminLog(Files.readAllBytes(Server.LOG_FILE.toPath())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (text.startsWith("aci:")) { //request contact details
                            contactDetails = dh.getContactDetail(text.substring(4).split(":")[0], text.substring(4).split(":")[1]);
                            if (contactDetails != null) {
                                outputQueue.add(contactDetails);
                            }
                        } else if (text.startsWith("rm:")) { //remove admin
                            dh.removeAdmin(text.substring(3));
                        } else if (text.startsWith("rs:")) { //remove student
                            dh.removeStudent(text.substring(3));
                        } else if (text.startsWith("rl:")) { //remove lecturer
                            dh.removeLecturer(text.substring(3));
                        } else if (text.startsWith("rc:")) { //remove class
                            dh.removeClass(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("ra:")) { //remove attendance
                            dh.removeAttendance(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rr:")) { //remove result template
                            dh.removeResultTemplate(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rn:")) { //remove notice
                            dh.removeNotice(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rf:")) { //remove notification
                            dh.removeNotification(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rd:")) { //remove contact details
                            dh.removeContactDetails(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("ri:")) { //remove important dates
                            dh.removeImportantDate(Integer.parseInt(text.substring(3)));
                        } else if (text.startsWith("rct:")) { //remove class time
                            dh.removeClassTime(Integer.parseInt(text.substring(4)));
                        } else if (text.startsWith("uc:")) { //unregister class
                            dh.removeStudentFromClass(text.split(":")[1], Integer.parseInt(text.split(":")[2]));
                            outputQueue.add(dh.getStudent(student.getStudentNumber()));
                        } else if (text.startsWith("rsc:")) { //register class
                            dh.addStudentToClass(text.split(":")[1], Integer.parseInt(text.split(":")[2]));
                            outputQueue.add(dh.getStudent(student.getStudentNumber()));
                        } else if (text.startsWith("gac:")) { //get all classes
                            outputQueue.add(dh.getAllStudentClasses());
                        } else if (text.startsWith("rap:")) { //reset admin password
                            dh.resetAdminPassword(text.split(":")[1], text.split(":")[2]);
                        } else if (text.startsWith("rsp:")) { //reset student password
                            dh.resetStudentPassword(text.split(":")[1], text.split(":")[2]);
                        } else if (text.startsWith("rlp:")) { //reset lecturer password
                            dh.resetLecturerPassword(text.split(":")[1], text.split(":")[2]);
                        } else if (text.startsWith("idp:")) { //is default password
                            isDefaultPassword();
                        } else if (text.startsWith("cdp:")) { //reset admin password
                            changeDefaultPassword(text.substring(4));
                        } else if (text.startsWith("rse:")) { //register supplementary examination
                            dh.regSuppExam(text.split(":")[1], Integer.parseInt(text.split(":")[2]));
                        } else {
                            dh.log("Admin " + username + "> Requested Unknown Command: " + input);
                        }
                    } else if (input instanceof Student) {
                        dh.updateStudent((Student) input);
                    } else if (input instanceof Admin) {
                        dh.updateAdmin((Admin) input);
                    } else if (input instanceof Attendance) {
                        dh.updateAttendance((Attendance) input);
                    } else if (input instanceof Lecturer) {
                        dh.updateLecturer((Lecturer) input);
                    } else if (input instanceof StudentClass) {
                        dh.updateClass((StudentClass) input);
                    } else if (input instanceof ClassTime) {
                        dh.updateClassTime((ClassTime) input);
                    } else if (input instanceof Notification) {
                        dh.updateNotification((Notification) input);
                    } else if (input instanceof Notice) {
                        dh.updateNotice((Notice) input);
                    } else if (input instanceof ImportantDate) {
                        dh.updateDate((ImportantDate) input);
                    } else if (input instanceof Result) {
                        dh.updateResult((Result) input);
                    } else if (input instanceof ContactDetails) {
                        dh.updateContactDetails((ContactDetails) input);
                    } else if (input instanceof List<?>) {
                        List list = (List) input;
                        if (!list.isEmpty() && list.get(0) instanceof ResultTemplate) {
                            dh.updateResultTemplate(list);
                        }
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
