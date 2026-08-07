USE bank_account;

ALTER TABLE t_account
    ADD COLUMN source_event_id CHAR(36) NULL AFTER account_no,
    ADD UNIQUE KEY uk_account_source_event_id (source_event_id);
