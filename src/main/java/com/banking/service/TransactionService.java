//
//package com.banking.service;
//
//import com.banking.config.DBConnection;
//import com.banking.exception.BankingException;
//
//import java.math.BigDecimal;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//public class TransactionService {
//
//    public void deposit(String transactionId,
//                        String accountNumber,
//                        BigDecimal amount)
//            throws SQLException, BankingException {
//
//        if (amount == null ||
//                amount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new BankingException(
//                    "Deposit amount must be greater than zero."
//            );
//        }
//
//        String updateSql = """
//                UPDATE account
//                SET balance = balance + ?
//                WHERE account_number = ?
//                  AND status = 'ACTIVE'
//                """;
//
//        String insertSql = """
//                INSERT INTO transaction_record
//                (transaction_id,  destination_account_id, amount,
//                 txn_type,status)
//                VALUES (?, ?, ?, 'DEPOSIT', 'SUCCESS')
//                """;
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            con.setAutoCommit(false);
//
//            try {
//                // Lock and validate the account.
//                String checkSql = """
//                        SELECT account_id
//                        FROM account
//                        WHERE account_number = ?
//                          AND status = 'ACTIVE'
//                        FOR UPDATE
//                        """;
//
//                int accountId;
//
//                try (PreparedStatement ps =
//                             con.prepareStatement(checkSql)) {
//
//                    ps.setString(1, accountNumber);
//
//                    try (ResultSet rs = ps.executeQuery()) {
//
//                        if (!rs.next()) {
//                            throw new BankingException(
//                                    "Account not found or inactive."
//                            );
//                        }
//
//                        accountId = rs.getInt("account_id");
//                    }
//                }
//
//                // Update account balance.
//                try (PreparedStatement ps =
//                             con.prepareStatement(updateSql)) {
//
//                    ps.setBigDecimal(1, amount);
//                    ps.setString(2, accountNumber);
//
//                    if (ps.executeUpdate() != 1) {
//                        throw new BankingException(
//                                "Unable to update account balance."
//                        );
//                    }
//                }
//
//                // Record successful transaction.
//                try (PreparedStatement ps =
//                             con.prepareStatement(insertSql)) {
//
//                    ps.setString(1, transactionId);
//                    ps.setInt(2, accountId);
//                    ps.setBigDecimal(3, amount);
//
//                    ps.executeUpdate();
//                }
//
//                con.commit();
//
//                System.out.println("Deposit Successful!");
//                System.out.println("Amount: ₹" + amount);
//
//            } catch (Exception e) {
//
//                con.rollback();
//
//                if (e instanceof BankingException) {
//                    throw (BankingException) e;
//                }
//
//                if (e instanceof SQLException) {
//                    throw (SQLException) e;
//                }
//
//                throw new SQLException(
//                        "Deposit failed.", e
//                );
//
//            } finally {
//                con.setAutoCommit(true);
//            }
//        }
//    }
//}










package com.banking.service;

