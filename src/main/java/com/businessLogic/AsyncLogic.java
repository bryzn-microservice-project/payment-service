package com.businessLogic;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.topics.AccountInfoRequest;
import com.topics.AccountInfoResponse;
import com.topics.PaymentRequest;
import com.topics.RewardsRequest;
import com.topics.RewardsRequest.Application;
import com.topics.RewardsResponse;

@Service
public class AsyncLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);

    // REST Clients to communicate with other microservices
    private RestClient userServiceClient = RestClient.create();
    private RestClient sessionManagerClient = RestClient.create();

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

    public AsyncLogic() {
        mapTopicsToClient();
    }

    /* Method to map topics to their respective microservices and endpoints
    * # api-gateway:8081
     * # movie-service:8082
     * # notification-service:8083
     * # payment-service:8084
     * # seating-service:8085
     * # user-management-service:8086
     * # gui-service:8087
     * # ticketing-manager:8088
     * # service-orchestrator:8089
     * # session-manager:8090   
    */
    public void mapTopicsToClient() {
        restRouter.put("RewardsRequest", userServiceClient);
        restRouter.put("AccountInfoRequest", userServiceClient);
        restEndpoints.put(userServiceClient, "http://user-management-service:8086/api/v1/processTopic");

        restEndpoints.put(sessionManagerClient, "http://session-manager:8090/api/v1/user");
        LOG.info("Sucessfully mapped the topics to their respective microservices...");
    }

    // get current logged in user from the session manager
    // then get the account info from the user management service
    // finally send a rewards request to the user management service
    @Async 
    public void handleRewards(PaymentRequest paymentRequest) {
        try {
            String user = sessionManagerClient.get()
                .uri(restEndpoints.get(sessionManagerClient))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

            LOG.info("Session Manager returned user: " + user);

            if(user != null && !user.equals("NO_USER")) {
                // send AccountInfoRequest to user-management-service
                AccountInfoRequest accountInfoRequest = new AccountInfoRequest();
                accountInfoRequest.setTopicName("AccountInfoRequest");
                accountInfoRequest.setEmail(paymentRequest.getEmail());
                accountInfoRequest.setUsername(user);
                accountInfoRequest.setCorrelatorId(paymentRequest.getCorrelatorId());

                AccountInfoResponse account = userServiceClient.post()
                    .uri(restEndpoints.get(restRouter.get("AccountInfoRequest")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(accountInfoRequest)
                    .retrieve()
                    .body(AccountInfoResponse.class);
                LOG.info("Sent an AccountInfoRequest to the User Management Service...");

                int rewardPoints = account.getRewardPoints();
                int newPoints = rewardPoints + (int)(paymentRequest.getPaymentAmount() / 10);
                LOG.info(user + " had " + rewardPoints + " reward points. Adding " + newPoints + " new points.");
                
                // send RewardsRequest to user-management-service
                RewardsRequest rewardsRequest = new RewardsRequest();
                rewardsRequest.setTopicName("RewardsRequest");
                rewardsRequest.setCorrelatorId(paymentRequest.getCorrelatorId());
                rewardsRequest.setEmail(paymentRequest.getEmail());
                rewardsRequest.setName(account.getName());
                rewardsRequest.setUsername(user);
                rewardsRequest.setRewardPoints(newPoints);
                rewardsRequest.setApplication(Application.REWARD_POINTS_ADDED);
                
                RewardsResponse rewardsResponse = userServiceClient
                    .post()
                    .uri(restEndpoints.get(restRouter.get("RewardsRequest")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rewardsRequest)
                    .retrieve()
                    .body(RewardsResponse.class);
                LOG.info("Sent a RewardsRequest to the User Management Service... Received response: " + rewardsResponse.getApplication());
            } else {
                LOG.info("No user logged in, skipping sending RewardsRequest and AccountInfoRequest to User Management Service");
            }

        } catch (Exception e) {
            LOG.error("Failed to process rewards", e);
        }
    }
}
