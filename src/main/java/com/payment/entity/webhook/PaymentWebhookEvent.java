package com.payment.entity.webhook;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.payment.converter.EncryptedStringConverter;
import com.payment.entity.PaymentProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(name = "payment_webhook_events", indexes ={
    @Index(name = "idx_webhook_provider_event", columnList = "provider,event_id",unique=true),
    @Index(name = "idx_webhook_order_id", columnList = "order_id"),
    @Index(name = "idx_webhook_processed", columnList = "processed")
    
})

public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "event_id", nullable = false, length = 150)
    private String eventId;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String payload;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PaymentWebhookEvent() {
    }

    public PaymentWebhookEvent(
        final PaymentProvider provider, 
        final String eventId, 
        final String orderId, 
        final String eventType, 
        final String payload
    ) {
        this.provider = provider;
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
    }

    public Long getId() {
        return id;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }
    
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }


    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
