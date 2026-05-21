package com.payment.repository.webhook;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.entity.PaymentProvider;
import com.payment.entity.webhook.PaymentWebhookEvent;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByProviderAndEventId(PaymentProvider provider, String eventId);

    boolean existsByProviderAndEventId(PaymentProvider provider, String eventId);

}