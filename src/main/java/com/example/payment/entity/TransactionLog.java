package com.example.payment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "transaction_logs", indexes = {
    @Index(name = "idx_transaction_log_tx_id", columnList = "transaction_id"),
    @Index(name = "idx_transaction_log_created", columnList = "created_at"),
    @Index(name = "idx_transaction_log_user", columnList = "user_identifier")
})
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private PaymentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private PaymentStatus newStatus;

    @Column(name = "triggering_event", nullable = false, length = 100)
    private String triggeringEvent;

    @Column(name = "user_identifier", nullable = false, length = 100)
    private String userIdentifier;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionLog() {}

    public TransactionLog(Transaction transaction, PaymentStatus oldStatus, PaymentStatus newStatus, 
                         String triggeringEvent, String userIdentifier) {
        this.transaction = transaction;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.triggeringEvent = triggeringEvent;
        this.userIdentifier = userIdentifier;
    }

    public TransactionLog(Transaction transaction, PaymentStatus oldStatus, PaymentStatus newStatus, 
                         String triggeringEvent, String userIdentifier, String additionalInfo) {
        this(transaction, oldStatus, newStatus, triggeringEvent, userIdentifier);
        this.additionalInfo = additionalInfo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public PaymentStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(PaymentStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public PaymentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PaymentStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getTriggeringEvent() {
        return triggeringEvent;
    }

    public void setTriggeringEvent(String triggeringEvent) {
        this.triggeringEvent = triggeringEvent;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLog that = (TransactionLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransactionLog{" +
                "id=" + id +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", triggeringEvent='" + triggeringEvent + '\'' +
                ", userIdentifier='" + userIdentifier + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}