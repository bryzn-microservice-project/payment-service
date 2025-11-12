CREATE TABLE IF NOT EXISTS payment_service.payments (
    id SERIAL PRIMARY KEY,
    email VARCHAR(50) NOT NULL,
    payment_amount DECIMAL(5,2) CHECK (payment_amount >= 0),
    cash_amount DECIMAL(5,2) CHECK (cash_amount >= 0),
    reward_cash_applied DECIMAL(5,2) CHECK (reward_cash_applied >= 0),
    credit_card VARCHAR(19) CHECK (credit_card ~ '^[0-9]{13,19}$'),
    cvc VARCHAR(4) CHECK (cvc ~ '^[0-9]{3,4}$'),
    initial_time_stamp TIMESTAMP WITH TIME ZONE NOT NULL
);