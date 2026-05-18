package com.payment.service;

import com.payment.entity.PaymentProvider;
import com.payment.entity.Transaction;

public interface PaymentGatewayService {
    PaymentProvider getProvider();

    void createPayment(final Transaction transaction, final String username);
} 
