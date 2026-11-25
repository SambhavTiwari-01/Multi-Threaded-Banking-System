
package com.banking.service;

import com.banking.config.DBConnection;
import com.banking.exception.BankingException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionService {

    public void deposit(String transactionId,
                        String accountNumber,
                        BigDecimal amount)
            throws SQLException, BankingException {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException(
                    "Deposit amount must be greater than zero."
            );
        }

        String updateSql = """
                UPDATE account
                SET balance = balance + ?
                WHERE account_number = ?
                  AND status = 'ACTIVE'
                """;

        String insertSql = """
                INSERT INTO transaction_record
                (transaction_id,  destination_account_id, amount,
                 txn_type,status)
                VALUES (?, ?, ?, 'DEPOSIT', 'SUCCESS')
                """;

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                // Lock and validate the account.
                String checkSql = """
                        SELECT account_id
                        FROM account
                        WHERE account_number = ?
                          AND status = 'ACTIVE'
                        FOR UPDATE
                        """;

                int accountId;

                try (PreparedStatement ps =
                             con.prepareStatement(checkSql)) {

                    ps.setString(1, accountNumber);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new BankingException(
                                    "Account not found or inactive."
                            );
                        }

                        accountId = rs.getInt("account_id");
                    }
                }

                // Update account balance.
                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {

                    ps.setBigDecimal(1, amount);
                    ps.setString(2, accountNumber);

                    if (ps.executeUpdate() != 1) {
                        throw new BankingException(
                                "Unable to update account balance."
                        );
                    }
                }

                // Record successful transaction.
                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {

                    ps.setString(1, transactionId);
                    ps.setInt(2, accountId);
                    ps.setBigDecimal(3, amount);

                    ps.executeUpdate();
                }

                con.commit();

                System.out.println("Deposit Successful!");
                System.out.println("Amount: ₹" + amount);

            } catch (Exception e) {

                con.rollback();

                if (e instanceof BankingException) {
                    throw (BankingException) e;
                }

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                throw new SQLException(
                        "Deposit failed.", e
                );

            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}

