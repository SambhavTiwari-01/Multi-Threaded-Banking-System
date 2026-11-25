-- BANKING SYSTEM SQL REPORTS FOR EVALUATION
use banking_txn_db;

-- 1. Top 10 accounts by transaction volume 
select
    a.account_number,
    a.account_type,
    count(t.txn_pk_id) as total_txns,
    coalesce(sum(t.amount), 0) as total_volume
from account a
left join transaction_record t
    on a.account_id = t.source_account_id or a.account_id = t.destination_account_id
group by a.account_id, a.account_number, a.account_type
order by total_volume desc
limit 10;

-- 2. Failed transactions grouped by reason
select
    error_category,
    reason,
    count(*) as failed_count,
    coalesce(sum(amount), 0) as total_failed_amount
from failed_transaction
group by error_category, reason
order by failed_count desc;

-- 3. Daily transaction amount by transaction type
select
    date(created_at) as txn_date,
    txn_type,
    count(*) as txn_count,
    sum(amount) as total_amount
from transaction_record
group by date(created_at), txn_type
order by txn_date desc, txn_type;

-- 4. Accounts with more than N transactions 
select
    a.account_number,
    c.full_name,
    count(t.txn_pk_id) as txn_count
from account a
join customer c on a.customer_id = c.customer_id
left join transaction_record t
    on a.account_id = t.source_account_id or a.account_id = t.destination_account_id
group by a.account_id, a.account_number, c.full_name
having count(t.txn_pk_id) > 2
order by txn_count desc;

-- 5. Complete transaction history for a customer
select
    c.customer_code,
    c.full_name,
    a.account_number,
    t.transaction_id,
    t.txn_type,
    t.amount,
    t.status,
    t.failure_reason,
    t.channel,
    t.created_at
from customer c
join account a on c.customer_id = a.customer_id
join transaction_record t
    on a.account_id = t.source_account_id or a.account_id = t.destination_account_id
where c.customer_code = 'CUST1003'
order by t.created_at desc;