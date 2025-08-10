package com.businessLogic;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.topics.*;
import com.postgres.PostgresService;
import com.postgres.models.Payment;
/*
 * Handles the business logic for processing various topics and utilizes 
 * REST clients to communicate with other microservices.
 */
@Service
public class BusinessLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);
    public final PostgresService postgresService;

    // REST Clients to communicate with other microservices
    private RestClient apiGatewayClient = RestClient.create();

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

    public BusinessLogic(PostgresService postgresService) {
        this.postgresService = postgresService;
        mapTopicsToClient();
    }

    /* Method to map topics to their respective microservices and endpoints
        # api-gateway:8081
        # movie-service:8082
        # notification-service:8083
        # payment-service:8084
        # seating-service:8085
        # user-management-service:8086
        # gui-service:8087 
    */
    public void mapTopicsToClient() {
        restRouter.put("PaymentResponse", apiGatewayClient);
        restEndpoints.put(apiGatewayClient, "http://api-gateway:8081");
        LOG.info("Sucessfully mapped the topics to their respective microservices...");
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
        LOG.info("PaymentRequest processed with status: " + postgresSaveResponse.getId() != null ? "Success" : "Failure");
        return postgresSaveResponse.getId() != null ? ResponseEntity.ok("Entity was created/updated successully") :
                ResponseEntity.status(500).body("Inernal Error Failed to process PaymentRequest");
    }

}
