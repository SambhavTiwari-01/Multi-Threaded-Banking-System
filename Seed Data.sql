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
