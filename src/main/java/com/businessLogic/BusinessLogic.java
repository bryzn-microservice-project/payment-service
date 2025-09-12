package com.businessLogic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.topics.PaymentRequest;
import com.topics.PaymentResponse;
import com.topics.PaymentResponse.Status;
import com.postgres.PostgresService;
import com.postgres.models.Payment;
/*
 * Handles the business logic for processing various topics and utilizes 
 * REST clients to communicate with other microservices.
 */
@Service
public class BusinessLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);
    private final PostgresService postgresService;
    private final AsyncLogic asyncLogic;

    public BusinessLogic(PostgresService postgresService, AsyncLogic asyncLogic) {
        this.postgresService = postgresService;
        this.asyncLogic = asyncLogic;
    }

    /*
     * Request handlers for the various topics, which communicate through REST clients
     */
    public ResponseEntity<String> processPaymentRequest(PaymentRequest paymentRequest) {
        LOG.info("Received a PaymentRequest. Sending the topic to the [Payment Service]");

        // Payment(Double paymentAmount, String email, String creditCard, String cvc)
        Payment payment = new Payment(paymentRequest.getPaymentAmount(), paymentRequest.getEmail(),
                paymentRequest.getCreditCard(), paymentRequest.getCvc());
        
        Payment postgresSaveResponse = postgresService.save(payment);
        Status paymentStatus = postgresSaveResponse.getId() != null ? Status.SUCCESSFUL : Status.FAILED;
        LOG.info("PaymentRequest processed with status: " + paymentStatus);
        PaymentResponse paymentResponse = createPaymentResponse(paymentRequest, paymentStatus);

        // send async work before returning
        asyncLogic.handleRewards(paymentRequest);

        return postgresSaveResponse.getId() != null ? ResponseEntity.ok(paymentResponse.toString()) :
                ResponseEntity.status(500).body("Inernal Error Failed to process PaymentRequest");
    }

    private PaymentResponse createPaymentResponse(PaymentRequest paymentRequest, Status paymentStatus) {
        LOG.info("Creating a PaymentResponse... with status: " + paymentStatus);
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setTopicName("PaymentResponse");
        paymentResponse.setPaymentAmount(paymentRequest.getPaymentAmount());
        paymentResponse.setEmail(paymentRequest.getEmail());
        paymentResponse.setCreditCard(paymentRequest.getCreditCard()); 
        paymentResponse.setCorrelatorId(paymentRequest.getCorrelatorId());
        paymentResponse.setStatus(paymentStatus);
        return paymentResponse;
    }

}
