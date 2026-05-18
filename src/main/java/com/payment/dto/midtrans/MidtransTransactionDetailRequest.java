package com.payment.dto.midtrans;

import java.math.BigDecimal;

public record MidtransTransactionDetailRequest(
    String order_id,
    BigDecimal grossAmount) {
        
}
