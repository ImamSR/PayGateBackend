package com.payment.service.midtrans;

import com.payment.config.MidtransProperties;
import com.payment.dto.midtrans.MidtransWebhookNotification;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class MidtransSignatureService {
    private final MidtransProperties midtransProperties;

    public MidtransSignatureService(final MidtransProperties midtransProperties) {
        this.midtransProperties = midtransProperties;
    }

    public boolean isValidSignature(final MidtransWebhookNotification notification) {

        final String expectedSignature = sha512(
                notification.orderId()
                        + notification.statusCode()
                        + notification.grossAmount()
                        + midtransProperties.getServerKey());

        return expectedSignature.equals(notification.signatureKey());
    }

    private String sha512(final String value){
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            final byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));

            final StringBuilder builder = new StringBuilder();
            for (final byte current : hash ){
                builder.append(String.format("%02x", current ));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception){
            throw new IllegalStateException("SHA-512 algorithm is not available", exception);
        }
    }

    public void handleNotification(MidtransWebhookNotification notification) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleNotification'");
    }
}
