
package com.banking.model;

import java.math.BigDecimal;

public class Account {

    private int accountId;
    private String accountNumber;
    private int customerId;
    private BigDecimal balance;
    private String accountStatus;

    public Account(int accountId, String accountNumber,
                   int customerId, BigDecimal balance,
                   String accountStatus) {

        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = balance;
        this.accountStatus = accountStatus;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}

