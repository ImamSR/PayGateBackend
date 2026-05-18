package com.payment.dto.midtrans;

public record MidtransChargeRequest(
    String payment_type,
    MidtransTransactionDetailRequest transaction_details
) {
    
}
