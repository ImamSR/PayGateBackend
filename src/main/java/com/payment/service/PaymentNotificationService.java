package com.payment.service;

import com.payment.dto.PaymentResponse;
import com.payment.dto.TransactionStatusUpdate;
import com.payment.entity.Transaction;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public PaymentNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendPaymentResponse(String username, PaymentResponse response) {
        messagingTemplate.convertAndSendToUser(username, "/queue/payment-response", response);
    }

    public void sendStatusUpdate(String username, Transaction transaction) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/status-updates",
                TransactionStatusUpdate.from(
                        transaction.getTransactionId(),
                        transaction.getStatus(),
                        transaction.getErrorMessage()
                )
        );
    }

    public void sendError(String username, String transactionId, String message) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                Map.of(
                        "transactionId", transactionId,
                        "message", message
                )
        );
    }
}
