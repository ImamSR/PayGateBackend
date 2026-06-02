package com.payment.service;


import com.payment.entity.PaymentProvider;
import com.payment.entity.Transaction;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentGatewayDispatcher {
    private final Map<PaymentProvider, PaymentGatewayService> gatewayServices;

    public PaymentGatewayDispatcher(final List<PaymentGatewayService>gatewayServices){
        this.gatewayServices = new EnumMap<>(PaymentProvider.class);

        for (PaymentGatewayService gatewayService: gatewayServices){
            this.gatewayServices.put(gatewayService.getProvider(), gatewayService);

        }
    }

    public void createPayment(final Transaction transaction, final String username){
        final PaymentGatewayService gatewayService = gatewayServices.get(transaction.getProvider());

        if (gatewayService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + transaction.getProvider());
        }
        gatewayService.createPayment(transaction, username);
    }
}
