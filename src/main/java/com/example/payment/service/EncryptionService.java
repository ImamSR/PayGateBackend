package com.example.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${encryption.key}") String encryptionKey) {
        this.secretKey = new SecretKeySpec(
            encryptionKey.getBytes(StandardCharsets.UTF_8), 
            ALGORITHM
        );
        this.secureRandom = new SecureRandom();
    }

    public sealed interface EncryptionResult 
        permits EncryptionResult.Success, EncryptionResult.Failure {
        
        record Success(String encryptedData) implements EncryptionResult {}
        record Failure(String error, Throwable cause) implements EncryptionResult {}
    }

    public sealed interface DecryptionResult 
        permits DecryptionResult.Success, DecryptionResult.Failure {
        
        record Success(String decryptedData) implements DecryptionResult {}
        record Failure(String error, Throwable cause) implements DecryptionResult {}
    }

    public EncryptionResult encryptSafe(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return new EncryptionResult.Success(plaintext);
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            String result = Base64.getEncoder().encodeToString(encryptedWithIv);
            return new EncryptionResult.Success(result);
        } catch (Exception e) {
            return new EncryptionResult.Failure("Encryption failed", e);
        }
    }

    public DecryptionResult decryptSafe(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return new DecryptionResult.Success(encryptedData);
        }

        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decodedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(encrypted);
            String result = new String(decryptedData, StandardCharsets.UTF_8);
            return new DecryptionResult.Success(result);
        } catch (Exception e) {
            return new DecryptionResult.Failure("Decryption failed", e);
        }
    }

    public String processEncryptionResult(EncryptionResult result) {
        if (result instanceof EncryptionResult.Success success) {
            return success.encryptedData();
        }
        return null;
    }

    public String processDecryptionResult(DecryptionResult result) {
        if (result instanceof DecryptionResult.Success success) {
            return success.decryptedData();
        }
        return null;
    }

    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    public String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        byte[] decodedData = Base64.getDecoder().decode(encryptedData);

        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];
        System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(decodedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] decryptedData = cipher.doFinal(encrypted);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
