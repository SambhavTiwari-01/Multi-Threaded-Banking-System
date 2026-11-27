
//package com.banking;

//import com.banking.config.DBConnection;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        try {
//            Connection con = DBConnection.getConnection();
//
//            if (con != null) {
//                System.out.println(
//                        "MySQL Database Connected Successfully!"
//                );
//                System.out.println(
//                        "Database: " + con.getCatalog()
//                );
//            }
//
//            con.close();
//
//        } catch (SQLException e) {
//            System.out.println(
//                    "Database Connection Failed!"
//            );
//            e.printStackTrace();
//        }
//    }
//}















//package com.banking;
//
//import com.banking.config.DBConnection;
//import com.banking.dao.AccountDAO;
//import com.banking.model.Account;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        try {
//            AccountDAO accountDAO = new AccountDAO();
//
//            Account account =
//                    accountDAO.findByAccountNumber("ACC1001");
//
//            if (account != null) {
//
//                System.out.println("Account Found!");
//                System.out.println("Account Number: "
//                        + account.getAccountNumber());
//
//                System.out.println("Customer ID: "
//                        + account.getCustomerId());
//
//                System.out.println("Balance: ₹"
//                        + account.getBalance());
//
//                System.out.println("Status: "
//                        + account.getAccountStatus());
//
//            } else {
//                System.out.println("Account Not Found!");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}











//package com.banking;
//
//import com.banking.service.TransactionService;
//import java.math.BigDecimal;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        TransactionService service =
//                new TransactionService();
//
//        try {
//            service.deposit(
//                    "TXN1002",
//                    "ACC2004",
//                    new BigDecimal("5000.00")
//            );
//
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }
//}









//package com.banking;
//
//import com.banking.service.TransactionService;
//import com.banking.exception.BankingException;
//
//import java.math.BigDecimal;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        TransactionService service = new TransactionService();
//
//        try {
//
//            // Test withdrawal function
//            service.withdraw(
//                    "TXN10004",
//                    "ACC1001",
//                    new BigDecimal("7000.00")
//            );
//
//            // This part Test insufficient balance in an account
//            service.withdraw(
//                    "TXN10005",
//                    "ACC1001",
//                    new BigDecimal("100000.00")
//            );
//
//        } catch (BankingException e) {
//
//            System.out.println("Transaction failed: " + e.getMessage());
//        }
//    }
//}







package com.banking;

import com.banking.service.TransactionService;
import com.banking.exception.BankingException;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        TransactionService transactionService = new TransactionService();

        System.out.println("================================");
        System.out.println("       FUND TRANSFER TEST");
        System.out.println("================================");

        try {

            transactionService.transfer(
                    "TXN10009",
                    "ACC1001",
                    "ACC2004",
                    new BigDecimal("5000.00")
            );

        } catch (BankingException e) {

            System.out.println("Transfer failed: " + e.getMessage());
        }

        System.out.println("\nFund transfer testing completed.");
    }
}
