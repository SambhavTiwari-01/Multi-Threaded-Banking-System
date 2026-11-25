
package com.banking.model;

import java.math.BigDecimal;

public class Transaction {

    private String transactionId;
    private String transactionType;
    private String accountNumber;
    private BigDecimal amount;

    public Transaction(String transactionId,
                       String transactionType,
                       String accountNumber,
                       BigDecimal amount) {

        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.accountNumber = accountNumber;
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
