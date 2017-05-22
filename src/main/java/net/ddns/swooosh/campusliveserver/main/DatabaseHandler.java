package net.ddns.swooosh.campusliveserver.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DatabaseHandler {

    private Connection con;
    private Email mail = new Email();

    public DatabaseHandler() {
        connectDB();
    }

    public Boolean connectDB() {
        try {
            Boolean createDatabase = false;
            if (!Server.DATABASE_FILE.exists()) {
                createDatabase = true;
            }
            con = DriverManager.getConnection("jdbc:sqlite:" + Server.DATABASE_FILE.getAbsolutePath());
            if (createDatabase) {
                Statement stmt = con.createStatement();
                stmt.execute("CREATE TABLE Student (" +
                        "StudentNumber text PRIMARY KEY, " +
                        "Campus text, " +
                        "Qualification text, " +
                        "FirstName text, " +
                        "LastName text, " +
                        "Password text, " +
                        "Email text, " +
                        "ContactNumber text);");
                stmt.execute("CREATE TABLE Registered (" +
                        "RegisteredID integer PRIMARY KEY AUTOINCREMENT, " +
                        "StudentNumber text, " +
                        "ClassID integer);");
                stmt.execute("CREATE TABLE Attendance (" +
                        "AttendanceID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID integer, " +
                        "StudentNumber text, " +
                        "ADate text, " +
                        "Attendance text);");
                stmt.execute("CREATE TABLE ClassTime (" +
                        "ClassTimeID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID integer, " +
                        "RoomNumber text, " +
                        "DayOfWeek integer, " +
                        "StartSlot integer, " +
                        "EndSlot integer);");
                stmt.execute("CREATE TABLE Class (" +
                        "ClassID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ModuleName text, " +
                        "ModuleNumber text, " +
                        "LecturerNumber text);");
                stmt.execute("CREATE TABLE Lecturer (" +
                        "LecturerNumber text PRIMARY KEY, " +
                        "Campus text, " +
                        "FirstName text, " +
                        "LastName text, " +
                        "Password text, " +
                        "Email text, " +
                        "ContactNumber text);");
                stmt.execute("CREATE TABLE Result (" +
                        "ResultID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ResultTemplateID integer, " +
                        "StudentNumber text, " +
                        "Result integer);");
                stmt.execute("CREATE TABLE ResultTemplate (" +
                        "ResultTemplateID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID integer, " +
                        "ResultMax integer, " +
                        "DPWeight integer, " +
                        "FinalWeight integer, " +
                        "ResultName text);");
                stmt.execute("CREATE TABLE NoticeBoard (" +
                        "NoticeBoardID integer PRIMARY KEY AUTOINCREMENT, " +
                        "Heading text, " +
                        "Description text, " +
                        "ExpiryDate text, " +
                        "Tag text);");
                stmt.execute("CREATE TABLE Admin (" +
                        "Username text, " +
                        "Password text);");
                log("Server> Created Database");
            }
            System.out.println("net.ddns.swooosh.campusliveserver.main.Server> Connected to database");
            log("Server> Connected to database");
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> connectDB> " + ex);
            return false;
        }
    }

    public Boolean authoriseStudent(String studentNumber, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ? AND Password = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseStudent> " + ex);
            return false;
        }
    }

    public Boolean authoriseLecturer(String lecturerNumber, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerNumber = ? AND Password = ?");
            preparedStatement.setString(1, lecturerNumber);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseLecturer> " + ex);
            return false;
        }
    }

    public Boolean authoriseAdmin(String username, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Admin WHERE Username = ? AND Password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> authoriseAdmin> " + ex);
            return false;
        }
    }

    public Student getStudent(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            List<ClassAndResult> classAndResults = getStudentClassesAndResults(studentNumber);
            Student student = new Student(rs.getString("StudentNumber"), rs.getString("Campus"), rs.getString("Qualification"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), classAndResults);
            log("Server> Successfully Created Student: " + studentNumber);
            return student;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudent> " + ex);
            return null;
        }
    }

    public List<ClassAndResult> getStudentClassesAndResults(String studentNumber) {
        try {
            List<ClassAndResult> classAndResults = new ArrayList<>();
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.StudentNumber = ? AND Registered.ClassID = Class.ClassID");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<Result> results = getStudentResults(studentNumber, rs.getInt("ClassID"));
                StudentClass studentClass = getStudentClass(rs.getInt("ClassID"), studentNumber);
                classAndResults.add(new ClassAndResult(studentClass, results));
            }
            log("Server> Successfully Created Classes And Results For Student: " + studentNumber);
            return classAndResults;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentClassesAndResults> " + ex);
            return null;
        }
    }

    public List<Result> getStudentResults(String studentNumber, int classID) {
        List<Result> results = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Result, ResultTemplate WHERE Result.ResultTemplateID = ResultTemplate.ResultTemplateID AND Result.StudentNumber = ? AND ResultTemplate.ClassID = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                results.add(new Result(rs.getString("ResultName"), rs.getDouble("Result"), rs.getDouble("ResultMax"), rs.getDouble("DPWeight"), rs.getDouble("FinalWeight")));
            }
            log("Server> Successfully Created Results For Student: " + studentNumber);
            return results;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentResults> " + ex);
            return null;
        }
    }

    public StudentClass getStudentClass(int classID, String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Lecturer, Registered WHERE Registered.ClassID = Class.ClassID AND Class.LecturerID = Lecturer.LecturerID AND Class.ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            StudentClass studentClass = null;
            if (rs.next()) {
                List<ClassTime> classTimes = getClassTimes(classID, studentNumber);
                List<ClassFile> files = getFiles(classID, studentNumber);
                studentClass = new StudentClass(rs.getString("ModuleName"), rs.getString("ModuleNumber"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("LecturerID"), rs.getString("Email"), classTimes, files);
            }
            log("Server> Successfully Created Class: " + classID + " for Student: " + studentNumber);
            return studentClass;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getStudentClass> " + ex);
            return null;
        }
    }

    public List<ClassTime> getClassTimes(int classID, String studentNumber) {
        List<ClassTime> classTimes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ClassTime WHERE ClassID = ?");
            preparedStatement.setString(1, Integer.toString(classID));
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                classTimes.add(new ClassTime(rs.getString("RoomNumber"), rs.getInt("DayOfWeek"), rs.getInt("StartSlot"), rs.getInt("EndSlot")));
            }
            log("Server> Successfully Created ClassTimes For Class: " + classID + " For Student: " + studentNumber);
            return classTimes;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getClassTimes> " + ex);
            return null;
        }
    }

    public String getStudentPassword(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password From Student WHERE StudentNumber = ?;");
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

    public Boolean changeStudentPassword(String studentNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ? WHERE StudentNumber = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            log("Server> Successfully Changed Password For Student: " + studentNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changeStudentPassword> " + ex);
            return false;
        }
    }

    public Boolean emailStudentPassword(String email, String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password, StudentNumber From Student WHERE Email = ?;");
            preparedStatement.setString(1, email);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Emailed Password For Student: " + studentNumber);
                return mail.emailPassword(rs.getString("StudentNumber"), email, rs.getString("Password"));
            } else {
                log("Server> Failed To Email Password For Student: " + studentNumber);
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> emailStudentPassword> " + ex);
            return false;
        }
    }

    public ObservableList<NoticeBoard> getNoticeBoards(String number) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * From NoticeBoard;");
            ResultSet rs = preparedStatement.executeQuery();
            ObservableList<NoticeBoard> notices = FXCollections.observableArrayList();
            while (rs.next()) {
                NoticeBoard newNotice = new NoticeBoard(rs.getString("Heading"), rs.getString("Description"));
                notices.add(newNotice);
            }
            log("Server> Successfully Gotten Notices For Student/Lecturer: " + number);
            return notices;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getNoticeBoards> " + ex);
            return null;
        }
    }

    private List<ClassFile> getFiles(int classID, String studentNumber) {
        List<ClassFile> files = new ArrayList<>();
        File classFilesDirectory = new File(Server.FILES_FOLDER.getAbsolutePath() + "/" + classID);
        if (classFilesDirectory.exists()) {
            for (File file : classFilesDirectory.listFiles()) {
                files.add(new ClassFile(classID, file.getName(), (int) file.length()));
            }
        }
        log("Server> Successfully Gotten Files For Student: " + studentNumber);
        return files;
    }

    public Lecturer getLecturer(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerNumber = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            List<LecturerClass> classes = getLecturerClasses(lecturerNumber);
            Lecturer student = new Lecturer(rs.getString("LecturerNumber"), rs.getString("Campus"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), classes);
            log("Server> Successfully Created Lecturer: " + lecturerNumber);
            return student;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getLecturer> " + ex);
            return null;
        }
    }

    public List<LecturerClass> getLecturerClasses(String lecturerNumber) {
        List<LecturerClass> classes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class WHERE LecturerNumber = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                classes.add(new LecturerClass(rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassTimes(rs.getInt("ClassID"), lecturerNumber), getFiles(rs.getInt("ClassID"), lecturerNumber)));
            }
            log("Server> Successfully Gotten Classes For Lecturer: " + lecturerNumber);
            return classes;
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getLecturerClasses> " + ex);
            return null;
        }
    }

    public String getLecturerPassword(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password From Lecturer WHERE lecturerNumber = ?;");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Gotten Password For Lecturer: " + lecturerNumber);
                return rs.getString("Password");
            } else {
                log("Server> Failed To Get Password For Lecturer: " + lecturerNumber);
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> getLecturerPassword> " + ex);
            return null;
        }
    }

    public Boolean changePasswordLecturer(String lecturerNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ? WHERE LecturerNumber = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            log("Server> Successfully Changed Password For Lecturer: " + lecturerNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> changePasswordLecturer> " + ex);
            return false;
        }
    }

    public Boolean emailLecturerPassword(String email, String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password, LecturerNumber From Lecturer WHERE Email = ?;");
            preparedStatement.setString(1, email);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                log("Server> Successfully Emailed Password For Lecturer: " + lecturerNumber);
                return mail.emailPassword(rs.getString("LecturerNumber"), email, rs.getString("Password"));
            } else {
                log("Server> Failed To Email Password For Lecturer: " + lecturerNumber);
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> emailLecturerPassword> " + ex);
            return false;
        }
    }

    public List<String> getStudentsInClass(int classID){
        List<String> students = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT StudentNumber FROM Student, Registered WHERE Student.StudentNumber = Registered.StudentNumber AND Registered.ClassID = ?");
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

    public Admin getAdmin(){

        return new Admin();//TODO
    }

    public Boolean addStudent(String studentNumber, String campus, String qualification, String firstName, String lastName, String email, String contactNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Student (StudentNumber, Campus, Qualification, FirstName, LastName, Password, Email, ContactNumber) VALUES (?,?,?,?,?,?,?,?);");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, campus);
            preparedStatement.setString(3, qualification);
            preparedStatement.setString(4, firstName);
            preparedStatement.setString(5, lastName);
            preparedStatement.setString(6, "password");
            preparedStatement.setString(7, email);
            preparedStatement.setString(8, contactNumber);
            log("Admin> Successfully Added Student: " + studentNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addStudent> " + ex);
            return false;
        }
    }

    public Boolean addLecturer(String lecturerNumber, String campus, String firstName, String lastName, String email, String contactNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Lecturer (LecturerNumber, Campus, FirstName, LastName, Password, Email, ContactNumber) VALUES (?,?,?,?,?,?,?);");
            preparedStatement.setString(1, lecturerNumber);
            preparedStatement.setString(2, campus);
            preparedStatement.setString(3, firstName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, "password");
            preparedStatement.setString(6, email);
            preparedStatement.setString(7, contactNumber);
            log("Admin> Successfully Added Lecturer: " + lecturerNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addLecturer> " + ex);
            return false;
        }
    }

    public Boolean addAdmin(String username, String password){
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

    public Boolean addClass(String moduleName, String moduleNumber, String lecturerNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Class (ModuleName, ModuleNumber, LecturerNumber) VALUES (?,?,?);");
            preparedStatement.setString(1, moduleName);
            preparedStatement.setString(2, moduleNumber);
            preparedStatement.setString(3, lecturerNumber);
            log("Admin> Successfully Added Class: " + moduleNumber);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addClass> " + ex);
            return false;
        }
    }

    public Boolean addClassTime(String classID, String roomNumber, String dayOfWeek, String startSlot, String endSlot){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ClassTime (ClassID, RoomNumber, DayOfWeek, StartSlot, EndSlot) VALUES (?,?,?,?,?);");
            preparedStatement.setString(1, classID);
            preparedStatement.setString(2, roomNumber);
            preparedStatement.setString(3, dayOfWeek);
            preparedStatement.setString(4, startSlot);
            preparedStatement.setString(5, endSlot);
            log("Admin> Successfully Added ClassTime For ClassID: " + classID);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addClassTime> " + ex);
            return false;
        }
    }

    public Boolean addResultTemplate(String classID, String resultMax, String dpWeight, String finalWeight, String resultName){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO ResultTemplate (ClassID, ResultMax, DPWeight, FinalWeight, ResultName) VALUES (?,?,?,?,?);");
            preparedStatement.setString(1, classID);
            preparedStatement.setString(2, resultMax);
            preparedStatement.setString(3, dpWeight);
            preparedStatement.setString(4, finalWeight);
            preparedStatement.setString(5, resultName);
            log("Admin> Successfully Added ResultTemplate: " + resultName + " For ClassID: " + classID);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addResultTemplate> " + ex);
            return false;
        }
    }

    public Boolean addResult(String resultTemplateID, String studentNumber, String result){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Result (ResultTemplateID, StudentNumber, Result) VALUES (?,?,?);");
            preparedStatement.setString(1, resultTemplateID);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.setString(3, result);
            log("Admin> Successfully Added Result For Student: " + studentNumber + " For ResultTemplate: " + resultTemplateID);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addResult> " + ex);
            return false;
        }
    }

    public Boolean addNotice(String heading, String description, String expiryDate, String tag){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO NoticeBoard (Heading, Description, ExpiryDate, Tag) VALUES (?,?,?,?);");
            preparedStatement.setString(1, heading);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, expiryDate);
            preparedStatement.setString(4, tag);
            log("Admin> Successfully Added Notice: " + heading + " For: " + tag);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addNotice> " + ex);
            return false;
        }
    }

    public Boolean registerStudentForClass(String studentNumber, String classID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Registered (StudentNumber, ClassID) VALUES (?,?,);");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, classID);
            log("Admin> Successfully Registered Student: " + studentNumber + " For Class: " + classID);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> registerStudentForClass> " + ex);
            return false;
        }
    }

    public Boolean addAttendance(String classID, String studentNumber, String aDate, String attendance){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO Attendance (ClassID, StudentNumber, ADate, Attendance) VALUES (?,?,?,?);");
            preparedStatement.setString(1, classID);
            preparedStatement.setString(2, studentNumber);
            preparedStatement.setString(3, aDate);
            preparedStatement.setString(4, attendance);
            log("Admin> Successfully Added Attendance For Student: " + studentNumber + " For Class: " + classID + " On: " + aDate);
            return preparedStatement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> addAttendance> " + ex);
            return false;
        }
    }

    public Boolean updateStudent(String newStudentNumber, String campus, String qualification, String firstName, String lastName, String password, String email, String contactNumber, String oldStudentNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET StudentNumber = ? AND Campus = ? AND Qualification = ? AND FirstName = ? AND LastName = ? AND Password = ? AND Email = ? AND ContactNumber = ? WHERE StudentNumber = ?;");
            preparedStatement.setString(1, newStudentNumber);
            preparedStatement.setString(2, campus);
            preparedStatement.setString(3, qualification);
            preparedStatement.setString(4, firstName);
            preparedStatement.setString(5, lastName);
            preparedStatement.setString(6, password);
            preparedStatement.setString(7, email);
            preparedStatement.setString(8, contactNumber);
            preparedStatement.setString(9, oldStudentNumber);
            log("Admin> Successfully Student: " + oldStudentNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateStudent> " + ex);
            return false;
        }
    }

    public Boolean updateLecturer(String newLecturerNumber, String campus, String firstName, String lastName, String password, String email, String contactNumber, String oldLecturerNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET LecturerNumber = ? AND Campus = ? AND FirstName = ? AND LastName = ? AND Password = ? AND Email = ? AND ContactNumber = ? WHERE LecturerNumber = ?;");
            preparedStatement.setString(1, newLecturerNumber);
            preparedStatement.setString(2, campus);
            preparedStatement.setString(3, firstName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, password);
            preparedStatement.setString(6, email);
            preparedStatement.setString(7, contactNumber);
            preparedStatement.setString(8, oldLecturerNumber);
            log("Admin> Successfully Updated Lecturer: " + oldLecturerNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateLecturer> " + ex);
            return false;
        }
    }

    public Boolean updateClass(String newClassID, String moduleName, String moduleNumber, String lecturerNumber, String oldClassID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Class SET ClassID = ? AND ModuleName = ? AND ModuleNumber = ? AND LecturerNumber = ? WHERE ClassID = ?;");
            preparedStatement.setString(1, newClassID);
            preparedStatement.setString(2, moduleName);
            preparedStatement.setString(3, moduleNumber);
            preparedStatement.setString(4, lecturerNumber);
            preparedStatement.setString(5, oldClassID);
            log("Admin> Successfully Updated Class: " + oldClassID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClass> " + ex);
            return false;
        }
    }

    public Boolean updateClassTime(String newClassTimeID, String classID, String roomNumber, String dayOfWeek, String startSlot, String endSlot, String oldClassTimeID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE ClassTime SET ClassTimeID = ? AND ClassID = ? AND RommNumber = ? AND DayOfWeek = ? AND StartSlot = ? AND EndSlot = ? WHERE ClassTime = ?;");
            preparedStatement.setString(1, newClassTimeID);
            preparedStatement.setString(2, classID);
            preparedStatement.setString(3, roomNumber);
            preparedStatement.setString(4, dayOfWeek);
            preparedStatement.setString(5, startSlot);
            preparedStatement.setString(6, endSlot);
            preparedStatement.setString(7, oldClassTimeID);
            log("Admin> Successfully Updated ClassTime: " + oldClassTimeID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateClassTime> " + ex);
            return false;
        }
    }

    public Boolean updateResultTemplate(String newResultTemplateID, String classID, String resultMax, String dpWeight, String finalWeight, String resultName, String oldResultTemplateID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE ResultTemplate SET ResultTemplateID = ? AND ClassID = ? AND ResultMax = ? AND DPWeight = ? AND FinalWeight = ? AND ResultName = ? WHERE ResultTemplateID = ?;");
            preparedStatement.setString(1, newResultTemplateID);
            preparedStatement.setString(2, classID);
            preparedStatement.setString(3, resultMax);
            preparedStatement.setString(4, dpWeight);
            preparedStatement.setString(5, finalWeight);
            preparedStatement.setString(6, resultName);
            preparedStatement.setString(7, oldResultTemplateID);
            log("Admin> Successfully Updated ResultTemplate: " + oldResultTemplateID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateResultTemplate> " + ex);
            return false;
        }
    }

    public Boolean updateResult(String newResultID, String resultTemplateID, String studentNumber, String result, String oldResultID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Result SET ResultID = ? AND ResultTemplateID = ? AND StudentNumber = ? AND Result = ? WHERE ResultID = ?;");
            preparedStatement.setString(1, newResultID);
            preparedStatement.setString(2, resultTemplateID);
            preparedStatement.setString(3, studentNumber);
            preparedStatement.setString(4, result);
            preparedStatement.setString(5, oldResultID);
            log("Admin> Successfully Updated Result: " + oldResultID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateResult> " + ex);
            return false;
        }
    }

    public Boolean updateNotice(String newNoticeBoardID, String heading, String description, String expiryDate, String tag, String oldNoticeBoardID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE NoticeBoard SET NoticeBoardID = ? AND Heading = ? AND Description = ? AND ExpiryDate = ? AND Tag = ? WHERE NoticeBoardID = ?;");
            preparedStatement.setString(1, newNoticeBoardID);
            preparedStatement.setString(2, heading);
            preparedStatement.setString(3, description);
            preparedStatement.setString(4, expiryDate);
            preparedStatement.setString(5, tag);
            preparedStatement.setString(6, oldNoticeBoardID);
            log("Admin> Successfully Updated NoticeBoard: " + oldNoticeBoardID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }

    public Boolean updateAttendance(String newAttendanceID, String classID, String studentNumber, String aDate, String attendance, String oldAttendanceID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Attendance SET AttendanceID = ? AND ClassID = ? AND StudentNumber = ? AND ADate = ? AND Attendance = ? WHERE AttendanceID = ?;");
            preparedStatement.setString(1, newAttendanceID);
            preparedStatement.setString(2, classID);
            preparedStatement.setString(3, studentNumber);
            preparedStatement.setString(4, aDate);
            preparedStatement.setString(5, attendance);
            preparedStatement.setString(6, oldAttendanceID);
            log("Admin> Successfully Updated Attendance: " + oldAttendanceID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> updateNotice> " + ex);
            return false;
        }
    }

    public Boolean removeStudentFromClass(String studentNumber, String classID){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM Registered WHERE StudentNumber = ? AND ClassID = ?;");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setString(2, classID);
            log("Admin> Successfully Removed Student: " + studentNumber + " From Class: " + classID);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("Server> removeStudentFromClass> " + ex);
            return false;
        }
    }


    public void log(String logDetails) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
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

