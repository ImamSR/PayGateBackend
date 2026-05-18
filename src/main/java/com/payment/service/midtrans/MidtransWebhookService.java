package com.payment.service.midtrans;

import org.springframework.stereotype.Service;

import com.payment.dto.midtrans.MidtransWebhookNotification;
import com.payment.entity.Transaction;
import com.payment.entity.PaymentStatus;
import com.payment.repository.TransactionRepository;
import com.payment.service.PaymentNotificationService;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MidtransWebhookService {
    private final TransactionRepository transactionRepository;
    private final MidtransSignatureService midtransSignatureService;
    private final PaymentNotificationService paymentNotificationService;

    public MidtransWebhookService(
        final TransactionRepository transactionRepository,
        final MidtransSignatureService midtransSignatureService,
        final PaymentNotificationService paymentNotificationService
    ) {
        this.transactionRepository = transactionRepository;
        this.midtransSignatureService = midtransSignatureService;
        this.paymentNotificationService = paymentNotificationService;
    }

@Transactional
public void handleNotification(final MidtransWebhookNotification notification){
    if (!midtransSignatureService.isValidSignature(notification)){
        throw new IllegalArgumentException("Invalid Midtrans Signature");
    }

    final Transaction transaction = transactionRepository.findByTransactionId(notification.orderId())
        .orElseThrow(() -> new IllegalArgumentException(
            "Transaction not found for order id:" + notification.orderId()));
            
    if (transaction.getStatus().isTerminal()){
        return;
    }

    final PaymentStatus mappedStatus = mapStatus(notification.transactionStatus(), notification.fraudStatus());

    transaction.setGatewayReference(notification.transactionId());
    transaction.setGatewayResponse(notification.transactionStatus());
    transaction.setErrorMessage(mappedStatus == PaymentStatus.FAILED ? "Midtrans reported failed payment" : null);
    transaction.updateStatus(mappedStatus, "midtrans-webhook", "MIDTRANS_WEBHOOK_"+ notification.transactionStatus());

    transactionRepository.save(transaction);

    paymentNotificationService.sendStatusUpdate(transaction.getUsername(), transaction);
    }

    private PaymentStatus mapStatus(final String transactionStatus, final String fraudStatus){
        if ("capture".equalsIgnoreCase(transactionStatus)){
            if("chalangge".equalsIgnoreCase(fraudStatus)){
               return PaymentStatus.PROCESSING;
            }
            return PaymentStatus.COMPLETED; 
        }
        
        if ("settlement".equalsIgnoreCase(transactionStatus)){
            return PaymentStatus.COMPLETED;
        }

        if ("pending".equalsIgnoreCase(transactionStatus)){
            return PaymentStatus.PENDING;
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

        throw new IllegalArgumentException("Unsupported Midtrans Transaction Status" + transactionStatus);
    }
}
