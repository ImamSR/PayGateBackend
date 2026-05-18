package com.payment.service.midtrans;

public class MidtransIntegrationExecption extends RuntimeException {
    public MidtransIntegrationExecption(final String messege)   {
        super(messege);
    }    

    public MidtransIntegrationExecption(final String messege, final Throwable cause){
        super(messege,cause);
    } 
}
