-- Banking transaction system
-- Tables:

create database if not exists banking_txn_db;
use banking_txn_db;



-- Stores customer details
create table customer (
                          customer_id bigint auto_increment primary key,
                          customer_code varchar(32) not null,
                          full_name varchar(120) not null,
                          email varchar(150) not null,
                          phone varchar(20),
                          status varchar(20) not null default 'ACTIVE',
                          created_at timestamp(6) not null default current_timestamp(6),
                          updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
                          constraint uq_customer_code unique (customer_code),
                          constraint uq_customer_email unique (email),
                          constraint chk_customer_status check (status in ('ACTIVE', 'INACTIVE', 'BLOCKED'))
) engine=InnoDB;


create table account (
                         account_id bigint auto_increment primary key,
                         account_number varchar(32) not null,
                         customer_id bigint not null,
                         account_type varchar(20) not null default 'SAVINGS',
                         currency char(3) not null default 'INR',
                         balance decimal(15, 2) not null default 0.00,
                         status varchar(20) not null default 'ACTIVE',
                         version bigint not null default 0,
                         created_at timestamp(6) not null default current_timestamp(6),
                         updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
                         constraint uq_account_number unique (account_number),
                         constraint fk_account_customer foreign key (customer_id) references customer(customer_id),
                         constraint chk_account_type check (account_type in ('SAVINGS', 'CURRENT')),
                         constraint chk_account_status check (status in ('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED')),
                         constraint chk_account_balance check (balance >= 0.00)
) engine=InnoDB;

-- Stores every transaction request
create table transaction_record (
                                    txn_pk_id bigint auto_increment primary key,
                                    transaction_id varchar(64) not null,
                                    txn_type varchar(20) not null,
                                    source_account_id bigint null,
                                    destination_account_id bigint null,
                                    amount decimal(15, 2) not null,
                                    status varchar(20) not null,
                                    failure_reason varchar(255) null,
                                    channel varchar(30) not null default 'API',
                                    created_at timestamp(6) not null default current_timestamp(6),
                                    completed_at timestamp(6) null,
                                    constraint uq_txn_business_id unique (transaction_id),
                                    constraint fk_txn_source_acc foreign key (source_account_id) references account(account_id),
                                    constraint fk_txn_dest_acc foreign key (destination_account_id) references account(account_id),
                                    constraint chk_txn_type check (txn_type in ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
                                    constraint chk_txn_status check (status in ('PENDING', 'SUCCESS', 'FAILED', 'REJECTED')),
                                    constraint chk_txn_amount check (amount > 0.00),
                                    constraint chk_txn_accounts check (
                                        (txn_type = 'DEPOSIT' and destination_account_id is not null)
                                            or (txn_type = 'WITHDRAWAL' and source_account_id is not null)
                                            or (
                                            txn_type = 'TRANSFER'
                                                and source_account_id is not null
                                                and destination_account_id is not null
                                                and source_account_id <> destination_account_id
                                            )
                                        )
) engine=InnoDB;

-- Tracks Transaction
create table transaction_audit (
                                   audit_id bigint auto_increment primary key,
                                   txn_pk_id bigint null,
                                   transaction_id varchar(64) not null,
                                   account_id bigint null,
                                   step_name varchar(60) not null,
                                   event_status varchar(20) not null,
                                   balance_before decimal(15, 2) null,
                                   balance_after decimal(15, 2) null,
                                   detail_message varchar(500) null,
                                   thread_name varchar(100) null,
                                   created_at timestamp(6) not null default current_timestamp(6),
                                   constraint fk_audit_txn foreign key (txn_pk_id) references transaction_record(txn_pk_id),
                                   constraint fk_audit_acc foreign key (account_id) references account(account_id),
                                   constraint chk_audit_status check (event_status in ('STARTED', 'SUCCEEDED', 'FAILED', 'REJECTED', 'DUPLICATE_IGNORED'))
) engine=InnoDB;

-- Stores failed or rejected transactions
create table failed_transaction (
                                    failed_id bigint auto_increment primary key,
                                    txn_pk_id bigint null,
                                    transaction_id varchar(64) not null,
                                    source_account_id bigint null,
                                    destination_account_id bigint null,
                                    txn_type varchar(20) null,
                                    amount decimal(15, 2) null,
                                    error_category varchar(60) not null,
                                    error_code varchar(40) null,
                                    reason varchar(500) not null,
                                    thread_name varchar(100) null,
                                    occurred_at timestamp(6) not null default current_timestamp(6),
                                    constraint fk_failed_txn foreign key (txn_pk_id) references transaction_record(txn_pk_id),
                                    constraint fk_failed_src_acc foreign key (source_account_id) references account(account_id),
                                    constraint fk_failed_dst_acc foreign key (destination_account_id) references account(account_id)
) engine=InnoDB;

-- Indexes
create index idx_acc_customer on account(customer_id);
create index idx_txn_src_created on transaction_record(source_account_id, created_at);
create index idx_txn_dst_created on transaction_record(destination_account_id, created_at);
create index idx_txn_created_type_status on transaction_record(created_at, txn_type, status);
create index idx_audit_txn_id on transaction_audit(transaction_id, created_at);
create index idx_failed_err_cat on failed_transaction(error_category, occurred_at);















