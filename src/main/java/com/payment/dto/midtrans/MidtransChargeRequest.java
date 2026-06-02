package com.payment.dto.midtrans;

public record MidtransChargeRequest(
    String payment_type,
    MidtransTransactionDetailRequest transaction_details,
    MidtransBankTransferRequest bank_transfer
) {

    public static MidtransChargeRequest forQris(final MidtransTransactionDetailRequest transactionDetails) {
        return new MidtransChargeRequest("qris", transactionDetails, null);
    }

    public static MidtransChargeRequest forBankTransfer(
            final MidtransTransactionDetailRequest transactionDetails,
            final String bank
    ) {
        return new MidtransChargeRequest(
                "bank_transfer",
                transactionDetails,
                new MidtransBankTransferRequest(bank)
        );
    }
}
