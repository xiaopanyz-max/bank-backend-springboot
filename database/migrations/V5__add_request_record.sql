USE bank_dev;

CREATE TABLE IF NOT EXISTS t_request_record (
    id BIGINT NOT NULL,
    global_serial_no VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    reference_no VARCHAR(64) NULL,
    fail_reason VARCHAR(500) NULL,
    create_time DATETIME(3) NULL,
    update_time DATETIME(3) NULL,
    creator BIGINT NULL,
    updater BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_record_global_serial_no (global_serial_no),
    KEY idx_request_record_status_time (status, create_time),
    KEY idx_request_record_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
