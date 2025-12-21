-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sepay_transaction_id BIGINT UNIQUE NOT NULL,
    order_id BIGINT,
    gateway VARCHAR(255),
    transaction_date DATETIME,
    account_number VARCHAR(50),
    content VARCHAR(1000),
    transfer_amount DOUBLE,
    reference_code VARCHAR(255),
    description VARCHAR(2000),
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    INDEX idx_order_id (order_id),
    INDEX idx_sepay_transaction_id (sepay_transaction_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment
ALTER TABLE payments COMMENT = 'Bảng lưu trữ thông tin giao dịch thanh toán từ SePay';

