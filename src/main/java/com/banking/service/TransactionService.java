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
    //  3.FUND TRANSFER METHOD
   // ==========================================

    public void transfer(String transactionId,
                         String sourceAccountNumber,
                         String destinationAccountNumber,
                         BigDecimal amount) throws BankingException {

        // Validate transaction ID and amount
        if (transactionId == null || transactionId.isBlank()) {
            throw new BankingException("Transaction ID cannot be empty");
        }

        if (sourceAccountNumber == null || sourceAccountNumber.isBlank()
                || destinationAccountNumber == null
                || destinationAccountNumber.isBlank()) {
            throw new BankingException("Account numbers cannot be empty");
        }

        if (sourceAccountNumber.equalsIgnoreCase(destinationAccountNumber)) {
            throw new BankingException(
                    "Source and destination accounts cannot be the same");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Amount must be greater than zero");
        }

        // Lock both accounts in ascending account_id order
        String lockSql = """
            SELECT account_id, account_number, balance, status, currency
            FROM account
            WHERE account_number IN (?, ?)
            ORDER BY account_id
            FOR UPDATE
            """;

        String debitSql = """
            UPDATE account
            SET balance = balance - ?,
                version = version + 1
            WHERE account_id = ?
            AND balance >= ?
            """;

        String creditSql = """
            UPDATE account
            SET balance = balance + ?,
                version = version + 1
            WHERE account_id = ?
            """;

        String insertSql = """
            INSERT INTO transaction_record
            (transaction_id, txn_type, source_account_id,
             destination_account_id, amount, status, completed_at)
            VALUES (?, 'TRANSFER', ?, ?, ?, 'SUCCESS', CURRENT_TIMESTAMP(6))
            """;

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Check duplicate transaction ID
            checkDuplicateTransaction(con, transactionId);

            long sourceAccountId = -1;
            long destinationAccountId = -1;

            BigDecimal sourceBalance = null;
            String sourceCurrency = null;
            String destinationCurrency = null;

            boolean sourceActive = false;
            boolean destinationActive = false;

            // Lock and retrieve both accounts
            try (PreparedStatement ps = con.prepareStatement(lockSql)) {

                ps.setString(1, sourceAccountNumber);
                ps.setString(2, destinationAccountNumber);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        String accountNumber = rs.getString("account_number");

                        if (accountNumber.equals(sourceAccountNumber)) {
                            sourceAccountId = rs.getLong("account_id");
                            sourceBalance = rs.getBigDecimal("balance");
                            sourceCurrency = rs.getString("currency");
                            sourceActive = "ACTIVE".equals(rs.getString("status"));

                        } else if (accountNumber.equals(destinationAccountNumber)) {
                            destinationAccountId = rs.getLong("account_id");
                            destinationCurrency = rs.getString("currency");
                            destinationActive = "ACTIVE".equals(rs.getString("status"));
                        }
                    }
                }
            }

            // Check whether both accounts exist
            if (sourceAccountId == -1 || destinationAccountId == -1) {
                throw new BankingException("Source or destination account not found");
            }

            // Check account status
            if (!sourceActive || !destinationActive) {
                throw new BankingException("Both accounts must be ACTIVE");
            }

            // Check currency compatibility
            if (!sourceCurrency.equals(destinationCurrency)) {
                throw new BankingException(
                        "Transfers between different currencies are not supported");
            }

            // Check sufficient balance
            if (sourceBalance.compareTo(amount) < 0) {
                throw new BankingException("Insufficient account balance");
            }

            // Debit money from source account
            try (PreparedStatement ps = con.prepareStatement(debitSql)) {

                ps.setBigDecimal(1, amount);
                ps.setLong(2, sourceAccountId);
                ps.setBigDecimal(3, amount);

                if (ps.executeUpdate() != 1) {
                    throw new BankingException("Source account debit failed");
                }
            }

            // Credit money to destination account
            try (PreparedStatement ps = con.prepareStatement(creditSql)) {

                ps.setBigDecimal(1, amount);
                ps.setLong(2, destinationAccountId);

                if (ps.executeUpdate() != 1) {
                    throw new BankingException("Destination account credit failed");
                }
            }

            // Record the transfer
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setString(1, transactionId);
                ps.setLong(2, sourceAccountId);
                ps.setLong(3, destinationAccountId);
                ps.setBigDecimal(4, amount);

                ps.executeUpdate();
            }

            // Commit both balance updates together
            con.commit();

            System.out.println("Fund transfer successful!");
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("From Account: " + sourceAccountNumber);
            System.out.println("To Account: " + destinationAccountNumber);
            System.out.println("Amount: Rs. " + amount);

        } catch (BankingException e) {

            rollback(con);
            throw e;

        } catch (SQLException e) {

            rollback(con);
            throw new BankingException(
                    "Fund transfer failed: " + e.getMessage());

        } finally {
            closeConnection(con);
        }
    }
    // ==========================================
    // 4. COMMON INPUT VALIDATION
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
    // 5. CHECK DUPLICATE TRANSACTION ID
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
    // 6. ROLLBACK METHOD
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
    // 7. CLOSE DATABASE CONNECTION
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
