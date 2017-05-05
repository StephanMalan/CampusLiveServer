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
                        "Date text, " +
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
                        "LecturerID text PRIMARY KEY, " +
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
                        "Description text);");
                stmt.execute("CREATE TABLE Admin (" +
                        "Username text, " +
                        "Password text);");
            }
            System.out.println("Server> Connected to database");
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
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
            System.out.println("Server> authoriseStudent> " + ex);
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
            System.out.println("Server> authoriseLecturer> " + ex);
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
            System.out.println("Server> authoriseAdmin> " + ex);
            return false;
        }
    }

    public Student getStudent(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            List<ClassAndResult> classAndResults = getClassesAndResults(studentNumber);
            Student student = new Student(rs.getString("StudentNumber"), rs.getString("Campus"), rs.getString("Qualification"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), classAndResults);
            System.out.println(student.getClassAndResults().get(0).getResults().get(0).getResult());
            return student;
        } catch (SQLException ex) {
            System.out.println("Server> getStudent> " + ex);
            return null;
        }
    }

    public List<ClassAndResult> getClassesAndResults(String studentNumber) {
        try {
            List<ClassAndResult> classAndResults = new ArrayList<>();
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Registered WHERE Registered.StudentNumber = ? AND Registered.ClassID = Class.ClassID");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                List<Result> results = getResults(studentNumber, rs.getInt("ClassID"));
                StudentClass studentClass = getStudentClass(rs.getInt("ClassID"));
                classAndResults.add(new ClassAndResult(studentClass, results));
            }
            return classAndResults;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public List<Result> getResults(String studentNumber, int classID) {
        List<Result> results = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Result, ResultTemplate WHERE Result.ResultTemplateID = ResultTemplate.ResultTemplateID AND Result.StudentNumber = ? AND ResultTemplate.ClassID = ?");
            preparedStatement.setString(1, studentNumber);
            preparedStatement.setInt(2, classID);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                results.add(new Result(rs.getString("ResultName"), rs.getDouble("Result"), rs.getDouble("ResultMax"), rs.getDouble("DPWeight"), rs.getDouble("FinalWeight")));
            }
            return results;
        } catch (SQLException ex) {
            System.out.println("Server> getResults> " + ex);
            return null;
        }
    }

    public StudentClass getStudentClass(int classID) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class, Lecturer, Registered WHERE Registered.ClassID = Class.ClassID AND Class.LecturerID = Lecturer.LecturerID AND Class.ClassID = ?");
            preparedStatement.setInt(1, classID);
            ResultSet rs = preparedStatement.executeQuery();
            StudentClass studentClass = null;
            if (rs.next()) {
                List<ClassTime> classTimes = getClassTimes(classID);
                List<ClassFile> files = getFiles(classID);
                studentClass = new StudentClass(rs.getString("ModuleName"), rs.getString("ModuleNumber"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("LecturerID"), rs.getString("Email"), classTimes, files);
            }
            return studentClass;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public List<ClassTime> getClassTimes(int classID) {
        List<ClassTime> classTimes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM ClassTime WHERE ClassID = ?");
            preparedStatement.setString(1, Integer.toString(classID));
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                classTimes.add(new ClassTime(rs.getString("RoomNumber"), rs.getInt("DayOfWeek"), rs.getInt("StartSlot"), rs.getInt("EndSlot")));
            }
            return classTimes;
        } catch (SQLException ex) {
            System.out.println("Server> getClassTimes> " + ex);
            return null;
        }
    }

    public String getStudentPassword(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password From Student WHERE StudentNumber = ?;");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("Password");
            } else {
                return null;
            }
        } catch (SQLException ex) {
            System.out.println("Server> getStudentPassword> " + ex);
            return null;
        }
    }

    public String getLecturerPassword(String lecturerNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password From Lecturer WHERE lecturerNumber = ?;");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("Password");
            } else {
                return null;
            }
        } catch (SQLException ex) {
            System.out.println("Server> getLecturerPassword> " + ex);
            return null;
        }
    }

    public Boolean changePasswordStudent(String studentNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Student SET Password = ? WHERE StudentNumber = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, studentNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println("Server> changePasswordStudent> " + ex);
            return false;
        }
    }

    public Boolean changePasswordLecturer(String lecturerNumber, String newPassword) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE Lecturer SET Password = ? WHERE LecturerNumber = ?;");
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, lecturerNumber);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println("Server> changePasswordLecturer> " + ex);
            return false;
        }
    }

    public Boolean emailPassword(String email) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT Password, StudentNumber From Student WHERE Email = ?;");
            preparedStatement.setString(1, email);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return mail.emailPassword(rs.getString("StudentNumber"), email, rs.getString("Password"));
            } else {
                return false;
            }
        } catch (SQLException ex) {
            System.out.println("Server> emailPassword> " + ex);
            return false;
        }
    }

    public ObservableList<NoticeBoard> getNoticeBoards() {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * From NoticeBoard;");
            ResultSet rs = preparedStatement.executeQuery();
            ObservableList<NoticeBoard> notices = FXCollections.observableArrayList();
            while (rs.next()) {
                NoticeBoard newNotice = new NoticeBoard(rs.getString("Heading"), rs.getString("Description"));
                notices.add(newNotice);
            }
            return notices;
        } catch (SQLException ex) {
            System.out.println("Server> getNoticesBoard> " + ex);
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
        return files;
    }

    public Lecturer getLecturer(String lecturerNumber){
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Lecturer WHERE LecturerNumber = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            List<LecturerClass> classes = getLecturerClasses(lecturerNumber);
            Lecturer student = new Lecturer(rs.getString("LecturerNumber"), rs.getString("Campus"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), classes);
            return student;
        } catch (SQLException ex) {
            System.out.println("Server> getLecturer> " + ex);
            return null;
        }
    }

    public List<LecturerClass> getLecturerClasses(String lecturerNumber){
        List<LecturerClass> classes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Class WHERE LecturerNumber = ?");
            preparedStatement.setString(1, lecturerNumber);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()){
                classes.add(new LecturerClass(rs.getString("ModuleName"), rs.getString("ModuleNumber"), getClassTimes(rs.getInt("ClassID")), getFiles(rs.getInt("ClassID"))));
            }
            return classes;
        } catch (SQLException ex) {
            System.out.println("Server> getLecturer> " + ex);
            return null;
        }
    }
}

