package net.ddns.swooosh.campusliveserver.main;

import models.admin.Admin;
import models.admin.AdminSearch;
import models.all.*;
import models.lecturer.LecturerStudentAttendance;
import models.lecturer.LecturerStudentAttendanceClass;
import models.lecturer.LecturerStudentResult;
import models.lecturer.LecturerStudentResultClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DatabaseHandler {

    private Connection con;

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
                        "AssignedPassword INTEGER, " +
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
                        "AssignedPassword INTEGER, " +
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
                        "AssignedPassword INTEGER, " +
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
    Boolean authoriseStudent(String studentNumber, String password) {
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
                studentClass = new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(classID), classTimes, files, getResultTemplates(classID));
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
                classes.add(new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(rs.getInt("ClassID")), classTimes, files, getResultTemplates(rs.getInt("ClassID"))));
            }
            log("Server> Successfully retrieved all classes");
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getAllStudentClasses> " + ex);
        }
        if (classes.isEmpty()) {
            classes.add(new StudentClass(-1, null, null, null, null, null, null));
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
            return Files.readAllBytes(new File(Server.LECTURER_IMAGES + "/" + lecturerNumber + "/profile.jpg").toPath());
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
                attendance.add(new Attendance(rs.getInt("AttendanceID"), rs.getString("ADate"), rs.getString("Attendance")));
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
            return new StudentClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassLecturer(classID), getClassTimes(classID), null, getResultTemplates(classID));
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
            return null;
        }
    }

    ContactDetails getContactDetail(String name, String position) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ContactDetails WHERE Name = ? AND Position = ?");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, position);
            ResultSet rs = preparedStatement.executeQuery();
            //log("Server> Successfully Created ClassLecturer: " + lecturerNumber);
            return new ContactDetails(rs.getInt("ContactDetailsID"), rs.getString("Name"), rs.getString("Position"), rs.getString("Department"), rs.getString("ContactNumber"), rs.getString("Email"), getContactImage(rs.getString("ContactDetailsID")));
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassLecturer> " + ex);
            return null;
        }
    }

    List<Notice> getNotices(String studentNumber, String qualification) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Notice WHERE Tag = ? OR Tag = ? OR Tag = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, qualification);
            preparedStatement.setString(3, "Campus");
            ResultSet rs = preparedStatement.executeQuery();
            List<Notice> notices = new ArrayList<>();
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

    List<Notification> getNotifications(String studentNumber) {
        try {
            List<Notification> notifications = new ArrayList<>();
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

    List<ContactDetails> getContactDetails() {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ContactDetails;");
            ResultSet rs = preparedStatement.executeQuery();
            List<ContactDetails> contactDetails = new ArrayList<>();
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
            return Files.readAllBytes(new File(Server.CONTACT_IMAGES + "/" + contactID + "/profile.jpg").toPath());
        } catch (Exception ex) {
            System.out.println("Server> Can't find picture for contact, " + contactID);
        }
        return null;
    }

    List<ImportantDate> getImportantDates() {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ImportantDate ORDER BY IDate;");
            ResultSet rs = preparedStatement.executeQuery();
            List<ImportantDate> importantDates = new ArrayList<>();
            while (rs.next()) {
                ImportantDate newImportantDate = new ImportantDate(rs.getInt("ImportantDateID"), rs.getString("IDate"), rs.getString("Description"));
                importantDates.add(newImportantDate);
            }
            log("Server> Successfully Gotten Notices For Student/ClassLecturer: ");
            if (importantDates.isEmpty()) {
                importantDates.add(new ImportantDate(0, "NoImportantDate", ""));
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
        List<ContactDetails> contactDetails = new ArrayList<>();
        for (LecturerClass aClass : classes) {
            try {
                PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student, Registered, Class WHERE Student.StudentNumber = Registered.StudentNumber AND Class.ClassID = Registered.ClassID AND Class.ClassID = ?;");
                preparedStatement.setInt(1, aClass.getId());
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    ContactDetails newContactDetail = new ContactDetails(0, rs.getString("FirstName") + " " + rs.getString("LastName"), rs.getString("StudentNumber"), "Student", rs.getString("ContactNumber"), rs.getString("Email"), null);
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
                classes.add(new LecturerClass(rs.getInt("ClassID"), rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassTimes(rs.getInt("ClassID")), getFiles(rs.getInt("ClassID"))));
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
        List<Admin> admins = new ArrayList<>();
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
                classSearches.add(new AdminSearch("Class", rs.getString("ClassID"), rs.getString("ModuleNumber")));
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
        List<Notice> notices = new ArrayList<>();
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

    private List<ResultTemplate> getResultTemplates(int classId) {
        List<ResultTemplate> resultTemplates = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ResultTemplate WHERE ClassID = ?;");
            preparedStatement.setInt(1, classId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                resultTemplates.add(new ResultTemplate(rs.getInt("ResultTemplateID"), classId, rs.getInt("ResultMax"), rs.getInt("DPWeight"), rs.getInt("FinalWeight"), rs.getString("ResultName")));
            }
            return resultTemplates;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getResultTemplates> " + ex);
            return null;
        }
    }

    List<Notification> getAllNotifications() {
        List<Notification> notifications = new ArrayList<>();
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
        List<LecturerStudentAttendance> studentsAttendance = new ArrayList<>();
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
        List<LecturerStudentAttendanceClass> studentsInClassAttendanceClasses = new ArrayList<>();
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
        List<Attendance> studentsInClassAttendanceAttendance = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Attendance WHERE ClassID = ? AND StudentNumber = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.setString(2, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                studentsInClassAttendanceAttendance.add(new Attendance(rs.getInt("AttendanceID"), rs.getString("ADate"), rs.getString("Attendance")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentsInClassAttendanceAttendance;
    }

    List<LecturerStudentResult> getAllStudentsInClassResults(String lecturerNumber) {//TODO Test
        List<LecturerStudentResult> addStudentsResults = new ArrayList<>();
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
        List<LecturerStudentResultClass> studentsInClassResultsClasses = new ArrayList<>();
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
        List<Result> studentsInClassResultsResults = new ArrayList<>();
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

    void changeLecturerPassword(String lecturerNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ? WHERE LecturerID = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            log("Server> Successfully Changed Password For ClassLecturer: " + lecturerNumber);
            preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changePasswordLecturer> " + ex);
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
                new Thread(() -> Email.emailPassword(studentNumber, email, password)).start();
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
                return Email.emailPassword(rs.getString("LecturerNumber"), email, rs.getString("Password"));
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

    void resetStudentPassword(String studentNumber, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ?, AssignedPassword = 1 WHERE StudentNumber = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.executeUpdate();
            Email.resetPassword(studentNumber, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + studentNumber + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void resetLecturerPassword(String lecturerNumber, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ?, AssignedPassword = 1 WHERE LecturerID = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            preparedStatement.executeUpdate();
            Email.resetPassword(lecturerNumber, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + lecturerNumber + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initAdminPassword(String adminUsername, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Admin SET Password = ?, AssignedPassword = 1 WHERE Username = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, adminUsername);
            preparedStatement.executeUpdate();
            Email.initPassword(adminUsername, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + adminUsername + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initStudentPassword(String studentNumber, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ?, AssignedPassword = 1 WHERE StudentNumber = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.executeUpdate();
            Email.initPassword(studentNumber, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + studentNumber + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initLecturerPassword(String lecturerNumber, String email) {
        try {
            String newPassword = calculateNewPassword();
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ?, AssignedPassword = 1 WHERE LecturerID = ?");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            preparedStatement.executeUpdate();
            Email.initPassword(lecturerNumber, email, newPassword);
            log("Server> resetAdminPassword> Successfully reset " + lecturerNumber + "'s password");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Adders">
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
                if (!rs.getString("ResultName").matches("Supplementary Exam")) {
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

    void addStudentToClass(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Registered (StudentNumber, ClassID) VALUES (?,?);");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            preparedStatement.execute();
            addRegisterResults(studentNumber, classID);
            notifyUpdatedStudent(studentNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> registerStudentForClass> " + ex);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Updaters">
    void updateStudent(Student student) {
        try {
            if (isStudentRegistered(student.getStudentNumber())) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Qualification = ?, FirstName = ?,  LastName = ?, Email = ?, ContactNumber = ? WHERE StudentNumber = ?;");
                preparedStatement.setString(1, student.getQualification());
                preparedStatement.setString(2, student.getFirstName());
                preparedStatement.setString(3, student.getLastName());
                preparedStatement.setString(4, student.getEmail());
                preparedStatement.setString(5, student.getContactNumber());
                preparedStatement.setString(6, student.getStudentNumber());
                preparedStatement.executeUpdate();
                notifyUpdatedStudent(student.getStudentNumber());
                log("Server> updateStudent> Updated student");
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Student (StudentNumber, Qualification, FirstName, LastName, Email, ContactNumber) VALUES (?, ?, ?, ?, ?, ?);");
                preparedStatement.setString(1, student.getStudentNumber());
                preparedStatement.setString(2, student.getQualification());
                preparedStatement.setString(3, student.getFirstName());
                preparedStatement.setString(4, student.getLastName());
                preparedStatement.setString(5, student.getEmail());
                preparedStatement.setString(6, student.getContactNumber());
                preparedStatement.executeUpdate();
                notifyUpdatedStudent(student.getStudentNumber());
                initStudentPassword(student.getStudentNumber(), student.getEmail());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
        }
    }

    void updateLecturer(Lecturer lecturer) {
        try {
            if (isLecturerRegistered(lecturer.getLecturerNumber())) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET FirstName = ?, LastName = ?, Email = ?, ContactNumber = ? WHERE LecturerID = ?;");
                preparedStatement.setString(1, lecturer.getFirstName());
                preparedStatement.setString(2, lecturer.getLastName());
                preparedStatement.setString(3, lecturer.getEmail());
                preparedStatement.setString(4, lecturer.getContactNumber());
                preparedStatement.setString(5, lecturer.getLecturerNumber());
                preparedStatement.executeUpdate();
                saveLecturerImage(lecturer.getLecturerNumber(), lecturer.getImageBytes());
                notifyUpdatedLecturer(lecturer.getLecturerNumber());
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Lecturer(LecturerID, FirstName, LastName, Email, ContactNumber) VALUES (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, lecturer.getFirstName());
                preparedStatement.setString(2, lecturer.getLastName());
                preparedStatement.setString(3, lecturer.getEmail());
                preparedStatement.setString(4, lecturer.getContactNumber());
                preparedStatement.setString(5, lecturer.getLecturerNumber());
                preparedStatement.executeUpdate();
                saveLecturerImage(lecturer.getLecturerNumber(), lecturer.getImageBytes());
                notifyUpdatedLecturer(lecturer.getLecturerNumber());
                initLecturerPassword(lecturer.getLecturerNumber(), lecturer.getEmail());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateLecturer> " + ex);
        }
    }

    void updateClass(StudentClass studentClass) {
        try {
            if (isClassRegistered(studentClass.getClassID())) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Class SET ModuleName = ?, ModuleNumber = ?, LecturerID = ? WHERE ClassID = ?;");
                preparedStatement.setString(1, studentClass.getModuleName());
                preparedStatement.setString(2, studentClass.getModuleNumber());
                preparedStatement.setString(3, studentClass.getClassLecturer().getLecturerID());
                preparedStatement.setInt(4, studentClass.getClassID());
                preparedStatement.executeUpdate();
                notifyUpdatedClass(studentClass.getClassID());
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Class (ModuleName, ModuleNumber, LecturerID) VALUES (?, ?, ?)");
                preparedStatement.setString(1, studentClass.getModuleName());
                preparedStatement.setString(2, studentClass.getModuleNumber());
                preparedStatement.setString(3, studentClass.getClassLecturer().getLecturerID());
                preparedStatement.executeUpdate();
                int classId = -1;
                preparedStatement = con.prepareStatement("SELECT ClassID FROM Class WHERE ModuleName = ? AND ModuleNumber = ? AND LecturerID = ?");
                preparedStatement.setString(1, studentClass.getModuleName());
                preparedStatement.setString(2, studentClass.getModuleNumber());
                preparedStatement.setString(3, studentClass.getClassLecturer().getLecturerID());
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    classId = rs.getInt("ClassID");
                }
                for (ResultTemplate rt : studentClass.getResultTemplates()) {
                    preparedStatement = con.prepareStatement("INSERT INTO ResultTemplate (ClassID, ResultMax, DPWeight, FinalWeight, ResultName) VALUES (?, ?, ?, ?, ?)");
                    preparedStatement.setInt(1, classId);
                    preparedStatement.setInt(2, rt.getResultMax());
                    preparedStatement.setInt(3, rt.getDpWeight());
                    preparedStatement.setInt(4, rt.getFinalWeight());
                    preparedStatement.setString(5, rt.getResultName());
                    preparedStatement.executeUpdate();
                }
                notifyUpdatedClass(studentClass.getClassID());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClass> " + ex);
        }
    }

    void updateClassTime(ClassTime classTime) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ClassTime (ClassID, RoomNumber, DayOfWeek, StartSlot, EndSlot) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, classTime.getClassID());
            preparedStatement.setString(2, classTime.getRoomNumber());
            preparedStatement.setInt(3, classTime.getDayOfWeek());
            preparedStatement.setInt(4, classTime.getStartSlot());
            preparedStatement.setInt(5, classTime.getEndSlot());
            preparedStatement.executeUpdate();
            notifyUpdatedClass(classTime.getClassID());
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClassTime> " + ex);
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

    void updateResultTemplate(List<ResultTemplate> resultTemplates) {
        try {
            for (ResultTemplate resultTemplate : resultTemplates) {
                if (resultTemplate.getId() > -1) {
                    PreparedStatement preparedStatement = con.prepareStatement("UPDATE ResultTemplate SET ClassID = ?, ResultMax = ?, DPWeight = ?, FinalWeight = ?, ResultName = ? WHERE ResultTemplateID = ?;");
                    preparedStatement.setInt(1, resultTemplate.getClassID());
                    preparedStatement.setInt(2, resultTemplate.getResultMax());
                    preparedStatement.setInt(3, resultTemplate.getDpWeight());
                    preparedStatement.setInt(4, resultTemplate.getFinalWeight());
                    preparedStatement.setString(5, resultTemplate.getResultName());
                    preparedStatement.setInt(6, resultTemplate.getId());
                    log("Admin> Successfully Updated ResultTemplate: ");
                    preparedStatement.executeUpdate();
                    notifyUpdatedClass(resultTemplate.getClassID());
                } else {
                    PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ResultTemplate (ClassID, ResultMax, DPWeight, FinalWeight, ResultName) VALUES (?, ?, ?, ?, ?)");
                    preparedStatement.setInt(1, resultTemplate.getClassID());
                    preparedStatement.setInt(2, resultTemplate.getResultMax());
                    preparedStatement.setInt(3, resultTemplate.getDpWeight());
                    preparedStatement.setInt(4, resultTemplate.getFinalWeight());
                    preparedStatement.setString(5, resultTemplate.getResultName());
                    log("Admin> Successfully Updated ResultTemplate: ");
                    preparedStatement.executeUpdate();
                    notifyUpdatedClass(resultTemplate.getClassID());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateResultTemplate> " + ex);
        }

    }

    void updateResult(Result result) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Result SET Result = ? WHERE resultTemplateID = ? AND StudentNumber = ?;");
            preparedStatement.setInt(1, (int) result.getResult());
            preparedStatement.setInt(2, result.getResultTemplateID());
            preparedStatement.setString(3, result.getStudentNumber());
            log("Admin> Successfully Updated Result: ");
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(result.getStudentNumber());
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateResult> " + ex);
        }
    }

    void updateNotice(Notice notice) {
        try {
            if (notice.getId() != -1) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Notice SET Heading = ?, Description = ?,  ExpiryDate = ?, Tag = ? WHERE NoticeID = ?;");
                preparedStatement.setString(1, notice.getHeading());
                preparedStatement.setString(2, notice.getDescription());
                preparedStatement.setString(3, notice.getExpiryDate());
                preparedStatement.setString(4, notice.getTag());
                preparedStatement.setInt(5, notice.getId());
                preparedStatement.executeUpdate();
                notifyUpdatedNotices(notice.getTag());
                log("Admin> Successfully Updated Notice: ");
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Notice (Heading, Description, ExpiryDate, Tag) VALUES (?, ?, ?, ?)");
                preparedStatement.setString(1, notice.getHeading());
                preparedStatement.setString(2, notice.getDescription());
                preparedStatement.setString(3, notice.getExpiryDate());
                preparedStatement.setString(4, notice.getTag());
                preparedStatement.executeUpdate();
                notifyUpdatedNotices(notice.getTag());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
        }
    }

    void updateDate(ImportantDate importantDate) {
        try {
            if (importantDate.getId() != -1) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE ImportantDate SET Description = ?, IDate = ? WHERE ImportantDateID = ?;");
                preparedStatement.setString(1, importantDate.getDescription());
                preparedStatement.setString(2, importantDate.getDate());
                preparedStatement.setInt(3, importantDate.getId());
                preparedStatement.executeUpdate();
                notifyUpdatedDate();
                log("Admin> Successfully Updated Important Date: ");
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ImportantDate (Description, IDate) VALUES (?, ?)");
                preparedStatement.setString(1, importantDate.getDescription());
                preparedStatement.setString(2, importantDate.getDate());
                preparedStatement.executeUpdate();
                notifyUpdatedDate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateDate> " + ex);
        }
    }

    void updateNotification(Notification notification) {
        try {
            if (notification.getId() != -1) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Notification SET Heading = ?, Description = ?, Tag = ? WHERE NotificationID = ?;");
                preparedStatement.setString(1, notification.getHeading());
                preparedStatement.setString(2, notification.getDescription());
                preparedStatement.setString(3, notification.getTag());
                preparedStatement.setInt(4, notification.getId());
                preparedStatement.executeUpdate();
                notifyUpdatedNotification(notification.getTag());
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Notification (Heading, Description, Tag) VALUES (?, ?, ?)");
                preparedStatement.setString(1, notification.getHeading());
                preparedStatement.setString(2, notification.getDescription());
                preparedStatement.setString(3, notification.getTag());
                preparedStatement.executeUpdate();
                notifyUpdatedNotification(notification.getTag());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
        }
    }

    void updateContactDetails(ContactDetails contactDetails) {
        try {
            if (contactDetails.getId() != -1) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE ContactDetails SET Name = ?, Position = ?, Department = ?, ContactNumber = ?, Email = ? WHERE ContactDetailsID = ?;");
                preparedStatement.setString(1, contactDetails.getName());
                preparedStatement.setString(2, contactDetails.getPosition());
                preparedStatement.setString(3, contactDetails.getDepartment());
                preparedStatement.setString(4, contactDetails.getContactNumber());
                preparedStatement.setString(5, contactDetails.getEmail());
                preparedStatement.setInt(6, contactDetails.getId());
                preparedStatement.executeUpdate();
                saveContactImage(contactDetails.getId() + "", contactDetails.getImageBytes());
                notifyUpdatedContact();
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ContactDetails (Name, Position, Department, ContactNumber, Email) VALUES (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, contactDetails.getName());
                preparedStatement.setString(2, contactDetails.getPosition());
                preparedStatement.setString(3, contactDetails.getDepartment());
                preparedStatement.setString(4, contactDetails.getContactNumber());
                preparedStatement.setString(5, contactDetails.getEmail());
                preparedStatement.executeUpdate();
                saveContactImage(contactDetails.getId() + "", contactDetails.getImageBytes());
                notifyUpdatedContact();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateContactDetails> " + ex);
        }
    }

    void updateAdmin(Admin admin) {
        try {
            if (isAdminRegistered(admin.getAdminName())) {
                PreparedStatement preparedStatement = con.prepareStatement("UPDATE Admin SET Email = ? WHERE Username = ?;");
                preparedStatement.setString(1, admin.getAdminEmail());
                preparedStatement.setString(2, admin.getAdminName());
                preparedStatement.executeUpdate();
                notifyAdminUpdate();
            } else {
                PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Admin (Username, Email) VALUES (?, ?)");
                preparedStatement.setString(1, admin.getAdminName());
                preparedStatement.setString(2, admin.getAdminEmail());
                preparedStatement.executeUpdate();
                notifyAdminUpdate();
                initAdminPassword(admin.getAdminName(), admin.getAdminEmail());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void updateAttendance(Attendance attendance) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Attendance SET Attendance = ? WHERE AttendanceID = ?;");
            preparedStatement.setString(1, attendance.getAttendance());
            preparedStatement.setInt(2, attendance.getAttendanceID());
            log("Admin> Successfully Updated Attendance: ");
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("SELECT StudentNumber FROM Attendance WHERE AttendanceID = ?;");
            preparedStatement.setInt(1, attendance.getAttendanceID());
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            notifyUpdatedStudent(rs.getString("StudentNumber"));
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Is Registered">
    private Boolean isAdminRegistered(String username) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Admin WHERE Username = ?;");
            preparedStatement.setString(1, username);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

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
                if (((StudentConnectionHandler) ch).getStudentNumber().equals(studentNumber)) {
                    ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    return;
                }
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateStudents.set(true);
            }
        }
    }

    private void notifyUpdatedLecturer(String lecturerNumber) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                if (((LecturerConnectionHandler) ch).getLecturerNumber().equals(lecturerNumber)) {
                    ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
                }
            } else if (ch instanceof StudentConnectionHandler) {
                for (ClassResultAttendance classResultAttendance : ((StudentConnectionHandler) ch).getStudent().getClassResultAttendances()) {
                    if (classResultAttendance.getStudentClass().getClassLecturer().getLecturerID().equals(lecturerNumber)) {
                        ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    }
                }
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateLecturers.set(true);
                ((AdminConnectionHandler) ch).updateStudents.set(true);
                ((AdminConnectionHandler) ch).updateClasses.set(true);
            }
        }
    }

    private void notifyUpdatedClass(int classID) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                for (LecturerClass lc : ((LecturerConnectionHandler) ch).getLecturer().getClasses()) {
                    if (lc.getId() == classID) {
                        ((LecturerConnectionHandler) ch).updateLecturer.setValue(true);
                    }
                }
            } else if (ch instanceof StudentConnectionHandler) {
                for (ClassResultAttendance cra : ((StudentConnectionHandler) ch).getStudent().getClassResultAttendances()) {
                    if (cra.getStudentClass().getClassID() == classID) {
                        ((StudentConnectionHandler) ch).updateStudent.setValue(true);
                    }
                }
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateLecturers.set(true);
                ((AdminConnectionHandler) ch).updateStudents.set(true);
                ((AdminConnectionHandler) ch).updateClasses.set(true);
            }
        }
    }

    private void notifyUpdatedNotices(String tag) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                if (((LecturerConnectionHandler) ch).getLecturer().getLecturerNumber().equals(tag) || tag.equals("campus")) {
                    ((LecturerConnectionHandler) ch).updateNotices.setValue(true);
                }
            } else if (ch instanceof StudentConnectionHandler) {
                if (((StudentConnectionHandler) ch).getStudent().getStudentNumber().equals(tag) || ((StudentConnectionHandler) ch).getStudent().getQualification().equals(tag) || tag.equals("campus")) {
                    ((StudentConnectionHandler) ch).updateNotices.setValue(true);
                }
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateNotices.setValue(true);
            }
        }
    }

    private void notifyUpdatedNotification(String tag) {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof StudentConnectionHandler) {
                if (((StudentConnectionHandler) ch).getStudent().getStudentNumber().equals(tag)) {
                    ((StudentConnectionHandler) ch).updateNotifications.setValue(true);
                    System.out.println("Should update student");
                }
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateNotifications.setValue(true);
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

    private void notifyAdminUpdate() {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateAdmins.set(true);
            }
        }
    }

    private void notifyUpdatedContact() {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                ((LecturerConnectionHandler) ch).updateContactDetails.set(true);
            } else if (ch instanceof StudentConnectionHandler) {
                ((StudentConnectionHandler) ch).updateContactDetails.setValue(true);
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateContactDetails.set(true);
            }
        }
    }

    private void notifyUpdatedDate() {
        for (ConnectionHandler ch : Server.connectionsList) {
            if (ch instanceof LecturerConnectionHandler) {
                ((LecturerConnectionHandler) ch).updateImportantDates.set(true);
            } else if (ch instanceof StudentConnectionHandler) {
                ((StudentConnectionHandler) ch).updateImportantDates.setValue(true);
            } else if (ch instanceof AdminConnectionHandler) {
                ((AdminConnectionHandler) ch).updateImportantDates.set(true);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Remove">
    void removeStudent(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE Tag = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Notice WHERE Tag = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Registered WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Result WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(studentNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeAdmin(String username) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Admin WHERE Username = ?;");
            preparedStatement.setString(1, username);
            log("Admin> Successfully Removed Admin");
            preparedStatement.executeUpdate();
            notifyAdminUpdate();
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
            preparedStatement = con.prepareStatement("SELECT ResultTemplateID FROM ResultTemplate WHERE ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PreparedStatement preparedStatement2 = con.prepareStatement("DELETE FROM Result WHERE StudentNumber = ? AND ResultTemplateID = ?;");
                preparedStatement2.setString(1, studentNumber);
                preparedStatement2.setInt(2, rs.getInt("ResultTemplateID"));
                preparedStatement2.executeUpdate();
            }
            preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE StudentNumber = ? AND ClassID = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(studentNumber);
            log("Admin> Successfully Removed Student: " + studentNumber + " From Class: " + classID);
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
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE Tag = ?;");
            preparedStatement.setString(1, l.getLecturerNumber());
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Notice WHERE Tag = ?;");
            preparedStatement.setString(1, l.getLecturerNumber());
            preparedStatement.executeUpdate();
            for (LecturerClass lc : l.getClasses()) {
                preparedStatement = con.prepareStatement("UPDATE Class SET LecturerID = '' WHERE ClassID = ?;");
                preparedStatement.setInt(1, lc.getId());
                preparedStatement.executeUpdate();
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
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Registered WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("DELETE FROM ClassTime WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeUpdate();
            preparedStatement = con.prepareStatement("SELECT ResultTemplateID FROM ResultTemplate WHERE ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PreparedStatement preparedStatement2 = con.prepareStatement("DELETE FROM Result WHERE ResultTemplateID = ?;");
                preparedStatement2.setInt(1, rs.getInt("ResultTemplateID"));
                preparedStatement2.executeUpdate();
            }
            preparedStatement = con.prepareStatement("DELETE FROM ResultTemplate WHERE ClassID = ?;");
            preparedStatement.setInt(1, classID);
            preparedStatement.executeUpdate();
            notifyUpdatedClass(classID);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeClassTime(int classTimeID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT ClassID FROM ClassTime WHERE ClassTimeID = ?;");
            preparedStatement.setInt(1, classTimeID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            int classID = rs.getInt("ClassID");
            preparedStatement = con.prepareStatement("DELETE FROM ClassTime WHERE ClassTimeID = ?;");
            preparedStatement.setInt(1, classTimeID);
            preparedStatement.executeUpdate();
            notifyUpdatedClass(classID);
        } catch (Exception ex) {
            ex.printStackTrace();
            log("Server> removeClassTime> " + ex);
        }
    }

    void removeAttendance(int attendanceID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT StudentNumber FROM Attendance WHERE AttendanceID = ?;");
            preparedStatement.setInt(1, attendanceID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            String studentNumber = rs.getString("StudentNumber");
            preparedStatement = con.prepareStatement("DELETE FROM Attendance WHERE AttendanceID = ?;");
            preparedStatement.setInt(1, attendanceID);
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(studentNumber);
        } catch (Exception ex) {
            ex.printStackTrace();
            log("Server> removeAttendance> " + ex);
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
            preparedStatement.executeUpdate();
            notifyUpdatedNotices(tag);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeNotification(int notificationID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Tag FROM Notification WHERE NotificationID = ?;");
            preparedStatement.setInt(1, notificationID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            String tag = rs.getString("Tag");
            preparedStatement = con.prepareStatement("DELETE FROM Notification WHERE NotificationID = ?;");
            preparedStatement.setInt(1, notificationID);
            preparedStatement.executeUpdate();
            notifyUpdatedNotification(tag);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeContactDetails(int contactDetailsID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM ContactDetails WHERE ContactDetailsID = ?;");
            preparedStatement.setInt(1, contactDetailsID);
            preparedStatement.executeUpdate();
            notifyUpdatedContact();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }

    void removeImportantDate(int importantDateID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM ImportantDate WHERE ImportantDateID = ?;");
            preparedStatement.setInt(1, importantDateID);
            preparedStatement.executeUpdate();
            notifyUpdatedDate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
        }
    }
    //</editor-fold>

    void regSuppExam(String studentNumber, int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT ResultTemplateID FROM ResultTemplate WHERE ClassID = ? AND ResultName = 'Supplementary Exam'");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            int resultTemplateID = rs.getInt("ResultTemplateID");
            preparedStatement = con.prepareStatement("INSERT INTO Result (ResultTemplateID, StudentNumber, Result) VALUES (?, ?, -1)");
            preparedStatement.setInt(1, resultTemplateID);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.executeUpdate();
            notifyUpdatedStudent(studentNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> isDefaultStudentPassword> " + ex);
        }
    }

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

    private void saveLecturerImage(String lecturerNumber, byte[] imageBytes) {
        try {
            if (imageBytes != null) {
                File newFile = new File(Server.LECTURER_IMAGES + "/" + lecturerNumber + "/profile.jpg");
                newFile.getParentFile().mkdirs();
                Files.write(newFile.toPath(), imageBytes);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log("Server> saveLecturerImage> " + ex);
        }
    }

    private void saveContactImage(String contactID, byte[] imageBytes) {
        try {
            if (imageBytes != null) {
                File newFile = new File(Server.CONTACT_IMAGES + "/" + contactID + "/profile.jpg");
                newFile.getParentFile().mkdirs();
                Files.write(newFile.toPath(), imageBytes);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log("Server> saveContactImage> " + ex);
        }
    }

    private String calculateNewPassword() {
        StringBuilder newPassword = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int random = (int) ((Math.random() * 26) + 65);
            if (Math.round(Math.random()) == 1) {
                random += 32;
            }
            newPassword.append((char) random).append("");
        }
        System.out.println("Random password:" + newPassword);
        return newPassword.toString();
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

