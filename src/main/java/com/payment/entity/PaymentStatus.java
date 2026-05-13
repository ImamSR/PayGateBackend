package com.payment.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    public boolean canBeCancelled() {
        return this == PENDING || this == PROCESSING;
    }
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}