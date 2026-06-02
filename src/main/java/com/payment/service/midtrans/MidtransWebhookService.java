package com.payment.service.midtrans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.midtrans.MidtransWebhookNotification;
import com.payment.entity.webhook.PaymentWebhookEvent;
import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.repository.TransactionRepository;
import com.payment.repository.webhook.PaymentWebhookEventRepository;
import com.payment.service.PaymentNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MidtransWebhookService {
    private final TransactionRepository transactionRepository;
    private final MidtransSignatureService midtransSignatureService;
    private final PaymentNotificationService paymentNotificationService;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final ObjectMapper objectMapper;

    public MidtransWebhookService(
        final TransactionRepository transactionRepository,
        final MidtransSignatureService midtransSignatureService,
        final PaymentNotificationService paymentNotificationService,
        final PaymentWebhookEventRepository paymentWebhookEventRepository,
        final ObjectMapper objectMapper

    ) {
        this.transactionRepository = transactionRepository;
        this.midtransSignatureService = midtransSignatureService;
        this.paymentNotificationService = paymentNotificationService;
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleNotification(final MidtransWebhookNotification notification){
        if (!midtransSignatureService.isValidSignature(notification)){
            throw new IllegalArgumentException("Invalid Midtrans signature");
        }

        final String eventId = buildEventId(notification);

        if (paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, eventId)){
            return;
        }

        final PaymentWebhookEvent webhookEvent = paymentWebhookEventRepository.save(
                new PaymentWebhookEvent(
                    PaymentProvider.MIDTRANS,
                    eventId,
                    notification.orderId(),
                    notification.transactionStatus(),
                    serializePayload(notification)
                )
        );

        final Transaction transaction = transactionRepository.findByTransactionId(notification.orderId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Transaction not found for order id: " + notification.orderId()));

        if (transaction.getProvider() != PaymentProvider.MIDTRANS) {
            throw new IllegalArgumentException("Transaction provider mismatch for order id: " + notification.orderId());
        }

        if (!transaction.getStatus().isTerminal()){
            final PaymentStatus mappedStatus = mapStatus(   
                notification.transactionStatus(),
                notification.fraudStatus()
            );

            transaction.setGatewayReference(notification.transactionId());
            transaction.setErrorMessage(
                mappedStatus == PaymentStatus.FAILED
                        ? "Midtrans reported failed payment"
                        : null
            );

            transaction.updateStatus(mappedStatus,
            "midtrans-webhook", 
            "MIDTRANS_WEBHOOK_" + notification.transactionStatus()
            );

            transactionRepository.save(transaction);
            paymentNotificationService.sendStatusUpdate(transaction.getUsername(), transaction);
        }
        webhookEvent.setProcessed(true);
        paymentWebhookEventRepository.save(webhookEvent);
    }

    private String buildEventId(final MidtransWebhookNotification notification) {
        return notification.transactionId() + ":" + notification.transactionStatus();
    }
    
    private String serializePayload(final MidtransWebhookNotification notification){
        try {
            return objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException exception){
            throw new IllegalStateException("Failed to serialize Midtrans webhook payload", exception);
        }
    }

    private PaymentStatus mapStatus(final String transactionStatus, final String fraudStatus){
        if ("capture".equalsIgnoreCase(transactionStatus)){
            if("challenge".equalsIgnoreCase(fraudStatus)){
               return PaymentStatus.PROCESSING;
            }
            return PaymentStatus.COMPLETED; 
        }
        
        if ("settlement".equalsIgnoreCase(transactionStatus)){
            return PaymentStatus.COMPLETED;
        }

        if ("pending".equalsIgnoreCase(transactionStatus)){
            return PaymentStatus.PROCESSING;
        }

        if ("deny".equalsIgnoreCase(transactionStatus)
        || "cancel".equalsIgnoreCase(transactionStatus)
        || "expire".equalsIgnoreCase(transactionStatus)) {
            return PaymentStatus.FAILED;
        }

        if ("refund".equalsIgnoreCase(transactionStatus)
        || "partial_refund".equalsIgnoreCase(transactionStatus)
        || "authorize".equalsIgnoreCase(transactionStatus)){
            return PaymentStatus.PROCESSING;
        }

        throw new IllegalArgumentException("Unsupported Midtrans transaction status: " + transactionStatus);
    }
}
