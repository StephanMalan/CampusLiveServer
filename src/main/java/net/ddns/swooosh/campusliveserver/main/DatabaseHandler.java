package net.ddns.swooosh.campusliveserver.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import models.admin.*;
import models.all.*;
import models.lecturer.LecturerStudentAttendance;
import models.lecturer.LecturerStudentAttendanceClass;
import models.lecturer.LecturerStudentResult;
import models.lecturer.LecturerStudentResultClass;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


public class DatabaseHandler {

    private Connection con;
    private Email mail = new Email();

    DatabaseHandler() {
        connectDB();
    }

    //<editor-fold desc="Database Connection">
    private void connectDB() {
        try {
            Boolean createDatabase = false;
            if (!Server.DATABASE_FILE.exists()) {
                createDatabase = true;
            }
            con = DriverManager.getConnection("jdbc:sqlite:" + Server.DATABASE_FILE.getAbsolutePath());
            if (createDatabase) {
                Statement stmt = con.createStatement();
                stmt.execute("CREATE TABLE Student (" +
                        "StudentNumber TEXT PRIMARY KEY, " +
                        "Qualification TEXT, " +
                        "FirstName TEXT, " +
                        "LastName TEXT, " +
                        "Password TEXT, " +
                        "AssignedPassword BOOLEAN, " +
                        "Email TEXT, " +
                        "ContactNumber TEXT);");
                stmt.execute("CREATE TABLE Registered (" +
                        "RegisteredID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "StudentNumber TEXT, " +
                        "ClassID INTEGER);");
                stmt.execute("CREATE TABLE Attendance (" +
                        "AttendanceID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID INTEGER, " +
                        "StudentNumber TEXT, " +
                        "ADate TEXT, " +
                        "Attendance TEXT);");
                stmt.execute("CREATE TABLE ClassTime (" +
                        "ClassTimeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID INTEGER, " +
                        "RoomNumber TEXT, " +
                        "DayOfWeek INTEGER, " +
                        "StartSlot INTEGER, " +
                        "EndSlot INTEGER);");
                stmt.execute("CREATE TABLE Class (" +
                        "ClassID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ModuleName TEXT, " +
                        "ModuleNumber TEXT, " +
                        "LecturerID TEXT);");
                stmt.execute("CREATE TABLE Lecturer (" +
                        "LecturerID TEXT PRIMARY KEY, " +
                        "FirstName TEXT, " +
                        "LastName TEXT, " +
                        "Password TEXT, " +
                        "Email TEXT, " +
                        "ContactNumber TEXT);");
                stmt.execute("CREATE TABLE Result (" +
                        "ResultID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ResultTemplateID INTEGER, " +
                        "StudentNumber TEXT, " +
                        "Result INTEGER);");
                stmt.execute("CREATE TABLE ResultTemplate (" +
                        "ResultTemplateID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID INTEGER, " +
                        "ResultMax INTEGER, " +
                        "DPWeight INTEGER, " +
                        "FinalWeight INTEGER, " +
                        "ResultName TEXT);");
                stmt.execute("CREATE TABLE Notice (" +
                        "NoticeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Heading TEXT, " +
                        "Description TEXT, " +
                        "ExpiryDate TEXT, " +
                        "Tag TEXT);");
                stmt.execute("CREATE TABLE Notification (" +
                        "NotificationID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Heading TEXT, " +
                        "Description TEXT, " +
                        "Tag TEXT);");
                stmt.execute("CREATE TABLE ContactDetails (" +
                        "ContactDetailsID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Name TEXT, " +
                        "Position TEXT, " +
                        "Department TEXT, " +
                        "ContactNumber TEXT, " +
                        "Email TEXT);");
                stmt.execute("CREATE TABLE ImportantDate (" +
                        "ImportantDateID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "IDate TEXT, " +
                        "Description TEXT);");
                stmt.execute("CREATE TABLE Admin (" +
                        "Username TEXT, " +
                        "Password TEXT, " +
                        "AssignedPassword BOOLEAN, " +
                        "Email TEXT);");
                log("Server> Created Database");
            }
            System.out.println("Server> Connected to database");
            log("Server> Connected to database");
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> connectDB> " + ex);
            System.exit(0);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Authorisation">
    protected Boolean authoriseStudent(String studentNumber, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ? AND Password = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseStudent> " + studentNumber + "> " + ex);
            return false;
        }
    }

    Boolean authoriseLecturer(String lecturerNumber, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerID = ? AND Password = ?");
            preparedStatement.setString(1, lecturerNumber);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseLecturer> " + lecturerNumber + "> " + ex);
            return false;
        }
    }

    Boolean authoriseAdmin(String username, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Admin WHERE Username = ? AND Password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseAdmin> " + username + "> " + ex);
            return false;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Getters">
    Student getStudent(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            List<ClassResultAttendance> classResultAttendances = getStudentClassesResultsAttendance(studentNumber);
            Student student = new Student(rs.getString("StudentNumber"), rs.getString("Qualification"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), rs.getString("ContactNumber"), classResultAttendances);
            log("Server> Successfully Created Student: " + studentNumber);
            return student;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudent> " + studentNumber + "> " + ex);
            return null;
        }
    }

