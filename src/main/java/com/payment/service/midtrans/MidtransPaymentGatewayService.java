package com.payment.service.midtrans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentResponse;
import com.payment.dto.midtrans.MidtransChargeRequest;
import com.payment.dto.midtrans.MidtransChargeResponse;
import com.payment.dto.midtrans.MidtransTransactionDetailRequest;
import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.repository.TransactionRepository;
import com.payment.service.PaymentGatewayService;
import com.payment.service.PaymentNotificationService;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MidtransPaymentGatewayService implements PaymentGatewayService {
    
    private final TransactionRepository transactionRepository;
    private final PaymentNotificationService paymentNotificationService;
    private final MeterRegistry meterRegistry;
    private final MidtransClient midtransClient;
    private final ObjectMapper objectMapper;

    public MidtransPaymentGatewayService(
        final TransactionRepository transactionRepository,
        final PaymentNotificationService paymentNotificationService,
        final MeterRegistry meterRegistry,
        final MidtransClient midtransClient,
        final ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.paymentNotificationService = paymentNotificationService;
        this.meterRegistry = meterRegistry;
        this.midtransClient = midtransClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider getProvider(){
        return PaymentProvider.MIDTRANS;
    }

    @Override
    @Async
    @Transactional
    public void createPayment(final Transaction transaction, final String username) {
        final Transaction managedTransaction = transactionRepository.findByTransactionId(transaction.getTransactionId())
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transaction.getTransactionId()));

        if (!managedTransaction.isPending()) {
            return;
        }

        try {
            final MidtransChargeRequest chargeRequest = buildChargeRequest(managedTransaction);
            final MidtransChargeResponse chargeResponse = midtransClient.charge(chargeRequest);

            managedTransaction.setGatewayReference(chargeResponse.transactionId());
            managedTransaction.setGatewayResponse(serializeChargeResponse(chargeResponse));
            managedTransaction.setErrorMessage(null);
            managedTransaction.updateStatus(PaymentStatus.PROCESSING, username, "MIDTRANS_PAYMENT_CREATED");

            transactionRepository.save(managedTransaction);

            meterRegistry.counter("payment.transactions.midtrans.created").increment();
            paymentNotificationService.sendStatusUpdate(username, managedTransaction);
            paymentNotificationService.sendPaymentResponse(
                    username,
                    PaymentResponse.success(managedTransaction.getTransactionId(), managedTransaction.getStatus())
            );
        } catch (MidtransIntegrationException | IllegalArgumentException exception) {
            managedTransaction.setErrorMessage(exception.getMessage());
            managedTransaction.updateStatus(PaymentStatus.FAILED, username, "MIDTRANS_PAYMENT_FAILED");
            transactionRepository.save(managedTransaction);

            meterRegistry.counter("payment.transactions.midtrans.failed").increment();
            paymentNotificationService.sendStatusUpdate(username, managedTransaction);
            paymentNotificationService.sendError(username, managedTransaction.getTransactionId(), exception.getMessage());
            paymentNotificationService.sendPaymentResponse(
                    username,
                    PaymentResponse.error(
                            managedTransaction.getTransactionId(),
                            managedTransaction.getStatus(),
                            exception.getMessage()
                    )
            );
        }
    }

    private MidtransChargeRequest buildChargeRequest(final Transaction transaction) {
        final MidtransTransactionDetailRequest transactionDetails = new MidtransTransactionDetailRequest(
                transaction.getTransactionId(),
                transaction.getAmount()
        );

        if ("QRIS".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forQris(transactionDetails);
        }

        if ("BCA_VA".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forBankTransfer(transactionDetails, "bca");
        }

        if ("BNI_VA".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forBankTransfer(transactionDetails, "bni");
        }

        if ("BRI_VA".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forBankTransfer(transactionDetails, "bri");
        }

        if ("CIMB_VA".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forBankTransfer(transactionDetails, "cimb");
        }

        if ("PERMATA_VA".equalsIgnoreCase(transaction.getPaymentMethod())) {
            return MidtransChargeRequest.forBankTransfer(transactionDetails, "permata");
        }

        throw new IllegalArgumentException("Unsupported Midtrans payment method: " + transaction.getPaymentMethod());
    }

    private String serializeChargeResponse(final MidtransChargeResponse chargeResponse) {
        try {
            return objectMapper.writeValueAsString(chargeResponse);
        } catch (JsonProcessingException exception) {
            throw new MidtransIntegrationException("Failed to serialize Midtrans charge response", exception);
        }
    }

}
