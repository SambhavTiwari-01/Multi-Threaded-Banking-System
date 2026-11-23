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














-- SEED DATA FOR TESTING PURPOSE

use banking_txn_db;

-- Customers
insert ignore into customer (customer_code, full_name, email, phone, status) values
('CUST1001', 'Sambhav tiwari', 'Sambhav.tiwari@example1.com', '+91-9811100001', 'ACTIVE'),
('CUST1002', 'Ayush dwivedi', 'Ayush dwivedi@example2.com', '+91-9811100002', 'ACTIVE'),
('CUST1003', 'Vaibhav mishra', 'Vaibhav.mishra@example3.com', '+91-9811100003', 'ACTIVE'),
('CUST1004', 'Vaibhav dwivedi', 'Vaibhav.dwivedi@example4.com', '+91-9811100004', 'INACTIVE');

-- Accounts (ACC1002 starts at 10000.00 for testing concurrent 7000 and 5000 withdrawals)
insert ignore into account (account_number, customer_id, account_type, currency, balance, status, version)
select 'ACC1001', customer_id, 'SAVINGS', 'INR', 50000.00, 'ACTIVE', 0 from customer where customer_code = 'CUST1001'
union all
select 'ACC1002', customer_id, 'CURRENT', 'INR', 10000.00, 'ACTIVE', 0 from customer where customer_code = 'CUST1001'
union all
select 'ACC2004', customer_id, 'SAVINGS', 'INR', 25000.00, 'ACTIVE', 0 from customer where customer_code = 'CUST1002'
union all
select 'ACC3001', customer_id, 'SAVINGS', 'INR', 15000.00, 'ACTIVE', 0 from customer where customer_code = 'CUST1003'
union all
select 'ACC4001', customer_id, 'SAVINGS', 'INR', 8000.00, 'INACTIVE', 0 from customer where customer_code = 'CUST1004';

-- Sample transactions 
insert ignore into transaction_record (
    transaction_id, txn_type, source_account_id, destination_account_id,
    amount, status, failure_reason, channel, created_at, completed_at
)
select
    'TRXN_SEED_001', 'DEPOSIT', null, a_dst.account_id,
    20000.00, 'SUCCESS', null, 'BRANCH',
    date_sub(now(6), interval 2 day), date_sub(now(6), interval 2 day)
from account a_dst where a_dst.account_number = 'ACC1001'
union all
select
    'TRXN_SEED_002', 'TRANSFER', a_src.account_id, a_dst.account_id,
    12500.00, 'SUCCESS', null, 'MOBILE',
    date_sub(now(6), interval 1 day), date_sub(now(6), interval 1 day)
from account a_src, account a_dst
where a_src.account_number = 'ACC1001' and a_dst.account_number = 'ACC2004'
union all
select
    'TRXN_SEED_003', 'WITHDRAWAL', a_src.account_id, null,
    4000.00, 'SUCCESS', null, 'ATM',
    date_sub(now(6), interval 6 hour), date_sub(now(6), interval 6 hour)
from account a_src where a_src.account_number = 'ACC3001'
union all
select
    'TRXN_SEED_004', 'WITHDRAWAL', a_src.account_id, null,
    50000.00, 'FAILED', 'Insufficient balance for withdrawal', 'ATM',
    date_sub(now(6), interval 4 hour), date_sub(now(6), interval 4 hour)
from account a_src where a_src.account_number = 'ACC3001'
union all
select
    'TRXN_SEED_005', 'TRANSFER', a_src.account_id, a_dst.account_id,
    1000.00, 'REJECTED', 'Source account is INACTIVE', 'INTERNET',
    date_sub(now(6), interval 2 hour), date_sub(now(6), interval 2 hour)
from account a_src, account a_dst
where a_src.account_number = 'ACC4001' and a_dst.account_number = 'ACC2004'
union all
select
    'TRXN_SEED_006', 'DEPOSIT', null, a_dst.account_id,
    5000.00, 'SUCCESS', null, 'MOBILE',
    date_sub(now(6), interval 1 hour), date_sub(now(6), interval 1 hour)
from account a_dst where a_dst.account_number = 'ACC2004';

-- Audit rows linked to the seed transactions above
insert into transaction_audit (
    txn_pk_id, transaction_id, account_id, step_name, event_status,
    balance_before, balance_after, detail_message, thread_name, created_at
)
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'CREDIT_POSTED', 'SUCCEEDED',
       30000.00, 50000.00, 'Seed deposit added', 'seed-thread-1', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC1001'
where tr.transaction_id = 'TRXN_SEED_001'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_001' and ta.step_name = 'CREDIT_POSTED')
union all
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'DEBIT_POSTED', 'SUCCEEDED',
       62500.00, 50000.00, 'Seed transfer debit', 'seed-thread-1', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC1001'
where tr.transaction_id = 'TRXN_SEED_002'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_002' and ta.step_name = 'DEBIT_POSTED')
union all
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'CREDIT_POSTED', 'SUCCEEDED',
       12500.00, 25000.00, 'Seed transfer credit', 'seed-thread-1', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC2004'
where tr.transaction_id = 'TRXN_SEED_002'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_002' and ta.step_name = 'CREDIT_POSTED')
union all
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'DEBIT_POSTED', 'SUCCEEDED',
       19000.00, 15000.00, 'Seed ATM withdrawal', 'seed-thread-2', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC3001'
where tr.transaction_id = 'TRXN_SEED_003'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_003' and ta.step_name = 'DEBIT_POSTED')
union all
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'BALANCE_CHECK', 'FAILED',
       15000.00, 15000.00, 'Insufficient funds', 'seed-thread-2', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC3001'
where tr.transaction_id = 'TRXN_SEED_004'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_004' and ta.step_name = 'BALANCE_CHECK')
union all
select tr.txn_pk_id, tr.transaction_id, a.account_id, 'STATUS_CHECK', 'REJECTED',
       8000.00, 8000.00, 'Account status is INACTIVE', 'seed-thread-3', tr.created_at
from transaction_record tr join account a on a.account_number = 'ACC4001'
where tr.transaction_id = 'TRXN_SEED_005'
  and not exists (select 1 from transaction_audit ta where ta.transaction_id = 'TRXN_SEED_005' and ta.step_name = 'STATUS_CHECK');

-- Failed transaction records for TXN_SEED_004 and TXN_SEED_005
insert into failed_transaction (
    txn_pk_id, transaction_id, source_account_id, destination_account_id,
    txn_type, amount, error_category, error_code, reason, thread_name, occurred_at
)
select
    tr.txn_pk_id, tr.transaction_id, tr.source_account_id, tr.destination_account_id,
    tr.txn_type, tr.amount, 'INSUFFICIENT_BALANCE', 'ERR_LOW_BALANCE',
    tr.failure_reason, 'seed-thread-2', tr.created_at
from transaction_record tr
where tr.transaction_id = 'TRXN_SEED_004'
  and not exists (select 1 from failed_transaction ft where ft.transaction_id = 'TRXN_SEED_004')
union all
select
    tr.txn_pk_id, tr.transaction_id, tr.source_account_id, tr.destination_account_id,
    tr.txn_type, tr.amount, 'INVALID_ACCOUNT_STATE', 'ERR_INACTIVE_ACCOUNT',
    tr.failure_reason, 'seed-thread-3', tr.created_at
from transaction_record tr
where tr.transaction_id = 'TRXN_SEED_005'
  and not exists (select 1 from failed_transaction ft where ft.transaction_id = 'TRXN_SEED_005');

