package com.example.payment;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.service.EncryptionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class Java17FeaturesTest {

    @Test
    void testRecordFeatures() {
        PaymentRequest request = new PaymentRequest(
            new BigDecimal("100.50"),
            "USD",
            "CREDIT_CARD"
        );
        
        assertEquals(new BigDecimal("100.50"), request.amount());
        assertEquals("USD", request.currency());
        assertEquals("CREDIT_CARD", request.paymentMethod());
        
        PaymentResponse response = PaymentResponse.success("txn-123", PaymentStatus.COMPLETED);
        assertEquals("txn-123", response.transactionId());
        assertEquals(PaymentStatus.COMPLETED, response.status());
        assertNotNull(response.timestamp());
        assertNull(response.message());
        
        PaymentResponse errorResponse = PaymentResponse.error("txn-456", PaymentStatus.FAILED, "Payment declined");
        assertEquals("txn-456", errorResponse.transactionId());
        assertEquals(PaymentStatus.FAILED, errorResponse.status());
        assertEquals("Payment declined", errorResponse.message());
    }

    @Test
    void testSealedClassesWithInstanceofPatternMatching() {
        String validKey = "myTestKey123456789012345678901234567890123456789012345678901234567890";
        EncryptionService encryptionService = new EncryptionService(validKey.substring(0, 32));
        
        EncryptionService.EncryptionResult successResult = encryptionService.encryptSafe("test data");

        String processedResult;
        if (successResult instanceof EncryptionService.EncryptionResult.Success success) {
            assertNotNull(success.encryptedData());
            assertNotEquals("test data", success.encryptedData());
            processedResult = "Encryption successful: " + success.encryptedData().length() + " chars";
        } else if (successResult instanceof EncryptionService.EncryptionResult.Failure failure) {
            fail("Encryption should not fail for valid input: " + failure.error());
            return;
        } else {
            fail("Unexpected encryption result type");
            return;
        }
        
        assertTrue(processedResult.startsWith("Encryption successful"));
        
        if (successResult instanceof EncryptionService.EncryptionResult.Success encryptedSuccess) {
            EncryptionService.DecryptionResult decryptResult = encryptionService.decryptSafe(encryptedSuccess.encryptedData());

            String decryptedData;
            if (decryptResult instanceof EncryptionService.DecryptionResult.Success decrypted) {
                assertEquals("test data", decrypted.decryptedData());
                decryptedData = decrypted.decryptedData();
            } else if (decryptResult instanceof EncryptionService.DecryptionResult.Failure failure) {
                fail("Decryption should not fail for valid encrypted data: " + failure.error());
                return;
            } else {
                fail("Unexpected decryption result type");
                return;
            }

            assertEquals("test data", decryptedData);
        }
    }

    @Test
    void testTextBlocks() {
        String jsonTemplate = """
            {
                "transactionId": "%s",
                "amount": %s,
                "currency": "%s",
                "status": "%s",
                "timestamp": "%s"
            }
            """;
        
        String json = jsonTemplate.formatted(
            "txn-123",
            "100.50",
            "USD",
            "COMPLETED",
            Instant.now().toString()
        );
        
        assertTrue(json.contains("txn-123"));
        assertTrue(json.contains("100.50"));
        assertTrue(json.contains("USD"));
        assertTrue(json.contains("COMPLETED"));
    }

    @Test
    void testPatternMatchingWithInstanceof() {
        Object testValue = "Hello World";

        String result = describeValue(testValue);
        
        assertEquals("Long string: Hello World", result);
        
        testValue = 42;
        result = describeValue(testValue);

        assertEquals("Positive number: 42", result);
    }

    @Test
    void testRecordValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PaymentRequest(
                new BigDecimal("100.123456"),
                "USD",
                "CREDIT_CARD"
            );
        });
        
        assertDoesNotThrow(() -> {
            new PaymentRequest(
                new BigDecimal("100.1234"),
                "USD",
                "CREDIT_CARD"
            );
        });
    }

    private String describeValue(Object value) {
        if (value == null) {
            return "Null value";
        }
        if (value instanceof String stringValue) {
            return stringValue.length() > 10
                    ? "Long string: " + stringValue
                    : "Short string: " + stringValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue > 0
                    ? "Positive number: " + integerValue
                    : "Non-positive number: " + integerValue;
        }
        return "Unknown type: " + value.getClass().getSimpleName();
    }
}
