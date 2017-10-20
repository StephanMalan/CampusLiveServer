package net.ddns.swooosh.campusliveserver.main;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.all.*;
import models.lecturer.LecturerStudentAttendance;
import models.lecturer.LecturerStudentResult;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class LecturerConnectionHandler extends ConnectionHandler implements Runnable {

    private String lecturerNumber;
    private ObjectProperty<Lecturer> lecturer = new SimpleObjectProperty<>();
    private ObservableList<Notice> notices = FXCollections.observableArrayList();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
    private ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    private ObservableList<LecturerStudentAttendance> studentAttendance = FXCollections.observableArrayList();
    private ObservableList<LecturerStudentResult> studentResults = FXCollections.observableArrayList();
    public volatile ObservableList<Object> outputQueue = FXCollections.observableArrayList();
    public volatile ObservableList<FilePart> downloadQueue = FXCollections.observableArrayList();
    public volatile BooleanProperty updateLecturer = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateNotices = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateContactDetails = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateImportantDates = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateStudentAttendance = new SimpleBooleanProperty(false);
    public volatile BooleanProperty updateStudentResults = new SimpleBooleanProperty(false);
    public volatile BooleanProperty running = new SimpleBooleanProperty(true);
    //private FileDownloader fileDownloader = new FileDownloader()//TODO where to start

    public LecturerConnectionHandler(Socket socket, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, String lecturerNumber, ObservableList<ConnectionHandler> connectionsList, DatabaseHandler dh) {
        super(socket, objectInputStream, objectOutputStream, connectionsList, dh);
        this.lecturerNumber = lecturerNumber;
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
        updateStudentAttendance.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateStudentAttendance();
                updateStudentAttendance.set(false);
            }
        });
        updateStudentResults.addListener((obs, oldV, newV) -> {
            if (newV) {
                updateStudentResults();
                updateStudentResults.set(false);
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
                        if (text.startsWith("cp:")) {
                            changePassword(text.substring(3).split(":")[0], text.substring(3).split(":")[1]);
                        } else if (text.startsWith("fp:")) {
                            forgotPassword(text.substring(3));
                        } else if (text.startsWith("gf:")) {
                            getFile(text.substring(3).split(":")[0], text.substring(3).split(":")[1]);
                        } else if (text.startsWith("lgt:")) {
                            terminateConnection();
                        } else {
                            System.out.println("Unknown command: " + input);
                        }
                    } else if (input instanceof FilePart) {
                        FilePart fp = (FilePart) input;
                        downloadQueue.add(fp);
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

    public class FileDownloader extends Thread {

        public volatile IntegerProperty size;
        public volatile DoubleProperty progress;
        ClassFile file;
        byte[] bytes;

        public FileDownloader(ClassFile file) {
            this.file = file;
            bytes = new byte[file.getFileLength()];
            size = new SimpleIntegerProperty(0);
            progress = new SimpleDoubleProperty(0);
        }

        @Override
        public void run() {
            outputQueue.add("gf:" + file.getClassID() + ":" + file.getFileName());
            Done:
            while (true) {
                FilePart filePartToRemove = null;
                BreakSearch:
                for (int i = downloadQueue.size() - 1; i > -1; i--) {
                    try {
                        Object object = downloadQueue.get(i);
                        if (object instanceof FilePart) {
                            FilePart filePart = (FilePart) object;
                            if (filePart.getClassID() == file.getClassID() && filePart.getFileName().equals(file.getFileName())) {
                                filePartToRemove = filePart;
                                break BreakSearch;
                            }
                        }
                    } catch (IndexOutOfBoundsException ex) {
                    }
                }
                if (filePartToRemove != null) {
                    for (int i = 0; i < filePartToRemove.getFileBytes().length; i++) {
                        bytes[size.get() + i] = filePartToRemove.getFileBytes()[i];
                    }
                    size.set(size.get() + filePartToRemove.getFileBytes().length);
                    progress.set(1D * size.get() / bytes.length);
                    downloadQueue.remove(filePartToRemove);
                }
                if (size.get() == file.getFileLength()) {
                    System.out.println("File successfully downloaded!");
                    File f = new File(Server.FILES_FOLDER + "/" + file.getClassID() + "/" + file.getFileName());
                    f.getParentFile().mkdirs();
                    try {
                        Files.write(f.toPath(), bytes);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break Done;
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

    public Object getReply() {
        try {
            Object input;
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
            dh.changeLecturerPassword(lecturerNumber, newPassword);
            outputQueue.add(0, "cp:y");
        } else {
            outputQueue.add(0, "cp:n");
        }
    }

    public String getLecturerNumber() {
        return lecturerNumber;
    }

    public Lecturer getLecturer() {
        return lecturer.getValue();
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
        //new FileDownloader() TODO
        updateStudents(classID);
        updateLecturer.setValue(true);
    }


    private Boolean updateStudents(String classID) {
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

    private void updateNotices() {
        notices.addAll(dh.getNotices(lecturerNumber, "ClassLecturer"));
    }

    private void updateNotifications() {
        notifications.addAll(dh.getNotifications(lecturerNumber, "ClassLecturer"));
    }

    private void updateContactDetails() {
        contactDetails.addAll(dh.getContactDetails());
        contactDetails.addAll(dh.getStudentContactDetails(lecturerNumber));
    }

    private void updateImportantDates() {
        importantDates.addAll(dh.getImportantDates());
    }

    private void updateStudentAttendance() {
        studentAttendance.addAll(dh.getAllStudentsInClassAttendance(lecturerNumber));
    }

    private void updateStudentResults() {
        studentResults.addAll(dh.getAllStudentsInClassResults(lecturerNumber));
    }

}
