package com.ehms.program.util;

import java.sql.*;


public class DatabaseConnection {
     static final String URL = "jdbc:postgresql://localhost:5432/MYDATABASE";
     static final String USER = "MYUSER";
     static final String PASSWORD = "MYPASSWORD";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
