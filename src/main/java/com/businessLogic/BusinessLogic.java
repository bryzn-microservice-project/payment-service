package com.businessLogic;

import java.math.BigDecimal;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.topics.AccountInfoRequest;
import com.topics.AccountInfoResponse;
import com.topics.PaymentRequest;
import com.topics.PaymentResponse;
import com.topics.PaymentResponse.Status;
import com.topics.RewardsRequest;
import com.topics.RewardsResponse;
import com.topics.RewardsRequest.Application;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postgres.PostgresService;
import com.postgres.models.Payment;

/*
 * Handles the business logic for processing various topics and utilizes REST clients to communicate
 * with other microservices.
 */
@Service
public class BusinessLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);
    private final PostgresService postgresService;
    private final AsyncLogic asyncLogic;

    // REST Clients to communicate with other microservices
    private RestClient userServiceClient = RestClient.create();

    @Value("${user.management.service}")
    private String userManagementService;
    @Value("${user.management.service.port}")
    private String userManagementServicePort;
    private String ums;

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

    @PostConstruct
    public void init() {
        ums = "http://" + userManagementService + ":" + userManagementServicePort
                + "/api/v1/processTopic";
        restRouter.put("RewardsRequest", userServiceClient);
        restRouter.put("AccountInfoRequest", userServiceClient);
        restEndpoints.put(userServiceClient, ums);
        LOG.info("Business Logic initialized with User Management Service at: " + ums);
    }

    public BusinessLogic(PostgresService postgresService, AsyncLogic asyncLogic) {
        this.postgresService = postgresService;
        this.asyncLogic = asyncLogic;
    }

    /*
     * Request handlers for the various topics, which communicate through REST clients
     */
    @Transactional
    public ResponseEntity<String> processPaymentRequest(PaymentRequest paymentRequest) {
        System.out.println("\n");
        LOG.info("Received a PaymentRequest, posting record into the database...");

        // Grabbing reward points from the User Mangement Service and auto apply the discount
        AccountInfoRequest accountInfoRequest = new AccountInfoRequest();
        accountInfoRequest.setTopicName("AccountInfoRequest");
        accountInfoRequest.setEmail(paymentRequest.getEmail());
        accountInfoRequest.setCorrelatorId(paymentRequest.getCorrelatorId());

        String accountResponse = userServiceClient.post()
                .uri(restEndpoints.get(restRouter.get("AccountInfoRequest")))
                .contentType(MediaType.APPLICATION_JSON).body(accountInfoRequest).retrieve()
                .body(String.class);
        LOG.info("Attempting to find information about the account via email...");

        // Total reward points, the discounted price after application,
        // and max points in case user has more points than needed for full discount
        Double rewardPoints = 0.0;
        Double discountedAmount = 0.0;
        Double cappedPoints = 0.0;

        if (accountResponse == null) {
            LOG.warn("AccountInfoResponse is null, user may not exist.");
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                AccountInfoResponse account =
                        mapper.readValue(accountResponse, AccountInfoResponse.class);
                rewardPoints = Double.valueOf(account.getRewardPoints());
                LOG.info(account.getUsername() + " had " + rewardPoints
                        + " reward points. Automatically applying discount at rate of [RewardPoints/200].");

                discountedAmount = paymentRequest.getPaymentAmount();
                if (rewardPoints > 0) {
                    discountedAmount = discountedAmount - (rewardPoints / 200.0) < 0 ? 0
                            : discountedAmount - (rewardPoints / 200.0);

                    cappedPoints = Math.min(rewardPoints, paymentRequest.getPaymentAmount() * 200);
                    String formattedPoints = String.format("$%.2f", cappedPoints / 200.0);
                    String formattedDiscount = String.format("$%.2f", discountedAmount);
                    LOG.info("Applied discount of " + cappedPoints + " points equating to " + formattedPoints + ", new payment amount is: "
                            + formattedDiscount);
                }

                // Update the reward points for the user, depleting the used points
                RewardsRequest rewardsRequest = new RewardsRequest();
                rewardsRequest.setTopicName("RewardsRequest");
                rewardsRequest.setCorrelatorId(paymentRequest.getCorrelatorId());
                rewardsRequest.setEmail(paymentRequest.getEmail());
                rewardsRequest.setName(account.getName());
                rewardsRequest.setUsername(account.getUsername());
                rewardsRequest.setRewardPoints(rewardPoints.intValue() - cappedPoints.intValue());
                rewardsRequest.setApplication(Application.REWARD_POINTS_REDEEMED);

                String rewardResponse = userServiceClient.post()
                    .uri(restEndpoints.get(restRouter.get("RewardsRequest")))
                    .contentType(MediaType.APPLICATION_JSON).body(rewardsRequest).retrieve()
                    .body(String.class);

                RewardsResponse response =
                        mapper.readValue(rewardResponse, RewardsResponse.class);

                LOG.info("Sent a RewardsRequest to deplete used reward points. Status: " + response.getApplication());
                
            } catch (Exception e) {
                LOG.error("Error parsing AccountInfoResponse: " + e.getMessage());
            }
        }

        // Payment(Double paymentAmount, String email, String creditCard, String cvc)
        Payment payment = new Payment(BigDecimal.valueOf(paymentRequest.getPaymentAmount()),
                BigDecimal.valueOf(discountedAmount), BigDecimal.valueOf(cappedPoints/200.0),
                paymentRequest.getEmail(), paymentRequest.getCreditCard(), paymentRequest.getCvc());

        Payment postgresSaveResponse = postgresService.save(payment);
        Status paymentStatus =
                postgresSaveResponse.getId() != null ? Status.SUCCESSFUL : Status.FAILED;
        LOG.info("PaymentRequest processed with status: " + paymentStatus + " ID: "
                + postgresSaveResponse.getId());
        PaymentResponse paymentResponse = createPaymentResponse(paymentRequest, paymentStatus);

        // send async work before returning
        asyncLogic.handleRewards(paymentRequest, discountedAmount);

        return postgresSaveResponse.getId() != null ? ResponseEntity.ok(paymentResponse.toString())
                : ResponseEntity.status(500).body("Inernal Error Failed to process PaymentRequest");
    }

    private PaymentResponse createPaymentResponse(PaymentRequest paymentRequest,
            Status paymentStatus) {
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
