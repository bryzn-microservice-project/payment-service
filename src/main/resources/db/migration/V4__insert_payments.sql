INSERT INTO payment_service.payments(email, payment_amount, cash_amount, reward_cash_applied, credit_card, cvc, initial_time_stamp) VALUES
('alice@example.com', '12.34', '12.34', '0.0', '4532015112830366', '123', '2025-11-10T19:30:00-06:00'),
('bob.smith@example.com', '14.25', '12.00', '2.25', '5500005555555559', '456', '2025-11-11T12:30:00-06:00'),
('charlie.d@example.com', '122.21', '100.00', '22.21', '340000000000009', '7890', '2025-11-12T15:30:00-06:00'),
('bryzntest@gmail.com', '22.22', '0.00', '22.22', '6011000990139424', '321', '2025-11-12T15:35:00-06:00')
ON CONFLICT DO NOTHING;