package com.payment.dto.midtrans;


import  com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import  com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MidtransChargeResponse(
    @JsonProperty("transaction_id")
    String transactionId,

    @JsonProperty("order_id")
    String orderId,

    @JsonProperty("transaction_status")
    String transactionStatus,

    @JsonProperty("fraud_status")
    String fraudStatus,

    @JsonProperty("status_code")
    String statusCode,

    @JsonProperty("status_message")
    String statusMessage,

    @JsonProperty("payment_type")
    String paymentType,

    @JsonProperty("expiry_time")
    String expiryTime,

    @JsonProperty("qr_string")
    String qrString,

    @JsonProperty("permata_va_number")
    String permataVaNumber,

    @JsonProperty("actions")
    List<MidtransPaymentActionResponse> actions,

    @JsonProperty("va_numbers")
    List<MidtransVaNumberResponse> vaNumbers
) {
    
}
