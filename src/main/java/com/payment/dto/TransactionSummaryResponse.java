package com.payment.dto;

import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSummaryResponse(
        String transactionId,
        BigDecimal amount,
        String currency,
        PaymentProvider provider,
        String paymentMethod,
        PaymentStatus status,
        String message,
        String gatewayReference,
        String gatewayResponse,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant updatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant processedAt
) {
    public static TransactionSummaryResponse from(Transaction transaction) {
        return new TransactionSummaryResponse(
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getProvider(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getErrorMessage(),
                transaction.getGatewayReference(),
                transaction.getGatewayResponse(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getProcessedAt()
        );
    }
}
