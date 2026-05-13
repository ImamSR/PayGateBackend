package com.example.payment.entity;

import com.example.payment.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_auth_user_status", columnList = "auth_user_id,status"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_gateway_reference", columnList = "gateway_reference"),
    @Index(name = "idx_transaction_username", columnList = "username")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String errorMessage;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String gatewayResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionLog> logs = new ArrayList<>();

    public Transaction() {
        this.transactionId = UUID.randomUUID().toString();
    }

    public Transaction(Long authUserId, String username, BigDecimal amount, String currency, String paymentMethod) {
        this();
        this.authUserId = authUserId;
        this.username = username;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(Long authUserId) {
        this.authUserId = authUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public List<TransactionLog> getLogs() {
        return logs;
    }

    public void setLogs(List<TransactionLog> logs) {
        this.logs = logs;
    }

    public void addLog(TransactionLog log) {
        logs.add(log);
        log.setTransaction(this);
    }

    public void removeLog(TransactionLog log) {
        logs.remove(log);
        log.setTransaction(null);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return PaymentStatus.PROCESSING.equals(this.status);
    }

    public boolean isCompleted() {
        return PaymentStatus.COMPLETED.equals(this.status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(this.status);
    }

    public boolean isCancelled() {
        return PaymentStatus.CANCELLED.equals(this.status);
    }

    public boolean canBeCancelled() {
        return isPending() || isProcessing();
    }

    public boolean isTerminal() {
        return isCompleted() || isFailed() || isCancelled();
    }

    public void updateStatus(PaymentStatus newStatus, String userIdentifier, String triggeringEvent) {
        PaymentStatus oldStatus = this.status;
        this.status = newStatus;
        
        if (isTerminal()) {
            this.processedAt = Instant.now();
        }
        
        TransactionLog log = new TransactionLog(this, oldStatus, newStatus, triggeringEvent, userIdentifier);
        addLog(log);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
