package com.payment.dto.midtrans;

  import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
  import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)

public record MidtransWebhookNotification(
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

    @JsonProperty("gross_amount")
    String grossAmount,

    @JsonProperty("signature_key")
    String signatureKey,

    @JsonProperty("payment_type")
    String paymentType  
) {
    

}
