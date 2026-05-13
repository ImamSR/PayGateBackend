package com.example.payment.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.UserPrincipal;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.TransactionSummaryResponse;
import com.example.payment.entity.PaymentAccount;
import com.example.payment.entity.Transaction;
import com.example.payment.repository.PaymentAccountRepository;
import com.example.payment.repository.TransactionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentNotificationService paymentNotificationService;
    private final MeterRegistry meterRegistry;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            PaymentAccountRepository paymentAccountRepository,
            PaymentProcessingService paymentProcessingService,
            PaymentNotificationService paymentNotificationService,
            MeterRegistry meterRegistry
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.paymentProcessingService = paymentProcessingService;
        this.paymentNotificationService = paymentNotificationService;
        this.meterRegistry = meterRegistry;

        Gauge.builder("payment.transactions.pending", this.transactionRepository, repository -> repository.findByStatus(com.example.payment.entity.PaymentStatus.PENDING).size())
                .description("Current number of pending transactions")
                .register(meterRegistry);
    }

    @Transactional
    public PaymentResponse createTransaction(PaymentRequest request, Authentication authentication) {
        User user = resolveUser(authentication);
        ensurePaymentAccount(user);

        Transaction transaction = new Transaction(
                user.getId(),
                user.getUsername(),
                request.amount(),
                request.currency(),
                request.paymentMethod()
        );
        transaction.updateStatus(transaction.getStatus(), user.getUsername(), "TRANSACTION_CREATED");
        transactionRepository.saveAndFlush(transaction);

        meterRegistry.counter("payment.transactions.created").increment();

        PaymentResponse response = PaymentResponse.success(transaction.getTransactionId(), transaction.getStatus());
        paymentNotificationService.sendPaymentResponse(user.getUsername(), response);
        paymentNotificationService.sendStatusUpdate(user.getUsername(), transaction);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentProcessingService.processTransaction(transaction.getTransactionId(), user.getUsername());
            }
        });
        return response;
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryResponse> getTransactions(Authentication authentication) {
        User user = resolveUser(authentication);

        if (user.isAdmin()) {
            return transactionRepository.findAll().stream()
                    .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                    .map(TransactionSummaryResponse::from)
                    .toList();
        }

        return transactionRepository.findByAuthUserId(
                user.getId(), 
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream()
                .map(TransactionSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionSummaryResponse getTransaction(String transactionId, Authentication authentication) {
        User user = resolveUser(authentication);

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!user.isAdmin() && !transaction.getAuthUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Transaction does not belong to the current user");
        }

        return TransactionSummaryResponse.from(transaction);
    }

    private User resolveUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userRepository.findById(userPrincipal.getId())
                    .orElseGet(() -> userRepository.findByUsername(userPrincipal.getUsername())
                            .orElseThrow(() -> new IllegalStateException("Authenticated user not found")));
        }

        throw new IllegalStateException("Authenticated user is required");
    }

    private void ensurePaymentAccount(User user) {
        paymentAccountRepository.findById(user.getId())
                .orElseGet(() -> paymentAccountRepository.save(new PaymentAccount(user.getId(), user.getUsername())));
    }
}
