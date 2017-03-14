package net.ddns.swooosh.campusliveserver.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.ddns.swooosh.campusliveserver.models.ClassAndResult;
import net.ddns.swooosh.campusliveserver.models.Result;
import net.ddns.swooosh.campusliveserver.models.Student;
import net.ddns.swooosh.campusliveserver.models.StudentClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseHandler {

    private Connection con;

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
                        "LecturerID text);");
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
                        "Result intger);");
                stmt.execute("CREATE TABLE ResultTemplate (" +
                        "ResultTemplateID integer PRIMARY KEY AUTOINCREMENT, " +
                        "ClassID integer, " +
                        "ResultMax integer, " +
                        "Weight integer, " +
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

    public Boolean authoriseStudent(String username, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ? AND Password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println("Server> authoriseStudenttudent> " + ex);
        }
        return false;
    }

    public Boolean authoriseLecturer(String username, String password) {//TODO
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ? AND Password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println("Server> authoriseStudent> " + ex);
        }
        return false;
    }

    public Boolean authoriseAdmin(String username, String password) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM StudentNumbert WHERE StudentNumber = ? AND Password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            return preparedStatement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println("Server> authoriseStudent> " + ex);
        }
        return false;
    }

    public Student getStudent(String studentNumber) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM Student, Class, Result WHERE StudentNumber = ?");
            preparedStatement.setString(1, studentNumber);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {

                StudentClass studentClass = null;
                ObservableList<Result> result = FXCollections.observableArrayList();
                ObservableList<ClassAndResult> classAndResult = FXCollections.observableArrayList();


                Student student = new Student(rs.getString("StudentNumber"), rs.getString("Campus"), rs.getString("Qualification"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), classAndResult);//TODO class and results
                return student;
            } else {
                PreparedStatement preparedStatement2 = con.prepareStatement("SELECT * FROM Student WHERE StudentNumber = ?");
                preparedStatement2.setString(1, studentNumber);
                ResultSet rs2 = preparedStatement2.executeQuery();
                if (rs2.next()) {
                    Student student = new Student(rs.getString("StudentNumber"), rs.getString("Campus"), rs.getString("Qualification"), rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), null);
                    return student;
                }
            }
        } catch (SQLException ex) {
            System.out.println("Server> getStudent> " + ex);
        }
        return null;
    }



}