    private List<ClassResultAttendance> getStudentClassesResultsAttendance(String studentNumber) {
        try {
            List<ClassResultAttendance> classResultAttendances = new ArrayList<>();
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.StudentNumber = ? AND Registered.ClassID = Class.ClassID");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<Result> results = getStudentResults(studentNumber, rs.getInt("ClassID"));
                StudentClass studentClass = getStudentClass(rs.getInt("ClassID"), studentNumber);
                List<Attendance> attendance = getStudentAttendance(rs.getInt("ClassID"), studentNumber);
                classResultAttendances.add(new ClassResultAttendance(studentClass, results, attendance));
            }
            log("Server> Successfully Created Classes Results Attendance For Student: " + studentNumber);
            return classResultAttendances;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentClassesAndResults> " + studentNumber + "> " + ex);
            return null;
        }
    }

    private List<Result> getStudentResults(String studentNumber, int classID) {
        List<Result> results = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Result, ResultTemplate WHERE Result.ResultTemplateID = ResultTemplate.ResultTemplateID AND Result.StudentNumber = ? AND ResultTemplate.ClassID = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                results.add(new Result(rs.getInt("ResultTemplateID"), rs.getString("StudentNumber"), rs.getString("ResultName"), rs.getInt("Result"), rs.getInt("ResultMax"), rs.getInt("DPWeight"), rs.getInt("FinalWeight")));
            }
            log("Server> Successfully Created Results For Student: " + studentNumber);
            return results;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentResults> " + studentNumber + "> " + ex);
            return null;
        }
    }

    private StudentClass getStudentClass(int classID, String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.ClassID = Class.ClassID AND Class.ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            StudentClass studentClass = null;
            if (rs.next()) {
                List<ClassTime> classTimes = getClassTimes(classID);
                List<ClassFile> files = getFiles(classID);
                studentClass = new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(classID), classTimes, files);
            }
            log("Server> Successfully Created Class: " + classID + " for Student: " + studentNumber);
            return studentClass;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentClass> " + studentNumber + "> " + ex);
            return null;
        }
    }

    List<StudentClass> getAllStudentClasses() {
        List<StudentClass> classes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<ClassTime> classTimes = getClassTimes(rs.getInt("ClassID"));
                List<ClassFile> files = getFiles(rs.getInt("ClassID"));
                classes.add(new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(rs.getInt("ClassID")), classTimes, files));
            }
            log("Server> Successfully retrieved all classes");
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getAllStudentClasses> " + ex);
        }
        if (classes.isEmpty()) {
            classes.add(new StudentClass(-1, null, null, null, null, null));
        }
        return classes;
    }

    private ClassLecturer getClassLecturer(int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer, Class WHERE Lecturer.LecturerID = Class.LecturerID AND Class.ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return new ClassLecturer(rs.getString("LecturerID"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("ContactNumber"), rs.getString("Email"), getLecturerImage(rs.getString("LecturerID")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
        }
        return null;
    }

    private byte[] getLecturerImage(String lecturerNumber) {
        try {
            BufferedImage lecturerImage = ImageIO.read(new File(Server.LECTURER_IMAGES + "/" + lecturerNumber + "/profile.jpg"));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(lecturerImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byte[] lecturerImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return lecturerImageBytes;
        } catch (Exception ex) {
            log("Server> Can't find picture for lecturer, " + lecturerNumber);
        }
        return null;
    }

    private List<Attendance> getStudentAttendance(int classID, String studentNumber) {
        List<Attendance> attendance = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Attendance WHERE ClassID = ? AND StudentNumber = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.setString(2, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                attendance.add(new Attendance(rs.getString("ADate"), rs.getString("Attendance")));
            }
            log("Server> Successfully Created Attendance For Student: " + studentNumber);
            return attendance;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentResults> " + ex);
            return null;
        }
    }

    private List<ClassTime> getClassTimes(int classID) {
        List<ClassTime> classTimes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ClassTime WHERE ClassID = ?");
            preparedStatement.setString(1, Integer.toString(classID));
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                classTimes.add(new ClassTime(rs.getInt("ClassTimeID"), rs.getInt("ClassID"), rs.getString("RoomNumber"), rs.getInt("DayOfWeek"), rs.getInt("StartSlot"), rs.getInt("EndSlot")));
            }
            log("Server> Successfully Created ClassTimes For Class: " + classID);
            return classTimes;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassTimes> " + ex);
            return null;
        }
    }

    String getStudentPassword(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Gotten Password For Student: " + studentNumber);
                return rs.getString("Password");
            } else {
                log("Server> Failed To Get Password For Student: " + studentNumber);
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentPassword> " + ex);
            return null;
        }
    }

    String getStudentQualification(String studentNumber) {
        try {
            System.out.println(studentNumber);
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.getString("Qualification");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    StudentClass getClass(int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class WHERE classID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            //log("Server> Successfully Created ClassLecturer: " + lecturerNumber);
            return new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(classID), getClassTimes(classID), null);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
            return null;
        }
    }

    ContactDetails getContactDetailsDetail(String contactDetailsID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ContactDetails WHERE Name = ?");
            preparedStatement.setString(1, contactDetailsID);
            ResultSet rs = preparedStatement.executeQuery();
            //log("Server> Successfully Created ClassLecturer: " + lecturerNumber);
            return new ContactDetails(rs.getInt("ContactDetailsID"), rs.getString("Name"), rs.getString("Position"), rs.getString("Department"), rs.getString("ContactNumber"), rs.getString("Email"), getContactImage("ContactDetailsID"));
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
            return null;
        }
    }

    ObservableList<Notice> getNotices(String studentNumber, String qualification) {//if date past
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notice WHERE Tag = ? OR Tag = ? OR Tag = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, qualification);
            preparedStatement.setString(3, "Campus");
            ResultSet rs = preparedStatement.executeQuery();
            ObservableList<Notice> notices = FXCollections.observableArrayList();
            while (rs.next()) {
                Notice newNotice = new Notice(rs.getInt("NoticeID"), rs.getString("Heading"), rs.getString("Description"), rs.getString("Tag"), rs.getString("ExpiryDate"));
                notices.add(newNotice);
            }
            log("Server> Successfully Gotten Notices For Student/ClassLecturer: " + studentNumber);
            if (notices.isEmpty()) {
                notices.add(new Notice(0, "NoNotice", "", "", ""));
            }
            return notices;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
    }

    ObservableList<Notification> getNotifications(String studentNumber) {
        try {
            ObservableList<Notification> notifications = FXCollections.observableArrayList();
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notification WHERE Tag = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                notifications.add(new Notification(rs.getInt("NotificationID"), rs.getString("Heading"), rs.getString("Description"), rs.getString("Tag")));
            }
            log("Server> Successfully Gotten Notifications For Student/ClassLecturer: " + studentNumber);
            if (notifications.isEmpty()) {
                notifications.add(new Notification(0, "NoNotification", "", ""));
            }
            return notifications;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
    }

    ObservableList<ContactDetails> getContactDetails() {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ContactDetails;");
            ResultSet rs = preparedStatement.executeQuery();
            ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
            while (rs.next()) {
                ContactDetails newContactDetail = new ContactDetails(rs.getInt("ContactDetailsID"), rs.getString("Name"), rs.getString("Position"), rs.getString("Department"), rs.getString("ContactNumber"), rs.getString("Email"), getContactImage(rs.getString("ContactDetailsID")));
                contactDetails.add(newContactDetail);
            }
            log("Server> Successfully Gotten Notices For Student/ClassLecturer: ");
            if (contactDetails.isEmpty()) {
                contactDetails.add(new ContactDetails(0, "NoContactDetails", "", "", "", "", null));
            }
            return contactDetails;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
    }

    private byte[] getContactImage(String contactID) {
        try {
            BufferedImage lecturerImage = ImageIO.read(new File(Server.CONTACT_IMAGES + "/" + contactID + "/profile.jpg"));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(lecturerImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byte[] lecturerImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return lecturerImageBytes;
        } catch (Exception ex) {
            System.out.println("Server> Can't find picture for contact, " + contactID);
        }
        return null;
    }

    ObservableList<ImportantDate> getImportantDates() {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ImportantDate ORDER BY IDate;");
            ResultSet rs = preparedStatement.executeQuery();
            ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
            while (rs.next()) {
                ImportantDate newImportantDate = new ImportantDate(rs.getString("IDate"), rs.getString("Description"));
                importantDates.add(newImportantDate);
            }
            log("Server> Successfully Gotten Notices For Student/ClassLecturer: ");
            if (importantDates.isEmpty()) {
                importantDates.add(new ImportantDate("NoImportantDate", ""));
            }
            return importantDates;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
    }

    private List<ClassFile> getFiles(int classID) {
        List<ClassFile> files = new ArrayList<>();
        File classFilesDirectory = new File(Server.FILES_FOLDER.getAbsolutePath() + "/" + classID);
        if (classFilesDirectory.exists()) {
            for (File file : classFilesDirectory.listFiles()) {
                files.add(new ClassFile(classID, file.getName(), (int) file.length()));
            }
        }
        log("Server> Successfully Gotten Files: ");
        return files;
    }

    Lecturer getLecturer(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerID = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            List<LecturerClass> classes = getLecturerClasses(lecturerNumber);
            Lecturer lecturer = new Lecturer(rs.getString("FirstName"), rs.getString("LastName"), rs.getString("LecturerID"), rs.getString("Email"), rs.getString("ContactNumber"), getLecturerImage(rs.getString("LecturerID")), classes);
            log("Server> Successfully Created ClassLecturer: " + lecturerNumber);
            return lecturer;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
            return null;
        }
    }

    List<ContactDetails> getStudentContactDetails(String lecturerNumber) {
        List<LecturerClass> classes = getLecturerClasses(lecturerNumber);
        ObservableList<ContactDetails> contactDetails = FXCollections.observableArrayList();
        for (LecturerClass aClass : classes) {
            try {
                PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student, Registered, Class WHERE Student.StudentNumber = Registered.StudentNumber AND Class.ClassID = Registered.ClassID AND Class.ClassID = ?;");
                preparedStatement.setInt(1, aClass.getId());
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    ContactDetails newContactDetail = new ContactDetails(0, rs.getString("FirstName") + " " + rs.getString("LastName"), rs.getString("StudentNumber"), "Student", rs.getString("ContactNumber"), rs.getString("Email"), null);//TODO send null bytes
                    if (!contactDetails.contains(newContactDetail)) {
                        contactDetails.add(newContactDetail);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                log("Server> getNoticeBoards> " + ex);
                return null;
            }
        }
        return contactDetails;
    }

    private List<LecturerClass> getLecturerClasses(String lecturerNumber) {
        List<LecturerClass> classes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class WHERE LecturerID = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PreparedStatement preparedStatement2 = con.prepareStatement("SELECT ClassTimeID FROM ClassTime WHERE ClassID = ?");
                preparedStatement2.setInt(1, rs.getInt("ClassID"));
                ResultSet rs2 = preparedStatement2.executeQuery();
                classes.add(new LecturerClass(rs2.getInt("ClassTimeID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassTimes(rs.getInt("ClassID")), getFiles(rs.getInt("ClassID"))));
            }
            log("Server> Successfully Gotten Classes For ClassLecturer: " + lecturerNumber);
            return classes;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getLecturerClasses> " + ex);
            return null;
        }
    }

    String getLecturerPassword(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password FROM Lecturer WHERE LecturerID = ?;");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Gotten Password For ClassLecturer: " + lecturerNumber);
                return rs.getString("Password");
            } else {
                log("Server> Failed To Get Password For ClassLecturer: " + lecturerNumber);
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getLecturerPassword> " + ex);
            return null;
        }
    }

    List<String> getStudentsInClass(int classID) {
        List<String> students = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Student.StudentNumber FROM Student, Registered WHERE Student.StudentNumber = Registered.StudentNumber AND Registered.ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                students.add(rs.getString("StudentNumber"));
            }
            log("Server> Successfully Gotten Students For Class: " + classID);
            return students;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentsInClass> " + ex);
            return null;
        }
    }

    List<Admin> getAllAdmins() {
        List<Admin> admins = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Admin;");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                admins.add(new Admin(rs.getString("Username"), rs.getString("Email")));
            }
            return admins;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
            return null;
        }
    }

    List<AdminSearch> getStudentSearch() {
        List<AdminSearch> studentSearches = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT (FirstName || ' ' || LastName) AS Name, StudentNumber FROM Student");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                studentSearches.add(new AdminSearch("Student", rs.getString("Name"), rs.getString("StudentNumber")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (studentSearches.isEmpty()) {
            studentSearches.add(new AdminSearch("Student", "", ""));
        }
        return studentSearches;
    }

    List<AdminSearch> getLecturerSearch() {
        List<AdminSearch> lecturerSearches = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT (FirstName || ' ' || LastName) AS Name, LecturerID FROM Lecturer");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                lecturerSearches.add(new AdminSearch("Lecturer", rs.getString("Name"), rs.getString("LecturerID")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (lecturerSearches.isEmpty()) {
            lecturerSearches.add(new AdminSearch("Lecturer", "", ""));
        }
        return lecturerSearches;
    }

    List<AdminSearch> getClassSearch() {
        List<AdminSearch> classSearches = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT ModuleNumber, ClassID FROM Class");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                classSearches.add(new AdminSearch("Class", rs.getString("ModuleNumber"), rs.getString("ClassID")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (classSearches.isEmpty()) {
            classSearches.add(new AdminSearch("Class", "", ""));
        }
        return classSearches;
    }

    List<AdminSearch> getContactDetailsSearch() {
        List<AdminSearch> contactDetailsSearches = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Name, Position FROM ContactDetails");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                contactDetailsSearches.add(new AdminSearch("Contact", rs.getString("Name"), rs.getString("Position")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (contactDetailsSearches.isEmpty()) {
            contactDetailsSearches.add(new AdminSearch("Contact", "", ""));
        }
        return contactDetailsSearches;
    }

    List<Notice> getAllNotices() {
        List<Notice> notices = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notice;");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                notices.add(new Notice(rs.getInt("NoticeID"), rs.getString("Heading"), rs.getString("Description"), rs.getString("Tag"), rs.getString("ExpiryDate")));
            }
            return notices;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
            return null;
        }
    }

    List<Notification> getAllNotifications() {
        List<Notification> notifications = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notification;");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                notifications.add(new Notification(rs.getInt("NotificationID"), rs.getString("Heading"), rs.getString("Description"), rs.getString("Tag")));
            }
            return notifications;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
            return null;
        }
    }

    List<LecturerStudentAttendance> getAllStudentsInClassAttendance(String lecturerNumber) {//TODO Test
        ObservableList<LecturerStudentAttendance> studentsAttendance = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student, Registered, Class WHERE Student.StudentNumber = Registered.StudentNumber AND Class.ClassID = Registered.ClassID AND Class.LecturerID = ?;");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<LecturerStudentAttendanceClass> studentAttendance = getStudentsInClassAttendanceClasses(rs.getString("StudentNumber"), rs.getString("LecturerID"));
                LecturerStudentAttendance newStudentAttendance = new LecturerStudentAttendance(rs.getString("FirstName"), rs.getString("LastName"), rs.getString("StudentNumber"), studentAttendance);
                if (!studentsAttendance.contains(newStudentAttendance)) {
                    studentsAttendance.add(newStudentAttendance);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
        return studentsAttendance;
    }

    private List<LecturerStudentAttendanceClass> getStudentsInClassAttendanceClasses(String studentNumber, String lecturerNumber) {
        ObservableList<LecturerStudentAttendanceClass> studentsInClassAttendanceClasses = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.StudentNumber = ? AND Registered.ClassID = Class.ClassID AND Class.LecturerID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<Attendance> studentAttendanceClass = getStudentsInClassAttendanceAttendance(studentNumber, rs.getInt("ClassID"));
                studentsInClassAttendanceClasses.add(new LecturerStudentAttendanceClass(rs.getInt("ClassID"), studentAttendanceClass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentsInClassAttendanceClasses;
    }

    private List<Attendance> getStudentsInClassAttendanceAttendance(String studentNumber, int classID) {
        ObservableList<Attendance> studentsInClassAttendanceAttendance = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Attendance WHERE ClassID = ? AND StudentNumber = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.setString(2, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                studentsInClassAttendanceAttendance.add(new Attendance(rs.getString("ADate"), rs.getString("Attendance")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentsInClassAttendanceAttendance;
    }

    List<LecturerStudentResult> getAllStudentsInClassResults(String lecturerNumber) {//TODO Test
        ObservableList<LecturerStudentResult> addStudentsResults = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student, Registered, Class WHERE Student.StudentNumber = Registered.StudentNumber AND Class.ClassID = Registered.ClassID AND Class.LecturerID = ?;");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<LecturerStudentResultClass> studentResults = getStudentsInClassResultsClasses(rs.getString("StudentNumber"), rs.getString("LecturerID"));
                LecturerStudentResult newStudentResult = new LecturerStudentResult(rs.getString("FirstName"), rs.getString("LastName"), rs.getString("StudentNumber"), studentResults);
                if (!addStudentsResults.contains(newStudentResult)) {
                    addStudentsResults.add(newStudentResult);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
        return addStudentsResults;
    }

    private List<LecturerStudentResultClass> getStudentsInClassResultsClasses(String studentNumber, String lecturerNumber) {
        ObservableList<LecturerStudentResultClass> studentsInClassResultsClasses = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.StudentNumber = ? AND Registered.ClassID = Class.ClassID AND Class.LecturerID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<Result> studentResultClass = getStudentsInClassResultsResults(studentNumber, rs.getInt("ClassID"));
                studentsInClassResultsClasses.add(new LecturerStudentResultClass(rs.getInt("ClassID"), studentResultClass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentsInClassResultsClasses;
    }

    private List<Result> getStudentsInClassResultsResults(String studentNumber, int classID) {
        ObservableList<Result> studentsInClassResultsResults = FXCollections.observableArrayList();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Result, ResultTemplate WHERE Result.ResultTemplateID = ResultTemplate.ResultTemplateID AND Result.StudentNumber = ? AND ResultTemplate.ClassID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                studentsInClassResultsResults.add(new Result(rs.getInt("ResultTemplateID"), studentNumber, rs.getString("ResultName"), rs.getInt("Result"), rs.getInt("ResultMax"), rs.getInt("DPWeight"), rs.getInt("FinalWeight")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentsInClassResultsResults;
    }
    //</editor-fold>

    //<editor-fold desc="Change Password">
    Boolean changeStudentPassword(String studentNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ? WHERE StudentNumber = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            log("Server> Successfully Changed Password For Student: " + studentNumber);
            return preparedStatement.executeUpdate() != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changeStudentPassword> " + ex);
            return false;
        }
    }

    Boolean changeStudentDefaultPassword(String studentNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ?, AssignedPassword = 0 WHERE StudentNumber = ?;");
            System.out.println(newPassword);
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            log("Server> Successfully Changed Password For Student: " + studentNumber);
            return preparedStatement.executeUpdate() != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changeStudentDefaultPassword> " + ex);
            return false;
        }
    }

    Boolean changeAdminDefaultPassword(String username, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Admin SET Password = ?, AssignedPassword = 0 WHERE Username = ?;");
            System.out.println(newPassword);
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, username);
            log("Server> Successfully Changed Password For Admin: " + username);
            return preparedStatement.executeUpdate() != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changeStudentDefaultPassword> " + ex);
            return false;
        }
    }

    Boolean changeLecturerPassword(String lecturerNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ? WHERE LecturerID = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            log("Server> Successfully Changed Password For ClassLecturer: " + lecturerNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changePasswordLecturer> " + ex);
            return false;
        }
    }

    //</editor-fold>

    //<editor-fold desc="Email Passwords">
    void emailStudentPassword(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password, Email FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Emailed Password For Student: " + studentNumber);
                String email = rs.getString("Email");
                String password = rs.getString("Password");
                new Thread(() -> mail.emailPassword(studentNumber, email, password)).start();
            } else {
                log("Server> Failed To Email Password For Student: " + studentNumber);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> emailStudentPassword> " + ex);
        }
    }

    Boolean emailLecturerPassword(String email, String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password, LecturerID FROM Lecturer WHERE Email = ?;");
            preparedStatement.setString(1, email);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Emailed Password For ClassLecturer: " + lecturerNumber);
                return mail.emailPassword(rs.getString("LecturerNumber"), email, rs.getString("Password"));
            } else {
                log("Server> Failed To Email Password For ClassLecturer: " + lecturerNumber);
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> emailLecturerPassword> " + ex);
            return false;
        }
    }

    void resetAdminPassword(String adminUsername, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Admin SET Password = ?, AssignedPassword = 1 WHERE Username = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, adminUsername);
            preparedStatement.executeUpdate();
            Email.resetPassword(adminUsername, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + adminUsername + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Adders">
    private void addStudent(String studentNumber, String qualification, String firstName, String lastName, String email, String contactNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Student (StudentNumber, Qualification, FirstName, LastName, Password, Email, ContactNumber, AssignedPassword) VALUES (?,?,?,?,?,?,?,?);");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, qualification);
            preparedStatement.setString(3, firstName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, "password");
            preparedStatement.setString(6, email);
            preparedStatement.setString(7, contactNumber);
            preparedStatement.setBoolean(8, true);
            log("Admin> Successfully Added Student: " + studentNumber);
            preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addStudent> " + ex);
        }
    }

    private Boolean addLecturer(String lecturerNumber, String firstName, String lastName, String email, String contactNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Lecturer (LecturerID, FirstName, LastName, Password, Email, ContactNumber) VALUES (?,?,?,?,?,?);");
            preparedStatement.setString(1, lecturerNumber);
            preparedStatement.setString(2, firstName);
            preparedStatement.setString(3, lastName);
            preparedStatement.setString(4, "password");
            preparedStatement.setString(5, email);
            preparedStatement.setString(6, contactNumber);
            log("Admin> Successfully Added ClassLecturer: " + lecturerNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addLecturer> " + ex);
            return false;
        }
    }

    public Boolean addAdmin(String username, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Admin (Username, Pasword) VALUES (?,?);");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            log("Admin> Successfully Added Admin: " + username);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addAdmin> " + ex);
            return false;
        }
    }

    private void addClass(String moduleName, String moduleNumber, String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Class (ModuleName, ModuleNumber, LecturerID) VALUES (?,?,?);");
            preparedStatement.setString(1, moduleName);
            preparedStatement.setString(2, moduleNumber);
            preparedStatement.setString(3, lecturerNumber);
            log("Admin> Successfully Added Class: " + moduleNumber);
            preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addClass> " + ex);
        }
    }

    private void addClassTime(int classID, String roomNumber, int dayOfWeek, int startSlot, int endSlot) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ClassTime (ClassID, RoomNumber, DayOfWeek, StartSlot, EndSlot) VALUES (?,?,?,?,?);");
            preparedStatement.setInt(1, classID);
            preparedStatement.setString(2, roomNumber);
            preparedStatement.setInt(3, dayOfWeek);
            preparedStatement.setInt(4, startSlot);
            preparedStatement.setInt(5, endSlot);
            log("Admin> Successfully Added ClassTime For ClassID: " + classID);
            preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addClassTime> " + ex);
        }
    }

    private Boolean addResultTemplate(int classID, int resultMax, int dpWeight, int finalWeight, String resultName) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ResultTemplate (ClassID, ResultMax, DPWeight, FinalWeight, ResultName) VALUES (?,?,?,?,?);");
            preparedStatement.setInt(1, classID);
            preparedStatement.setInt(2, resultMax);
            preparedStatement.setInt(3, dpWeight);
            preparedStatement.setInt(4, finalWeight);
            preparedStatement.setString(5, resultName);
            preparedStatement.execute();
            if (!resultName.matches("SupplementaryExam")) {//TODO String correct
                preparedStatement = con.prepareStatement("SELECT ResultTemplateID FROM ResultTemplate WHERE ClassID = ? AND ResultMax = ? AND DPWeight = ? AND FinalWeight = ? AND ResultName = ?;");
                preparedStatement.setInt(1, classID);
                preparedStatement.setInt(2, resultMax);
                preparedStatement.setInt(3, dpWeight);
                preparedStatement.setInt(4, finalWeight);
                preparedStatement.setString(5, resultName);
                ResultSet rs = preparedStatement.executeQuery();
                addResultForClass(classID, rs.getInt("ResultTemplateID"));
                notifyUpdatedClass(classID);
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addResultTemplate> " + ex);
            return false;
        }
    }

    private void addResultForClass(int classID, int resultTemplateID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Registered WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PreparedStatement preparedStatement2 = con.prepareStatement("INSERT INTO Result (ResultTemplateID, StudentNumber, Result) VALUES (?,?,?);");
                preparedStatement2.setInt(1, resultTemplateID);
                preparedStatement2.setString(2, rs.getString("StudentNumber"));
                preparedStatement2.setInt(3, -1);
                preparedStatement2.execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> registerStudentForClass> " + ex);
        }
    }

    private void addResult(int resultTemplateID, String studentNumber, int result) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Result (ResultTemplateID, StudentNumber, Result) VALUES (?,?,?);");
            preparedStatement.setInt(1, resultTemplateID);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.setInt(3, result);
            log("Admin> Successfully Added Result For Student: " + studentNumber + " For ResultTemplate: " + resultTemplateID);
            preparedStatement.execute();
            notifyUpdatedStudent(studentNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addResult> " + ex);
        }
    }

    private void addNotice(String heading, String description, String expiryDate, String tag) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Notice (Heading, Description, ExpiryDate, Tag) VALUES (?,?,?,?);");
            preparedStatement.setString(1, heading);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, expiryDate);
            preparedStatement.setString(4, tag);
            log("Admin> Successfully Added Notice: " + heading + " For: " + tag);
            preparedStatement.execute();
            notifyUpdatedNotices(tag);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addNotice> " + ex);
        }
    }

    private void addRegisterResults(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ResultTemplate WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                if (!rs.getString("ResultName").matches("SupplementaryExam")) {//TODO String correct
                    PreparedStatement preparedStatement2 = con.prepareStatement("INSERT INTO Result (ResultTemplateID, StudentNumber, Result) VALUES (?,?,?);");
                    preparedStatement2.setInt(1, rs.getInt("ResultTemplateID"));
                    preparedStatement2.setString(2, studentNumber);
                    preparedStatement2.setInt(3, -1);
                    preparedStatement2.execute();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> registerStudentForClass> " + ex);
        }
    }

    //TODO Lecturer addAttendance
    public Boolean addAttendance(int classID, String studentNumber, String aDate, String attendance) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Attendance (ClassID, StudentNumber, ADate, Attendance) VALUES (?,?,?,?);");
            preparedStatement.setInt(1, classID);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.setString(3, aDate);
            preparedStatement.setString(4, attendance);
            log("Admin> Successfully Added Attendance For Student: " + studentNumber + " For Class: " + classID + " On: " + aDate);
            preparedStatement.execute();
            notifyUpdatedStudent(studentNumber);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addAttendance> " + ex);
            return false;
        }
    }

    private void addContactDetails(String name, String position, String department, String contactNumber, String email) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ContactDetails (Name, Position, Department, ContactNumber, Email) VALUES (?,?,?,?,?);");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, position);
            preparedStatement.setString(3, department);
            preparedStatement.setString(4, contactNumber);
            preparedStatement.setString(5, email);
            log("Admin> Successfully Added Notice: ");
            preparedStatement.execute();
            notifyUpdatedAll();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addNotice> " + ex);
        }
    }

    private void addNotification(String heading, String description, String tag) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Notification (Heading, Description, Tag) VALUES (?,?,?);");
            preparedStatement.setString(1, heading);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, tag);
            log("Admin> Successfully Added Notice: " + heading + " For: " + tag);
            preparedStatement.execute();
            notifyUpdatedNotices(tag);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addNotice> " + ex);
        }
    }

    Boolean addStudentToClass(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Registered (StudentNumber, ClassID) VALUES (?,?);");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            preparedStatement.execute();
            addRegisterResults(studentNumber, classID);
            notifyUpdatedStudent(studentNumber);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> registerStudentForClass> " + ex);
            return false;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Updaters">
    //TODO if studentNumber change
    Boolean updateStudent(String studentNumber, String qualification, String firstName, String lastName, String email, String contactNumber, List<ClassResultAttendance> classResultAttendances) {
        try {
            if (isStudentRegistered(studentNumber)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET StudentNumber = ? AND Qualification = ? AND FirstName = ? AND LastName = ? AND Email = ? AND ContactNumber = ? WHERE StudentNumber = ?;");
                preparedStatement.setString(1, studentNumber);
                preparedStatement.setString(2, qualification);
                preparedStatement.setString(3, firstName);
                preparedStatement.setString(4, lastName);
                preparedStatement.setString(5, email);
                preparedStatement.setString(6, contactNumber);
                preparedStatement.executeQuery().next();

                for (int i = 0; i < classResultAttendances.size(); i++) {
                    if (isRegisteredForClass(studentNumber, classResultAttendances.get(i).getStudentClass().getClassID())) {
                        for (int j = 0; j < classResultAttendances.size(); j++) {
                            preparedStatement = con.prepareStatement("UPDATE Attendance SET Attendance = ? WHERE StudentNumber = ? AND aDate = ?");
                            preparedStatement.setString(1, classResultAttendances.get(i).getAttendance().get(j).getAttendance());
                            preparedStatement.setString(2, studentNumber);
                            preparedStatement.setString(3, classResultAttendances.get(i).getAttendance().get(j).getAttendanceDate());
                            preparedStatement.executeQuery().next();
                        }
                        for (int k = 0; k < classResultAttendances.size(); k++) {
                            preparedStatement = con.prepareStatement("UPDATE Result SET Result = ? WHERE StudentNumber = ? AND ResultTemplateID = ?");
                            preparedStatement.setDouble(1, classResultAttendances.get(i).getResults().get(k).getResult());
                            preparedStatement.setString(2, studentNumber);
                            preparedStatement.setInt(3, classResultAttendances.get(i).getResults().get(k).getResultTemplateID());
                            preparedStatement.executeQuery().next();
                        }
                    } else {
                        addStudentToClass(studentNumber, classResultAttendances.get(i).getStudentClass().getClassID());
                    }
                    checkStudentClassesToRemove(studentNumber, classResultAttendances);
                }
                notifyUpdatedStudent(studentNumber);
            } else {
                addStudent(studentNumber, qualification, firstName, lastName, email, contactNumber);
                for (int i = 0; i < classResultAttendances.size(); i++) {
                    addStudentToClass(studentNumber, classResultAttendances.get(i).getStudentClass().getClassID());
                }
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    Boolean updateLecturer(String lecturerNumber, String firstName, String lastName, String email, String contactNumber) {
        try {
            if (isLecturerRegistered(lecturerNumber)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET LecturerID = ? AND FirstName = ? AND LastName = ? AND Email = ? AND ContactNumber = ? WHERE LecturerID = ?;");
                preparedStatement.setString(1, lecturerNumber);
                preparedStatement.setString(2, firstName);
                preparedStatement.setString(3, lastName);
                preparedStatement.setString(4, email);
                preparedStatement.setString(5, contactNumber);
                preparedStatement.executeQuery().next();
                notifyUpdatedLecturer(lecturerNumber);
                return true;
            } else {
                return addLecturer(lecturerNumber, firstName, lastName, email, contactNumber);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateLecturer> " + ex);
            return false;
        }
    }

    Boolean updateClass(int classID, String moduleName, String moduleNumber, String lecturerNumber, List<ClassTime> classTime) {
        try {
            if (isClassRegistered(classID)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Class SET ClassID = ? AND ModuleName = ? AND ModuleNumber = ? AND LecturerID = ? WHERE ClassID = ?;");
                preparedStatement.setInt(1, classID);
                preparedStatement.setString(2, moduleName);
                preparedStatement.setString(3, moduleNumber);
                preparedStatement.setString(4, lecturerNumber);
                preparedStatement.setInt(5, classID);
                preparedStatement.executeQuery().next();
                for (int i = 0; i < classTime.size(); i++) {
                    updateClassTime(classTime.get(i).getId(), classID, classTime.get(i).getRoomNumber(), classTime.get(i).getDayOfWeek(), classTime.get(i).getStartSlot(), classTime.get(i).getEndSlot());
                }
                notifyUpdatedClass(classID);
                return true;
            } else {
                addClass(moduleName, moduleNumber, lecturerNumber);
                PreparedStatement preparedStatement = con.prepareStatement("SELECT ClassID FROM Class WHERE ModuleName = ? AND ModuleNumber = ? AND LecurerID = ?;");
                preparedStatement.setString(1, moduleName);
                preparedStatement.setString(2, moduleNumber);
                preparedStatement.setString(3, lecturerNumber);
                ResultSet rs = preparedStatement.executeQuery();
                rs.next();
                for (int i = 0; i < classTime.size(); i++) {
                    addClassTime(classTime.get(i).getClassID(), classTime.get(i).getRoomNumber(), classTime.get(i).getDayOfWeek(), classTime.get(i).getStartSlot(), classTime.get(i).getEndSlot());
                }
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClass> " + ex);
            return false;
        }
    }

    private void updateClassTime(int classTimeID, int classID, String roomNumber, int dayOfWeek, int startSlot, int endSlot) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE ClassTime SET ClassTimeID = ? AND ClassID = ? AND RommNumber = ? AND DayOfWeek = ? AND StartSlot = ? AND EndSlot = ? WHERE ClassTime = ?;");
            preparedStatement.setInt(1, classTimeID);
            preparedStatement.setInt(2, classID);
            preparedStatement.setString(3, roomNumber);
            preparedStatement.setInt(4, dayOfWeek);
            preparedStatement.setInt(5, startSlot);
            preparedStatement.setInt(6, endSlot);
            preparedStatement.setInt(7, classTimeID);
            preparedStatement.executeQuery().next();
            notifyUpdatedClass(classID);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClassTime> " + ex);
        }
    }

    Boolean updateResultTemplate(int resultTemplateID, int classID, int resultMax, int dpWeight, int finalWeight, String resultName) {
        if (resultTemplateID > -1) {
            try {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE ResultTemplate SET ResultTemplateID = ? AND ClassID = ? AND ResultMax = ? AND DPWeight = ? AND FinalWeight = ? AND ResultName = ? WHERE ResultTemplateID = ?;");
                preparedStatement.setInt(1, resultTemplateID);
                preparedStatement.setInt(2, classID);
                preparedStatement.setInt(3, resultMax);
                preparedStatement.setInt(4, dpWeight);
                preparedStatement.setInt(5, finalWeight);
                preparedStatement.setString(6, resultName);
                preparedStatement.setInt(7, resultTemplateID);
                log("Admin> Successfully Updated ResultTemplate: ");
                preparedStatement.executeQuery().next();
                notifyUpdatedClass(classID);
                return true;
            } catch (SQLException ex) {
                ex.printStackTrace();
                log("Server> updateResultTemplate> " + ex);
                return false;
            }
        } else {
            return addResultTemplate(classID, resultMax, dpWeight, finalWeight, resultName);
        }
    }

    Boolean updateResult(int resultTemplateID, String studentNumber, int result) {
        try {
            if (isResultRegistered(resultTemplateID, studentNumber)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Result SET Result = ? WHERE resultTemplateID = ? AND StudentNumber = ?;");
                preparedStatement.setInt(1, result);
                preparedStatement.setInt(2, resultTemplateID);
                preparedStatement.setString(3, studentNumber);
                log("Admin> Successfully Updated Result: ");
                preparedStatement.executeQuery().next();
            } else {
                addResult(resultTemplateID, studentNumber, result);
            }
            notifyUpdatedStudent(studentNumber);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateResult> " + ex);
            return false;
        }
    }

    Boolean updateNotice(int noticeID, String heading, String description, String expiryDate, String tag) {
        try {
            if (isNoticeRegistered(noticeID)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Notice SET NoticeID = ? AND Heading = ? AND Description = ? AND ExpiryDate = ? AND Tag = ? WHERE NoticeID = ?;");
                preparedStatement.setInt(1, noticeID);
                preparedStatement.setString(2, heading);
                preparedStatement.setString(3, description);
                preparedStatement.setString(4, expiryDate);
                preparedStatement.setString(5, tag);
                preparedStatement.setInt(6, noticeID);
                log("Admin> Successfully Updated Notice: ");
                preparedStatement.executeQuery().next();
                notifyUpdatedNotices(tag);
                return true;
            } else {
                addNotice(heading, description, expiryDate, tag);
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }

    Boolean updateNotification(int notificationID, String heading, String description, String tag) {
        try {
            if (isNotificationRegistered(notificationID)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Notification SET NotificationID = ? AND Heading = ? AND Description = ? AND Tag = ? WHERE NotificationID = ?;");
                preparedStatement.setInt(1, notificationID);
                preparedStatement.setString(2, heading);
                preparedStatement.setString(3, description);
                preparedStatement.setString(4, tag);
                preparedStatement.setInt(5, notificationID);
                preparedStatement.executeQuery().next();
                notifyUpdatedNotices(tag);
                return true;
            } else {
                addNotification(heading, description, tag);
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }

    Boolean updateContactDetails(int id, String name, String position, String department, String contactNumber, String email) {
        try {
            if (isContactDetailRegistered(id)) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE ContactDetails SET Name = ? AND Position = ? AND Department = ? AND contactNumber = ? AND email = ? WHERE ContactDetailsID = ?;");
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, position);
                preparedStatement.setString(3, department);
                preparedStatement.setString(4, contactNumber);
                preparedStatement.setString(5, email);
                preparedStatement.setInt(6, id);
                preparedStatement.executeQuery().next();
                notifyUpdatedAll();
                return true;
            } else {
                addContactDetails(name, position, department, contactNumber, email);
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }

    private Boolean updateAttendance(int attendanceID, int classID, String studentNumber, String aDate, String attendance) {//add
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Attendance SET AttendanceID = ? AND ClassID = ? AND StudentNumber = ? AND ADate = ? AND Attendance = ? WHERE AttendanceID = ?;");
            preparedStatement.setInt(1, attendanceID);
            preparedStatement.setInt(2, classID);
            preparedStatement.setString(3, studentNumber);
            preparedStatement.setString(4, aDate);
            preparedStatement.setString(5, attendance);
            preparedStatement.setInt(6, attendanceID);
            log("Admin> Successfully Updated Attendance: ");
            preparedStatement.executeQuery().next();
            notifyUpdatedStudent(studentNumber);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Is Registered">
    private Boolean isStudentRegistered(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isRegisteredForClass(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Registered WHERE StudentNumber = ? AND ClassID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isLecturerRegistered(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerID = ?;");
            preparedStatement.setString(1, lecturerNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isClassRegistered(int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isResultRegistered(int resultTemplateID, String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Result WHERE ResultTemplateID = ? AND StudentNumber = ?;");
            preparedStatement.setInt(1, resultTemplateID);
            preparedStatement.setString(2, studentNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isNoticeRegistered(int noticeID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notice WHERE NoticeID = ?;");
            preparedStatement.setInt(1, noticeID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isNotificationRegistered(int notificationID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notification WHERE NotificationID = ?;");
            preparedStatement.setInt(1, notificationID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    private Boolean isContactDetailRegistered(int contactDetailsID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ContactDetails WHERE ContactDetailsID = ?;");
            preparedStatement.setInt(1, contactDetailsID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Notify">
    private void notifyUpdatedStudent(String studentNumber) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof StudentConnectionHandler) {
                if (((StudentConnectionHandler) ch).getStudentNumber().matches(studentNumber)) {
                    ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    return;
                }
            }
        }
    }

    private void notifyUpdatedLecturer(String lecturerNumber) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                if (((LecturerConnectionHandler) ch).getLecturerNumber().matches(lecturerNumber)) {
                    ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
                }
            } else if (ch instanceof StudentConnectionHandler) {
                for (int i = 0; i < ((StudentConnectionHandler) ch).getStudent().getClassResultAttendances().size(); i++) {
                    if (((StudentConnectionHandler) ch).getStudent().getClassResultAttendances().get(i).getStudentClass().getClassLecturer().getLecturerID().matches(lecturerNumber)) {
                        ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    }
                }
            }
        }
    }

    private void notifyUpdatedClass(int classID) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                for (int i = 0; i < ((LecturerConnectionHandler) ch).getLecturer().getClasses().size(); i++) {
                    if (((LecturerConnectionHandler) ch).getLecturer().getClasses().get(i).getId() == classID) {
                        ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
                    }
                }
            } else if (ch instanceof StudentConnectionHandler) {
                for (int i = 0; i < ((StudentConnectionHandler) ch).getStudent().getClassResultAttendances().size(); i++) {
                    if (((StudentConnectionHandler) ch).getStudent().getClassResultAttendances().get(i).getStudentClass().getClassID() == classID) {
                        ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    }
                }
            }
        }
    }

    private void notifyUpdatedNotices(String tag) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                if (((LecturerConnectionHandler) ch).getLecturer().getLecturerNumber().matches(tag) || tag.matches("all")) {
                    ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
                }
            } else if (ch instanceof StudentConnectionHandler) {
                if (((StudentConnectionHandler) ch).getStudent().getStudentNumber().matches(tag) || ((StudentConnectionHandler) ch).getStudent().getQualification().matches(tag) || tag.matches("all")) {
                    ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                }
            }
        }
    }

    private void notifyUpdatedAll() {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
            } else if (ch instanceof StudentConnectionHandler) {
                ((StudentConnectionHandler) ch).updateStudent.setValue(true);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Remove">
    void removeStudent(String studentNumber) {
        Student s = getStudent(studentNumber);
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, s.getStudentNumber());
            preparedStatement.executeQuery().next();
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE Tag = ?;");
            preparedStatement.setString(1, s.getStudentNumber());
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Notice WHERE Tag = ?;");
            preparedStatement.setString(1, s.getStudentNumber());
            preparedStatement.executeQuery().next();
            for (int i = 0; i < s.getClassResultAttendances().size(); i++) {
                removeStudentFromClass(s.getStudentNumber(), s.getClassResultAttendances().get(i).getStudentClass().getClassID());
                preparedStatement = con.prepareStatement("DELETE FROM Result WHERE StudentNumber = ?;");
                preparedStatement.setString(1, s.getStudentNumber());
                preparedStatement.executeQuery().next();
                preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE StudentNumber = ?;");
                preparedStatement.setString(1, s.getStudentNumber());
            }
            notifyUpdatedStudent(studentNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeStudentFromClass(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Registered WHERE StudentNumber = ? AND ClassID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            log("Admin> Successfully Removed Student: " + studentNumber + " From Class: " + classID);
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(studentNumber);
            //TODO remove others (results, result templates, attendance)
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeLecturer(String lecturerNumber) {
        Lecturer l = getLecturer(lecturerNumber);
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Lecturer WHERE LecturerID = ?;");
            preparedStatement.setString(1, l.getLecturerNumber());
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE Tag = ?;");
            preparedStatement.setString(1, l.getLecturerNumber());
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Notice WHERE Tag = ?;");
            preparedStatement.setString(1, l.getLecturerNumber());
            preparedStatement.executeQuery().next();
            for (int i = 0; i < l.getClasses().size(); i++) {
                preparedStatement = con.prepareStatement("UPDATE Class SET LecturerID = -1 WHERE ClassID = ?;");
                preparedStatement.setInt(1, l.getClasses().get(i).getId());
                preparedStatement.executeQuery().next();
            }
            notifyUpdatedLecturer(lecturerNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeClass(int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Class WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Registered WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM ClassTime WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM ResultTemplate WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeQuery().next();
            notifyUpdatedClass(classID);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeResultTemplate(int resultTemplateID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT ClassID FROM ResultTemplate WHERE ResultTemplateID = ?;");
            preparedStatement.setInt(1, resultTemplateID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            int classID = rs.getInt("ClassID");
            preparedStatement = con.prepareStatement("DELETE FROM ResultTemplate WHERE ResultTemplateID = ?;");
            preparedStatement.setInt(1, resultTemplateID);
            preparedStatement.executeQuery().next();
            preparedStatement = con.prepareStatement("DELETE FROM Result WHERE ResultTemplateID = ?;");
            preparedStatement.setInt(1, resultTemplateID);
            preparedStatement.executeQuery().next();
            notifyUpdatedClass(classID);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeNotice(int noticeID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Tag FROM Notice WHERE NoticeID = ?;");
            preparedStatement.setInt(1, noticeID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            String tag = rs.getString("Tag");
            preparedStatement = con.prepareStatement("DELETE FROM Notice WHERE NoticeID = ?;");
            preparedStatement.setInt(1, noticeID);
            preparedStatement.executeQuery().next();
            notifyUpdatedNotices(tag);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeNotification(int notificationID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE NotificationID = ?;");
            preparedStatement.setInt(1, notificationID);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeContactDetails(int contactDetailsID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM ContactDetails WHERE ContactDetailsID = ?;");
            preparedStatement.setInt(1, contactDetailsID);
            preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeImportantDate(int importantDateID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM ImporantDate WHERE ImporantDateID = ?;");
            preparedStatement.setInt(1, importantDateID);
            preparedStatement.executeQuery().next();
            notifyUpdatedAll();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }
    //</editor-fold>

    private void checkStudentClassesToRemove(String studentNumber, List<ClassResultAttendance> newClasses) {
        List<ClassResultAttendance> regClasses = getStudentClassesResultsAttendance(studentNumber);
        regClasses.removeAll(newClasses);//TODO Test
        for (ClassResultAttendance regClass : regClasses) {
            removeStudentFromClass(studentNumber, regClass.getStudentClass().getClassID());
        }
    }

    Boolean isDefaultStudentPassword(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ? AND AssignedPassword = 1;");
            preparedStatement.setString(1, studentNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> isDefaultStudentPassword> " + ex);
            return false;
        }
    }

    Boolean isDefaultAdminPassword(String username) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Admin WHERE Username = ? AND AssignedPassword = 1;");
            preparedStatement.setString(1, username);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> isDefaultAdminPassword> " + ex);
            return false;
        }
    }

    private String calculateNewPassword() {
        String newPassword = "";
        for (int i = 0; i < 8; i++) {
            int random = (int) ((Math.random() * 26) + 65);
            if (Math.round(Math.random()) == 1) {
                random += 32;
            }
            newPassword += ((char) random) + "";
        }
        System.out.println("Random password:" + newPassword);
        return newPassword;
    }

    void log(String logDetails) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.out.println(dateFormat.format(date) + " : " + logDetails);
            File logFile = Server.LOG_FILE.getAbsoluteFile();
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(dateFormat.format(date) + " : " + logDetails);
            bw.newLine();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

