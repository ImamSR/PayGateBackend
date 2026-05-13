package com.payment.service;

import com.payment.dto.PaymentResponse;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessingService {

    private final TransactionRepository transactionRepository;
    private final PaymentNotificationService paymentNotificationService;
    private final MeterRegistry meterRegistry;

    public PaymentProcessingService(
            TransactionRepository transactionRepository,
            PaymentNotificationService paymentNotificationService,
            MeterRegistry meterRegistry
    ) {
        this.transactionRepository = transactionRepository;
        this.paymentNotificationService = paymentNotificationService;
        this.meterRegistry = meterRegistry;
    }

    @Async
    @Transactional
    public void processTransaction(String transactionId, String username) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            updateStatus(transactionId, username, PaymentStatus.PROCESSING, "PAYMENT_PROCESSING_STARTED", null);
            Thread.sleep(1200);

            Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

            if (shouldFail(transaction)) {
                transaction.setErrorMessage("Payment gateway rejected the request");
                transaction.updateStatus(PaymentStatus.FAILED, username, "PAYMENT_GATEWAY_REJECTED");
                transactionRepository.save(transaction);

                meterRegistry.counter("payment.transactions.failed").increment();
                paymentNotificationService.sendStatusUpdate(username, transaction);
                paymentNotificationService.sendError(username, transactionId, transaction.getErrorMessage());
                paymentNotificationService.sendPaymentResponse(
                        username,
                        PaymentResponse.error(transactionId, PaymentStatus.FAILED, transaction.getErrorMessage())
                );
            } else {
                transaction.setGatewayReference("gw-" + transactionId.substring(0, 8));
                transaction.setGatewayResponse("Processed by simulated gateway");
                transaction.updateStatus(PaymentStatus.COMPLETED, username, "PAYMENT_COMPLETED");
                transactionRepository.save(transaction);

                meterRegistry.counter("payment.transactions.completed").increment();
                paymentNotificationService.sendStatusUpdate(username, transaction);
                paymentNotificationService.sendPaymentResponse(
                        username,
                        PaymentResponse.success(transactionId, PaymentStatus.COMPLETED)
                );
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } finally {
            sample.stop(Timer.builder("payment.transactions.processing.duration")
                    .description("Time spent processing payment transactions")
                    .register(meterRegistry));
        }
    }

    private void updateStatus(
            String transactionId,
            String username,
            PaymentStatus status,
            String event,
            String errorMessage
    ) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        transaction.setErrorMessage(errorMessage);
        transaction.updateStatus(status, username, event);
        transactionRepository.save(transaction);

        paymentNotificationService.sendStatusUpdate(username, transaction);
        paymentNotificationService.sendPaymentResponse(username, PaymentResponse.success(transactionId, status));
    }

    private boolean shouldFail(Transaction transaction) {
        return transaction.getPaymentMethod().toUpperCase().contains("FAIL")
                || transaction.getAmount().compareTo(java.math.BigDecimal.valueOf(25_000)) > 0;
    }
}
