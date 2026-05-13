package com.payment.dto;

import com.payment.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record TransactionStatusUpdate(
        String transactionId,
        PaymentStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp,
        String errorMessage
) {
    public static TransactionStatusUpdate from(String transactionId, PaymentStatus status, String errorMessage) {
        return new TransactionStatusUpdate(transactionId, status, Instant.now(), errorMessage);
    }
}
