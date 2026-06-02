package com.payment.service.midtrans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.midtrans.MidtransWebhookNotification;
import com.payment.entity.PaymentProvider;
import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import com.payment.entity.webhook.PaymentWebhookEvent;
import com.payment.repository.TransactionRepository;
import com.payment.repository.webhook.PaymentWebhookEventRepository;
import com.payment.service.PaymentNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MidtransWebhookServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Mock
    private MidtransSignatureService midtransSignatureService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MidtransWebhookService midtransWebhookService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction(
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
    void handle_notification_settlement_marks_transaction_completed() throws JsonProcessingException {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-001",
                "settlement",
                "accept",
                "200",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-001:settlement"))
                .thenReturn(false);
        when(objectMapper.writeValueAsString(notification)).thenReturn("{\"transaction_status\":\"settlement\"}");
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        midtransWebhookService.handleNotification(notification);

        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.COMPLETED, savedTransaction.getStatus());
        assertEquals("midtrans-tx-001", savedTransaction.getGatewayReference());
        assertNull(savedTransaction.getErrorMessage());

        verify(paymentNotificationService).sendStatusUpdate("edward", savedTransaction);
        verify(paymentWebhookEventRepository, times(2)).save(any(PaymentWebhookEvent.class));
    }

    @Test
    void handle_notification_pending_keeps_transaction_processing() throws JsonProcessingException {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-002",
                "pending",
                "accept",
                "201",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-002:pending"))
                .thenReturn(false);
        when(objectMapper.writeValueAsString(notification)).thenReturn("{\"transaction_status\":\"pending\"}");
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        midtransWebhookService.handleNotification(notification);

        final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        final Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(PaymentStatus.PROCESSING, savedTransaction.getStatus());
        assertEquals("midtrans-tx-002", savedTransaction.getGatewayReference());
        assertNull(savedTransaction.getErrorMessage());
    }

    @Test
    void handle_notification_duplicate_event_returns_without_processing() {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-003",
                "settlement",
                "accept",
                "200",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-003:settlement"))
                .thenReturn(true);

        midtransWebhookService.handleNotification(notification);

        verify(transactionRepository, never()).findByTransactionId(any(String.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(paymentWebhookEventRepository, never()).save(any(PaymentWebhookEvent.class));
        verify(paymentNotificationService, never()).sendStatusUpdate(any(String.class), any(Transaction.class));
    }

    @Test
    void handle_notification_invalid_signature_throws_exception() {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-004",
                "settlement",
                "accept",
                "200",
                "invalid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(false);

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> midtransWebhookService.handleNotification(notification)
        );

        assertEquals("Invalid Midtrans signature", exception.getMessage());
        verify(transactionRepository, never()).findByTransactionId(any(String.class));
        verify(paymentWebhookEventRepository, never()).save(any(PaymentWebhookEvent.class));
    }

    @Test
    void handle_notification_missing_transaction_throws_exception() throws JsonProcessingException {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-005",
                "settlement",
                "accept",
                "200",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-005:settlement"))
                .thenReturn(false);
        when(objectMapper.writeValueAsString(notification)).thenReturn("{\"transaction_status\":\"settlement\"}");
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.empty());

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> midtransWebhookService.handleNotification(notification)
        );

        assertEquals("Transaction not found for order id: order-123", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void handle_notification_serialization_failure_throws_exception() throws JsonProcessingException {
        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-006",
                "settlement",
                "accept",
                "200",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-006:settlement"))
                .thenReturn(false);
        when(objectMapper.writeValueAsString(notification)).thenThrow(new JsonProcessingException("serialization failed") {
        });

        final IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> midtransWebhookService.handleNotification(notification)
        );

        assertEquals("Failed to serialize Midtrans webhook payload", exception.getMessage());
        verify(paymentWebhookEventRepository, never()).save(any(PaymentWebhookEvent.class));
    }

    @Test
    void handle_notification_terminal_transaction_marks_event_processed_without_transaction_update()
            throws JsonProcessingException {
        transaction.setStatus(PaymentStatus.COMPLETED);

        final MidtransWebhookNotification notification = buildNotification(
                "midtrans-tx-007",
                "settlement",
                "accept",
                "200",
                "valid-signature"
        );

        when(midtransSignatureService.isValidSignature(notification)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.MIDTRANS, "midtrans-tx-007:settlement"))
                .thenReturn(false);
        when(objectMapper.writeValueAsString(notification)).thenReturn("{\"transaction_status\":\"settlement\"}");
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByTransactionId("order-123")).thenReturn(Optional.of(transaction));

        midtransWebhookService.handleNotification(notification);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(paymentNotificationService, never()).sendStatusUpdate(any(String.class), any(Transaction.class));

        final ArgumentCaptor<PaymentWebhookEvent> eventCaptor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, times(2)).save(eventCaptor.capture());
        assertFalse(eventCaptor.getAllValues().isEmpty());
        assertEquals(true, eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1).isProcessed());
    }

    private MidtransWebhookNotification buildNotification(
            final String transactionId,
            final String transactionStatus,
            final String fraudStatus,
            final String statusCode,
            final String signatureKey
    ) {
        return new MidtransWebhookNotification(
                transactionId,
                "order-123",
                transactionStatus,
                fraudStatus,
                statusCode,
                "10000.00",
                signatureKey,
                "qris"
        );
    }
}
