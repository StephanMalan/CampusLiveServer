package net.ddns.swooosh.campusliveserver.main;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.all.*;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudentConnectionHandler extends ConnectionHandler implements Runnable {

    private String studentNumber;
    private String qualification;
    private ObjectProperty<Student> student = new SimpleObjectProperty<>();
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    private volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    volatile BooleanProperty updateStudent = new SimpleBooleanProperty(false);
    volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    volatile BooleanProperty updateNotifications = new SimpleBooleanProperty(false);
    volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);

    public StudentConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String studentNumber, ObservableList<ConnectionHandler> connectionsList, DatabaseHandler dh) {
        super(socket, objectInputStream, objectOutputStream, connectionsList, dh);
        this.studentNumber = studentNumber;
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
                updateNotices.set(false);
            }
        });
        updateNotifications.addListener((obs, oldV, newV) -> {
            if (updateNotifications.get()) {
                updateNotifications();
                updateNotifications.set(false);
            }
        });
        updateContactDetails.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateContactDetails();
                updateContactDetails.set(false);
            }
        });
        updateImportantDates.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateImportantDates();
                updateImportantDates.set(false);
            }
        });
        //TODO no need to add listener to list as updateStudent() already runs when updated ???
        student.addListener(e -> {
            outputQueue.add(0, student.get());
        });
        notices.addListener((InvalidationListener) e -> {
            outputQueue.add(0, Arrays.asList(notices.toArray()));
        });
        notifications.addListener((InvalidationListener) e -> {
            if (!notifications.isEmpty()) {
                System.out.println("adding notification: " + notifications);
                outputQueue.add(0, Arrays.asList(notifications.toArray()));
            }
        });
        contactDetails.addListener((InvalidationListener) e -> {
            outputQueue.add(0, Arrays.asList(contactDetails.toArray()));
        });
        importantDates.addListener((InvalidationListener) e -> {
            outputQueue.add(0, Arrays.asList(importantDates.toArray()));
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
                Object input;
                if ((input = getReply()) != null) {
                    if (input instanceof String) {
                        String text = input.toString();
                        if (text.startsWith("lo:")) {
                            dh.log("Student " + studentNumber + "> Requested ClassLecturer Online");
                            if (isLecturerOnline(text.substring(3))) {
                                outputQueue.add(0, "lo:y");
                            } else {
                                outputQueue.add(0, "lo:n");
                            }
                        } else if (text.startsWith("cp:")) {
                            dh.log("Student " + studentNumber + "> Requested Change Password");
                            changePassword(text.substring(3).split(":")[0], text.substring(3).split(":")[1]);
                        } else if (text.startsWith("cdp:")) {
                            dh.log("Student " + studentNumber + "> Requested Change Default Password");
                            changeDefaultPassword(text.substring(4));
                        } else if (text.startsWith("sm:")) {
                            dh.log("Student " + studentNumber + "> Send Direct Message to ClassLecturer: " + text.substring(3).split(":")[1] + "> " + text.substring(3).split(":")[0]);
                            sendMessage(text.substring(3).split(":")[0], text.substring(3).split(":")[1]);
                        } else if (text.startsWith("gf:")) {
                            dh.log("Student " + studentNumber + "> Requested File: " + text.substring(3).split(":")[1] + " From class: " + text.substring(3).split(":")[0]);
                            getFile(text.substring(3).split(":")[0], text.substring(3).split(":")[1]);
                        } else if (text.startsWith("gff:")) {
                            dh.log("Student " + studentNumber + "> Requested File: " + text.substring(3).split(":")[1] + " From class: " + text.substring(3).split(":")[0]);
                            outputQueue.add(0, getOnlineDownload(Integer.parseInt(text.substring(3).split(":")[0]), text.substring(3).split(":")[1]));
                        } else if (text.startsWith("idp:")) {
                            isDefaultPassword();
                        } else if (text.startsWith("dn:")) {
                            dh.log("Student " + studentNumber + "> Dismissed Notification");
                            dh.removeNotification(Integer.parseInt(text.substring(3)));
                        } else {
                            dh.log("Student " + studentNumber + "> Requested Unknown Command: " + input);
                            System.out.println("Server> Unknown command: " + input);
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
                        if (out instanceof List && (((List) out).isEmpty() || ((List) out).get(0) == null)) {
                            outputQueue.remove(out);
                        } else {
                            sendData(out);
                            dh.log("Student " + studentNumber + "> OutputProcessor> Sent: " + out + " (" + out.getClass().toString() + ")");
                            outputQueue.remove(out);
                        }
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
                System.out.println(data);
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

    private boolean isLecturerOnline(String lecturerNumber) {
        for (ConnectionHandler ch : connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                if (((LecturerConnectionHandler) ch).getLecturerNumber().matches(lecturerNumber)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void changePassword(String prevPassword, String newPassword) {
        String sPassword = dh.getStudentPassword(studentNumber);
        if (prevPassword.matches(sPassword) && dh.changeStudentPassword(studentNumber, newPassword)) {
            outputQueue.add(0, "cp:y");
        } else {
            outputQueue.add(0, "cp:n");
        }
    }

    private void changeDefaultPassword(String newPassword) {
        if (dh.changeStudentDefaultPassword(studentNumber, newPassword)) {
            outputQueue.add(0, "cdp:y");
        } else {
            outputQueue.add(0, "cdp:n");
        }
    }

    private void isDefaultPassword() {
        if (dh.isDefaultStudentPassword(studentNumber)) {
            outputQueue.add(0, "idp:y");
        } else {
            outputQueue.add(0, "idp:n");
        }
    }

    private void sendMessage(String message, String lecturerNumber) {
        if (isLecturerOnline(lecturerNumber)) {
            for (ConnectionHandler ch : connectionsList) {
                if (ch instanceof LecturerConnectionHandler) {
                    if (((LecturerConnectionHandler) ch).getLecturerNumber().matches(lecturerNumber)) {
                        ((LecturerConnectionHandler) ch).addMessage(message, studentNumber);
                    }
                }
            }
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
                size += Math.min(Server.BUFFER_SIZE, fileBytes.length - size);
                dh.log("Student " + studentNumber + "> Successfully Downloaded File: " + fileName + " For Class: " + classID);
                System.out.println("Server> Successfully Downloaded File: " + fileName + " For Class: " + classID);
            }
        } catch (Exception ex) {
            dh.log("Server> getFile> " + ex);
            ex.printStackTrace();
        }
    }

    public Student getStudent() {
        return student.getValue();
    }

    public String getQualification() {
        return student.getValue().getQualification();
    }

    private void updateStudent() {
        student.setValue(dh.getStudent(studentNumber));
    }

    private void updateNotices() {
        notices.clear();
        notices.addAll(dh.getNotices(studentNumber, qualification));
    }

    private void updateNotifications() {
        notifications.clear();
        notifications.addAll(dh.getNotifications(studentNumber));
    }

    private void updateContactDetails() {
        contactDetails.clear();
        contactDetails.addAll(dh.getContactDetails());
    }

    private void updateImportantDates() {
        importantDates.clear();
        importantDates.addAll(dh.getImportantDates());
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public OnlineDownload getOnlineDownload(int classId, String fileName) {
        try {
            DbxRequestConfig reqConfig = new DbxRequestConfig("javarootsDropbox/1.0", Locale.getDefault().toString());
            String accessToken = "hU-YDhhEj8AAAAAAAAAASPYEYk01yvrEBZI95MDCN9oozxtJItePEFEKln71YSIl";
            DbxClientV2 client = new DbxClientV2(reqConfig, accessToken);
            SharedLinkMetadata slm = client.sharing().createSharedLinkWithSettings("/1/ITII320 – Assignment – Specification (V1.0).pdf", SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build());
            return new OnlineDownload(classId, fileName, slm.getUrl());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new OnlineDownload(classId, fileName, "Not available");
    }

}
