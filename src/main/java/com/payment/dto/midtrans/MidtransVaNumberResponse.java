package com.payment.dto.midtrans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MidtransVaNumberResponse(
        String bank,
        @com.fasterxml.jackson.annotation.JsonProperty("va_number")
        String vaNumber
) {
}
