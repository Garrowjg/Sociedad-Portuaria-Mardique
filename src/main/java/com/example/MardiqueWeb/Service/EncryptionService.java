package com.example.MardiqueWeb.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private final SecretKeySpec keySpec;

    public EncryptionService(@Value("${app.encryption.key}") String rawKey) {
        byte[] keyBytes = new byte[16];
        byte[] rawBytes = rawKey.getBytes();
        System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 16));
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String data) {
        if (data == null || data.isEmpty()) return data;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), "UTF-8");
        } catch (Exception e) {
            return encrypted;
        }
    }
}
