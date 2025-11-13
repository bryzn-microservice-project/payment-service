package com.schemaValidator;

import java.io.InputStream;
import java.net.URL;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import com.SchemaService;
import com.schema.SchemaValidator;  

@SpringBootTest(classes = SchemaValidator.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class SchemaValidatorTest {
    @Autowired
    private ResourceLoader resourceLoader;

    private SchemaValidator schemaValidator;

    @BeforeEach
    void setup() {
        schemaValidator = new SchemaValidator(resourceLoader);
    }

    @Test
    @DisplayName("[SCHEMA] Valid PaymentRequest 1")
    void testPaymentRequest1(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 987654,
                "paymentAmount": 120.50,
                "email": "test.user@example.com",
                "creditCard": "4111111111111111",
                "cvc": "123"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Valid PaymentRequest 2")
    void testPaymentRequest2(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 1123,
                "paymentAmount": 20.00,
                "email": "bryzntest@gmail.com",
                "creditCard": "6011000990139424",
                "cvc": "321"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Valid PaymentRequest 3")
    void testPaymentRequest3(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 1123,
                "paymentAmount": 2324.55,
                "email": "bryzntest@gmail.com",
                "creditCard": "6011000990139424",
                "cvc": "321"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid PaymentRequest (payment amount)")
    void testBadPaymentRequest1(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 1123,
                "paymentAmount": 2324.552342,
                "email": "bryzntest@gmail.com",
                "creditCard": "6011000990139424",
                "cvc": "321"
            }
        """);
        
        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid PaymentRequest (payment amount + credit card)")
    void testBadPaymentRequest2(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 1123,
                "paymentAmount": 2324.552342,
                "email": "bryzntest@gmail.com",
                "creditCard": "12345",
                "cvc": "321"
            }
        """);
        
        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid PaymentRequest (email)")
    void testBadPaymentRequest3(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "PaymentRequest",
                "correlatorId": 1123,
                "paymentAmount": 2324.55,
                "email": "bryzntest",
                "creditCard": "6011000990139424",
                "cvc": "321"
            }
        """);
        
        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "PaymentRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    private boolean validate(String topicName, JSONObject validJson)
    {
        URL schemaUrl = getClass().getClassLoader().getResource(SchemaService.getPathFor(topicName));

        // Check if the schema is found
        if (schemaUrl == null) {
            throw new RuntimeException("Schema not found for topic: " + topicName);
        }

        // Load schema stream from resource
        InputStream schemaStream = schemaValidator.getSchemaStream(SchemaService.getPathFor(topicName));

        if (schemaStream == null) {
            System.out.println("No schema found for topic: " + topicName);
        }

        // Validate the JSON using the schema stream
        return schemaValidator.validateJson(schemaStream, validJson);
    }
}
