package com.payment.dto.midtrans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MidtransPaymentActionResponse(
        String name,
        String method,
        String url,
        @JsonProperty("qr_code")
        String qrCode
) {
}
