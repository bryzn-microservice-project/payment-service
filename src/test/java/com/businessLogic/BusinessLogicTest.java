package com.businessLogic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postgres.PostgresService;
import com.postgres.models.Payment;
import com.topics.AccountInfoRequest;
import com.topics.AccountInfoResponse;
import com.topics.PaymentResponse.Status;
import com.topics.RewardsRequest;
import com.topics.RewardsResponse.Application;
import com.topics.PaymentRequest;
import com.topics.PaymentResponse;
import com.topics.RewardsResponse;

@ExtendWith(MockitoExtension.class)
public class BusinessLogicTest {
	@InjectMocks
	private BusinessLogic businessLogic;
	@Mock
	private PostgresService postgresService;
	@Mock
     private RestClient userManagementClient; 
     @Mock
     private AsyncLogic asyncLogic;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("[BUSINESS_LOGIC] Valid PaymentRequest")
	public void createPaymentRequest(TestInfo testInfo) {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
		String JSON = """
			{
                    "topicName": "PaymentRequest",
                    "correlatorId": 987654,
                    "paymentAmount": 125.00,
                    "email": "test.user@example.com",
                    "creditCard": "4111111111111111",
                    "cvc": "123"
               }
			""";
		
		PaymentRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, PaymentRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// REST CLIENT MOCK FOR THE USER MANGAEMENT SERVICE
          AccountInfoResponse accountResponse = new AccountInfoResponse();
          accountResponse.setTopicName("AccountInfoResponse");
          accountResponse.setCorrelatorId(987654);
          accountResponse.setName("Test User");
          accountResponse.setEmail("test.user@example.com");
          accountResponse.setRewardPoints(5000);
          accountResponse.setCreditCard("4111111111111111");
          accountResponse.setCvc("123");

          RewardsResponse rewardsResponse = new RewardsResponse();
          rewardsResponse.setTopicName("RewardResponse");
          rewardsResponse.setApplication(Application.SUCCESS);
          rewardsResponse.setCorrelatorId(987654);
          rewardsResponse.setUsername("TEST USER");
          rewardsResponse.setEmail("test.user@example.com");
          rewardsResponse.setRewardPoints(1000);

		// REST CLIENT MOCK FOR THE USER MANAGEMENT
          RestClient.RequestBodyUriSpec uriSpec1 = mock(RestClient.RequestBodyUriSpec.class);
          RestClient.RequestBodySpec bodySpec1 = mock(RestClient.RequestBodySpec.class);
          RestClient.ResponseSpec responseSpec1 = mock(RestClient.ResponseSpec.class);

          RestClient.RequestBodyUriSpec uriSpec2 = mock(RestClient.RequestBodyUriSpec.class);
          RestClient.RequestBodySpec bodySpec2 = mock(RestClient.RequestBodySpec.class);
          RestClient.ResponseSpec responseSpec2 = mock(RestClient.ResponseSpec.class);

          when(userManagementClient.post()).thenReturn(uriSpec1, uriSpec2);

          // First call - AccountInfo
          when(uriSpec1.uri(ArgumentMatchers.<String>any())).thenReturn(bodySpec1); // this line is crucial, or else URI error
          when(bodySpec1.contentType(any(MediaType.class))).thenReturn(bodySpec1);
          when(bodySpec1.body(any(AccountInfoRequest.class))).thenReturn(bodySpec1);
          when(bodySpec1.retrieve()).thenReturn(responseSpec1);
          when(responseSpec1.body(String.class)).thenReturn(toJson(accountResponse));

          // Second call - RewardsRequest
          when(uriSpec2.uri(ArgumentMatchers.<String>any())).thenReturn(bodySpec2); // this line is crucial, or else URI error
          when(bodySpec2.contentType(any(MediaType.class))).thenReturn(bodySpec2);
          when(bodySpec2.body(any(RewardsRequest.class))).thenReturn(bodySpec2);
          when(bodySpec2.retrieve()).thenReturn(responseSpec2);
          when(responseSpec2.body(String.class)).thenReturn(toJson(rewardsResponse));

          Payment payment = new Payment();
          payment.setId(Long.valueOf(1000));
          payment.setPaymentAmount(BigDecimal.valueOf(request.getPaymentAmount()));
          payment.setCashAmount(BigDecimal.valueOf(100));
          payment.setRewardCashApplied(BigDecimal.valueOf(25));
          payment.setCreditCard(request.getCreditCard());
          payment.setCvc(request.getCvc());
          payment.setEmail(request.getEmail());
          payment.setInitialTimeStamp("FAKE_TIME");

          when(postgresService.save(any(Payment.class)))
			.thenReturn(payment);

		ResponseEntity<Object> httpResponse = businessLogic.processPaymentRequest(request);
		PaymentResponse response = null;	
		try{
               @SuppressWarnings("null")
               String body = httpResponse.getBody().toString();
               if (isString(body)) {
                    System.out.println("\n" + httpResponse.getBody());
               } else {
                    response = objectMapper.readValue(body, PaymentResponse.class);
               }
          } catch (JsonProcessingException e) {
               e.printStackTrace();
          }

		assertNotNull(response);
		Assertions.assertEquals(Status.SUCCESSFUL, response.getStatus());
          Assertions.assertEquals(125.00, response.getPaymentAmount());
          Assertions.assertEquals(request.getCorrelatorId(), response.getCorrelatorId());
          Assertions.assertEquals(request.getEmail(), response.getEmail());
	}

	private boolean isString(String responseBody) {
		// Check if the response is a simple string (you may need more specific checks depending on your use case)
		return responseBody != null && responseBody.length() > 0 && responseBody.charAt(0) != '{';
	}

     // Helper method to serialize an object to JSON string
    private String toJson(Object obj) {
        try {
            // Use Jackson ObjectMapper to convert the object to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(obj);  // Convert object to JSON string
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"error\":\"Error processing JSON\"}";
        }
    }
}
