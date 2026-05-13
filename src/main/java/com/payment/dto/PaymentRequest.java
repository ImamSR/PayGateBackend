package com.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record PaymentRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000", message = "Amount must not exceed 1,000,000")
    @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase letters only")
    String currency,

    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    String paymentMethod
) {

    public PaymentRequest {
        // Additional validation can be added here if needed
        if (amount != null && amount.scale() > 4) {
            throw new IllegalArgumentException("Amount cannot have more than 4 decimal places");
        }
    }
}