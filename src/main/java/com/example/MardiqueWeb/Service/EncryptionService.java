package com.example.MardiqueWeb.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    public EncryptionService(@Value("${app.encryption.key}") String rawKey,
                            @Value("${app.encryption.iv}") String rawIv) {
        byte[] keyBytes = new byte[16];
        byte[] rawBytes = rawKey.getBytes();
        System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 16));
        this.keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] ivBytes = new byte[16];
        byte[] ivRawBytes = rawIv.getBytes();
        System.arraycopy(ivRawBytes, 0, ivBytes, 0, Math.min(ivRawBytes.length, 16));
        this.ivSpec = new IvParameterSpec(ivBytes);
    }

    public String encrypt(String data) {
        if (data == null || data.isEmpty()) return data;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), "UTF-8");
        } catch (Exception e) {
            return encrypted;
        }
    }
}
