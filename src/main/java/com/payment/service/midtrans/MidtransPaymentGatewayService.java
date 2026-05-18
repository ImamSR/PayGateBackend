package com.payment.service.midtrans;

import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.repository.TransactionRepository;
import com.payment.service.PaymentGatewayService;
import com.payment.service.PaymentNotificationService;
import com.payment.dto.PaymentResponse;
import com.payment.dto.midtrans.MidtransChargeRequest;
import com.payment.dto.midtrans.MidtransChargeResponse;
import com.payment.dto.midtrans.MidtransTransactionDetailRequest;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MidtransPaymentGatewayService implements PaymentGatewayService {
    
    private final TransactionRepository transactionRepository;
    private final PaymentNotificationService paymentNotificationService;
    private final MeterRegistry meterRegistry;

    public MidtransPaymentGatewayService(
        final TransactionRepository transactionRepository,
        final PaymentNotificationService paymentNotificationService,
        final MeterRegistry meterRegistry,
        final MidtransClient midtransClient
    ) {
        this.transactionRepository = transactionRepository;
        this.paymentNotificationService = paymentNotificationService;
        this.meterRegistry = meterRegistry;
        this.midtransClient = midtransClient;
    }

    @Override
    public PaymentProvider getProvider(){
        return PaymentProvider.MIDTRANS;
    }

    @Override
    @Async
    @Transactional
    public void createPayment (final Transaction transaction, final String username){
        final Transaction managedTransaction = transactionRepository.findByTransactionId(transaction.getTransactionId())
        .orElseThrow(()-> new IllegalStateException("Transaction Not Found" + transaction.getTransactionId()));


        final MidtransChargeRequest chargeRequest = new MidtransChargeRequest(
            mapPaymentType(managedTransaction.getPaymentMethod()),
            new MidtransTransactionDetailRequest(
                managedTransaction.getTransactionId(),
                managedTransaction.getAmount()
            )
        );

        final MidtransChargeResponse chargeResponse = midtransClient.charge(chargeRequest);
        managedTransaction.setGatewayReference(chargeResponse.transactionId());
        managedTransaction.setGatewayResponse(chargeResponse.statusMessage());
        managedTransaction.updateStatus(PaymentStatus.PROCESSING, username, "MIDTRANS_PAYMENT_CREATED");

        transactionRepository.save(managedTransaction);

        meterRegistry.counter("payment.transactions.midtrans.created").increment();
        paymentNotificationService.sendStatusUpdate(username, managedTransaction);
        paymentNotificationService.sendPaymentResponse(username,PaymentResponse.success(managedTransaction.getTransactionId(), managedTransaction.getStatus()));
    }
    private final MidtransClient midtransClient;

    private String mapPaymentType(final String paymentMethod){
        if("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)){
            return "bank_transfer";
        }

        if("QRIS".equalsIgnoreCase(paymentMethod)){
            return "qris";
        }

        throw new IllegalArgumentException("Unsuported Midtrans Payment Method " + paymentMethod);
    }


}
