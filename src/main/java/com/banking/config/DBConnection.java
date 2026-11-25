
package com.banking.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    public static Connection getConnection() throws SQLException {

        String url = dotenv.get("DB_URL");
        String username = dotenv.get("DB_USERNAME");
        String password = dotenv.get("DB_PASSWORD");

        // Fall back to system environment variables if needed
        if (url == null) {
            url = System.getenv("DB_URL");
        }

        if (username == null) {
            username = System.getenv("DB_USERNAME");
        }

        if (password == null) {
            password = System.getenv("DB_PASSWORD");
        }

        if (url == null || username == null || password == null) {
            throw new SQLException(
                    "Database configuration missing. Check your .env file."
            );
        }

        return DriverManager.getConnection(url, username, password);
    }
}
