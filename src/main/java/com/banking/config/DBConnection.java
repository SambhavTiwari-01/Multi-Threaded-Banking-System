
package com.banking.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/banking_txn_db";

    private static final String USER = "root";

    // Replace with your actual MySQL password
    private static final String PASSWORD = "Sambhav@2390";

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL, USER, PASSWORD
        );
    }
}
