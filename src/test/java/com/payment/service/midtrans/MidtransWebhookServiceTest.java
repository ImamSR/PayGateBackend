package com.payment.service.midtrans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.dto.midtrans.MidtransWebhookNotification;
import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.repository.TransactionRepository;
import com.payment.service.PaymentNotificationService;

import jakarta.xml.bind.annotation.W3CDomHandler;
import net.bytebuddy.asm.Advice.Argument;

@ExtendWith(MockitoExtension.class)
class MidtransWebhookServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MidtransSignatureService midtransSignatureService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private MidtransWebhookService midtransWebhookService;

    private Transaction transaction;

    @BeforeEach
    void setUp(){
        transaction =  new Transaction(
            1L,
            "edward",
            new BigDecimal("10000.00"),
            "IDR",
            PaymentProvider.MIDTRANS,
            "QRIS"
        );
        transaction.setTransactionId("order-123");
        transaction.setStatus(PaymentStatus.PROCESSING);
    }


    @Test
    void handle_notification_settlement_marks_transaction_completed() {
        // Given Test
        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "midtrans-tx-001",
            "order-123",
            "settlement",
            "accept",
            "200",
            "10000.00",
            "valid-signature",
            "qris"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        //when 
        midtransWebhookService.handleNotification(notification);

        //then
        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.COMPLETED, savedTransaction.getStatus());
        assertEquals("midtrans-tx-001", savedTransaction.getStatus());
        assertEquals("settlement", savedTransaction.getGatewayResponse());
        assertNull(savedTransaction.getErrorMessage());

        verify(paymentNotificationService).sendStatusUpdate("edward", savedTransaction);
    }

    @Test
    void handle_notification_pending_keeps_transaction_processing(){
        //given

        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-002", 
            "order-123",
            "pending", 
            "accept",
            "201",
            "10000.00",
            "valid-signature",
            "qris"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        //when 
        midtransWebhookService.handleNotification(notification);

        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.PROCESSING, savedTransaction.getStatus());
        assertEquals("Midtrans-tx-002", savedTransaction.getStatus());
        assertEquals("pending", savedTransaction.getGatewayResponse());
        assertNull(savedTransaction.getErrorMessage());
    }

    @Test
    void handle_notification_capture_challange_keeps_transaction_processing(){
        //given
        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-003", 
            "order-123",
            "capture", 
            "challenge",
            "202",
            "10000.00",
            "valid-signature",
            "qris"
        );  

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        //when 
        midtransWebhookService.handleNotification(notification);

        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.PROCESSING, savedTransaction.getStatus());
    }

    @Test
    void handle_notification_deny_marks_transaction_failed(){
        //given
        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-004", 
            "order-123",
            "deny", 
            "accept",
            "203",
            "10000.00",
            "valid-signature",
            "qris"
        );  

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        //when 
        midtransWebhookService.handleNotification(notification);

        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.FAILED, savedTransaction.getStatus());
        assertEquals("Midtrans reported failed payment", savedTransaction.getErrorMessage());
        assertNull(savedTransaction.getErrorMessage());
    }

    @Test
    void handle_notification_invalid_signature_throws_exception(){
        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-005", 
            "order-123",
            "settlement", 
            "accept",
            "200",
            "10000.00",
            "invalid-signature",
            "qris"
        );  

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(false);

        final IllegalArgumentException execption = assertThrows(
            IllegalArgumentException.class,
        () -> midtransWebhookService.handleNotification(notification)
        );

        assertEquals("Invalid Midtrans Signature", execption.getMessage());
        verify(transactionRepository, never()).findByTransactionId("order-123");
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
        verify(paymentNotificationService, never()).sendStatusUpdate(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Transaction.class)
        );
    }

    @Test
    void handle_notification_terminal_transaction_return_without_updating() {
        transaction.setStatus(PaymentStatus.COMPLETED);

        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-006", 
            "order-123",
            "settlement", 
            "accept",
            "200",
            "10000.00",
            "valid-signature",
            "qris"
        );  

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        midtransWebhookService.handleNotification(notification);

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
        verify(paymentNotificationService, never()).sendStatusUpdate(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Transaction.class)
        );
    }

    @Test
    void handle_notification_missing_transaction_throws_exception(){
        final MidtransWebhookNotification notification = new MidtransWebhookNotification(
            "Midtrans-tx-007", 
            "order-123",
            "settlement", 
            "accept",
            "200",
            "10000.00",
            "valid-signature",
            "qris"
        );  

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.empty());

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> midtransWebhookService.handleNotification(notification)
        );

        assertEquals("Transaction with ID order-123 not found", exception.getMessage());
        verify(transactionRepository).findByTransactionId("order-123");
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
        verify(paymentNotificationService, never()).sendStatusUpdate(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Transaction.class)
        );
    }
}
