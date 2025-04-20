package com.ehms.program.util;

import java.sql.*;


public class DatabaseConnection {
     static final String URL = "jdbc:postgresql://localhost:5432/postgres";
     static final String USER = "postgres";
     static final String PASSWORD = "10112005";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}