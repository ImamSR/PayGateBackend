package com.payment.dto;

import com.payment.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;


public record PaymentResponse(
    String transactionId,
    PaymentStatus status,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant timestamp,
    
    String message
) {
    
    public PaymentResponse(String transactionId, PaymentStatus status, Instant timestamp) {
        this(transactionId, status, timestamp, null);
    }
    
    public static PaymentResponse success(String transactionId, PaymentStatus status) {
        return new PaymentResponse(transactionId, status, Instant.now());
    }
    

    public static PaymentResponse error(String transactionId, PaymentStatus status, String message) {
        return new PaymentResponse(transactionId, status, Instant.now(), message);
    }
}