package com.payment.service.midtrans;

public class MidtransIntegrationException extends RuntimeException {
    public MidtransIntegrationException(final String messege)   {
        super(messege);
    }    

    public MidtransIntegrationException(final String messege, final Throwable cause){
        super(messege,cause);
    } 
}
