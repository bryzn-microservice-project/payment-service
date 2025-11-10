package com.postgres.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "payment_service")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_amount", nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "credit_card", nullable = false)
    private String creditCard;

    @Column(name = "cvc", nullable = false)
    private String cvc;

    @Column(name = "initial_time_stamp", nullable = false)
    private String initialTimeStamp;

    // for JPA only, no use
    public Payment() {}

    public Payment(BigDecimal paymentAmount, String email, String creditCard, String cvc) {
        this.paymentAmount = paymentAmount;
        this.email = email;
        this.creditCard = creditCard;
        this.cvc = cvc;
        this.initialTimeStamp = LocalDateTime.now().toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(String creditCard) {
        this.creditCard = creditCard;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    public String getInitialTimeStamp() {
        return initialTimeStamp;
    }

    public void setInitialTimeStamp(String timeStamp) {
        this.initialTimeStamp = timeStamp;
    }
}
