package com.payment.dto.midtrans;


import  com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import  com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
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
    String statusMessage
) {
    
}
