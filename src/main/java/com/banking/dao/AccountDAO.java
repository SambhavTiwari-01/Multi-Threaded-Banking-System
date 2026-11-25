
package com.banking.dao;

import com.banking.config.DBConnection;
import com.banking.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDAO {

    public Account findByAccountNumber(String accountNumber)
            throws SQLException {

        String sql = """
                SELECT account_id, account_number, customer_id,
                       balance, status
                FROM account
                WHERE account_number = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return new Account(
                            rs.getInt("account_id"),
                            rs.getString("account_number"),
                            rs.getInt("customer_id"),
                            rs.getBigDecimal("balance"),
                            rs.getString("status")
                    );
                }
            }
        }

        return null;
    }
}