import com.banking.config.DBConnection;
import com.banking.exception.BankingException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionService {

    // ==========================================
    // 1. DEPOSIT METHOD
    // ==========================================

    public void deposit(String transactionId,
                        String accountNumber,
                        BigDecimal amount) throws BankingException {

        // Validate input
        validateInput(transactionId, accountNumber, amount);

        String lockSql = """
                SELECT account_id
                FROM account
                WHERE account_number = ?
                AND status = 'ACTIVE'
                FOR UPDATE
                """;

        String updateSql = """
                UPDATE account
                SET balance = balance + ?,
                    version = version + 1
                WHERE account_id = ?
                """;

        String insertSql = """
                INSERT INTO transaction_record
                (transaction_id, txn_type, source_account_id,
                 destination_account_id, amount, status, completed_at)
                VALUES (?, 'DEPOSIT', NULL, ?, ?, 'SUCCESS', CURRENT_TIMESTAMP(6))
                """;

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Check duplicate transaction ID
            checkDuplicateTransaction(con, transactionId);

            // Lock account
            long accountId;

            try (PreparedStatement ps = con.prepareStatement(lockSql)) {

                ps.setString(1, accountNumber);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new BankingException(
                                "Account not found or account is inactive");
                    }

                    accountId = rs.getLong("account_id");
                }
            }

            // Update balance
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                ps.setBigDecimal(1, amount);
                ps.setLong(2, accountId);

                int rows = ps.executeUpdate();

                if (rows != 1) {
                    throw new BankingException("Deposit balance update failed");
                }
            }

            // Record transaction
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setString(1, transactionId);
                ps.setLong(2, accountId);
                ps.setBigDecimal(3, amount);

                ps.executeUpdate();
            }

            // Commit transaction
            con.commit();

            System.out.println("Deposit successful!");
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("Account: " + accountNumber);
            System.out.println("Amount: Rs. " + amount);

        } catch (BankingException e) {

            rollback(con);
            throw e;

        } catch (SQLException e) {

            rollback(con);

            throw new BankingException(
                    "Deposit failed: " + e.getMessage());

        } finally {
            closeConnection(con);
        }
    }


    // ==========================================
    // 2. WITHDRAWAL METHOD
    // ==========================================

    public void withdraw(String transactionId,
                         String accountNumber,
                         BigDecimal amount) throws BankingException {

        // Validate input
        validateInput(transactionId, accountNumber, amount);

        String lockSql = """
                SELECT account_id, balance
                FROM account
                WHERE account_number = ?
                AND status = 'ACTIVE'
                FOR UPDATE
                """;

        String updateSql = """
                UPDATE account
                SET balance = balance - ?,
                    version = version + 1
                WHERE account_id = ?
                """;

        String insertSql = """
                INSERT INTO transaction_record
                (transaction_id, txn_type, source_account_id,
                 destination_account_id, amount, status, completed_at)
                VALUES (?, 'WITHDRAWAL', ?, NULL, ?, 'SUCCESS', CURRENT_TIMESTAMP(6))
                """;

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Check duplicate transaction ID
            checkDuplicateTransaction(con, transactionId);

            // Lock account and retrieve balance
            long accountId;
            BigDecimal currentBalance;

            try (PreparedStatement ps = con.prepareStatement(lockSql)) {

                ps.setString(1, accountNumber);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new BankingException(
                                "Account not found or account is inactive");
                    }

                    accountId = rs.getLong("account_id");
                    currentBalance = rs.getBigDecimal("balance");
                }
            }

            // Check sufficient balance
            if (currentBalance.compareTo(amount) < 0) {

                throw new BankingException(
                        "Insufficient account balance");
            }

            // Deduct amount
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                ps.setBigDecimal(1, amount);
                ps.setLong(2, accountId);

                int rows = ps.executeUpdate();

                if (rows != 1) {
                    throw new BankingException("Withdrawal failed");
                }
            }

            // Record transaction
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setString(1, transactionId);
                ps.setLong(2, accountId);
                ps.setBigDecimal(3, amount);

                ps.executeUpdate();
            }

            // Commit transaction
            con.commit();

            System.out.println("Withdrawal successful!");
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("Account: " + accountNumber);
            System.out.println("Amount: Rs. " + amount);

        } catch (BankingException e) {

            rollback(con);
            throw e;

        } catch (SQLException e) {

            rollback(con);

            throw new BankingException(
                    "Withdrawal failed: " + e.getMessage());

        } finally {
            closeConnection(con);
        }
    }


    // ==========================================
    // 3. COMMON INPUT VALIDATION
    // ==========================================

    private void validateInput(String transactionId,
                               String accountNumber,
                               BigDecimal amount) throws BankingException {

        if (transactionId == null || transactionId.isBlank()) {
            throw new BankingException(
                    "Transaction ID cannot be empty");
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BankingException(
                    "Account number cannot be empty");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException(
                    "Amount must be greater than zero");
        }
    }


    // ==========================================
    // 4. CHECK DUPLICATE TRANSACTION ID
    // ==========================================

    private void checkDuplicateTransaction(Connection con,
                                           String transactionId)
            throws SQLException, BankingException {

        String sql = """
                SELECT transaction_id
                FROM transaction_record
                WHERE transaction_id = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, transactionId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    throw new BankingException(
                            "Duplicate transaction ID: " + transactionId);
                }
            }
        }
    }


    // ==========================================
    // 5. ROLLBACK METHOD
    // ==========================================

    private void rollback(Connection con) {

        if (con != null) {

            try {
                con.rollback();

            } catch (SQLException e) {

                System.out.println(
                        "Rollback failed: " + e.getMessage());
            }
        }
    }


    // ==========================================
    // 6. CLOSE DATABASE CONNECTION
    // ==========================================

    private void closeConnection(Connection con) {

        if (con != null) {

            try {
                con.close();

            } catch (SQLException e) {

                System.out.println(
                        "Connection closing failed: " + e.getMessage());
            }
        }
    }
}
