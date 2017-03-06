package net.ddns.swooosh.campusliveserver.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {

    private Connection con;

    public Boolean connect() {
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


}

