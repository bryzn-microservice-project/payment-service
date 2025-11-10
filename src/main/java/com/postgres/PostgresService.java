package com.postgres;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.postgres.models.Payment;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class PostgresService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    // save includes creating and updating
    @Transactional
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    public void deleteById(Long id) {
        paymentRepository.deleteById(id);
    }

    public List<Payment> findByPaymentAmount(double amount) {
        return paymentRepository.findByPaymentAmount(amount);
    }

    public List<Payment> findByEmail(String email) {
        return paymentRepository.findByEmail(email);
    }
}
